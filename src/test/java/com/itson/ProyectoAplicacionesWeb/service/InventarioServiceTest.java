package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.AjusteInventarioDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.FormatoLibroRepository;
import com.itson.ProyectoAplicacionesWeb.repository.ItemStockRepository;
import com.itson.ProyectoAplicacionesWeb.repository.MovimientoInventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventarioServiceTest {
    @Mock
    private ItemStockRepository itemStockRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @Mock
    private FormatoLibroRepository formatoLibroRepository;

    private InventarioService servicio;

    private ItemStock inventario;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);

        servicio = new InventarioService(
                itemStockRepository,
                movimientoRepository,
                formatoLibroRepository
        );

        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                new Categoria("Terror"),
                new Editorial("Penguin", null)
        );

        FormatoLibro formato = new FormatoLibro(
                libro, Formato.PASTA_DURA, new BigDecimal("399.00")
        );

        // 10 unidades, mínimo 3.
        inventario = new ItemStock(formato, 10, 3);

        when(itemStockRepository.findByFormatoLibroId(5L))
                .thenReturn(Optional.of(inventario));

        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private AjusteInventarioDTO ajuste(MotivoAjuste motivo,
                                       Integer diferencia,
                                       String nota) {
        AjusteInventarioDTO dto = new AjusteInventarioDTO();
        dto.setMotivo(motivo);
        dto.setDiferencia(diferencia);
        dto.setNota(nota);
        return dto;
    }

    @Test
    void ajustar_reposicion_debeSumarYRegistrarMovimiento() {

        MovimientoInventario movimiento = servicio.ajustar(
                5L,
                ajuste(MotivoAjuste.REPOSICION, 5, "  Llegó pedido  "),
                " admin@raudskinna.com "
        );

        assertEquals(15, inventario.getCantidad());

        assertEquals(MotivoAjuste.REPOSICION, movimiento.getMotivo());
        assertEquals(5, movimiento.getDiferencia());
        assertEquals(10, movimiento.getCantidadAnterior());
        assertEquals(15, movimiento.getCantidadNueva());
        assertEquals("Llegó pedido", movimiento.getNota());
        assertEquals("admin@raudskinna.com", movimiento.getResponsable());
        assertNotNull(movimiento.getFecha());

        verify(movimientoRepository).save(any(MovimientoInventario.class));
    }

    @Test
    void ajustar_merma_debeRestarUnidades() {

        MovimientoInventario movimiento = servicio.ajustar(
                5L,
                ajuste(MotivoAjuste.MERMA, -4, null),
                "admin@raudskinna.com"
        );

        assertEquals(6, inventario.getCantidad());
        assertEquals(-4, movimiento.getDiferencia());
        assertNull(movimiento.getNota());
    }

    @Test
    void ajustar_correccion_admiteDiferenciaPositivaONegativa() {

        servicio.ajustar(5L, ajuste(MotivoAjuste.CORRECCION, 2, null), "a@b.com");
        assertEquals(12, inventario.getCantidad());

        servicio.ajustar(5L, ajuste(MotivoAjuste.CORRECCION, -12, null), "a@b.com");
        assertEquals(0, inventario.getCantidad());
    }

    @Test
    void ajustar_quedandoEnNegativo_debeRechazarYNoRegistrar() {

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> servicio.ajustar(
                        5L,
                        ajuste(MotivoAjuste.MERMA, -11, null),
                        "admin@raudskinna.com"
                )
        );

        assertTrue(e.getMessage().contains("negativo"));
        assertEquals(10, inventario.getCantidad());
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void ajustar_conDiferenciaCeroONula_debeRechazar() {

        assertThrows(IllegalArgumentException.class, () -> servicio.ajustar(
                5L, ajuste(MotivoAjuste.CORRECCION, 0, null), "a@b.com"));

        assertThrows(IllegalArgumentException.class, () -> servicio.ajustar(
                5L, ajuste(MotivoAjuste.CORRECCION, null, null), "a@b.com"));

        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void ajustar_reposicionNegativa_debeRechazar() {

        assertThrows(IllegalArgumentException.class, () -> servicio.ajustar(
                5L, ajuste(MotivoAjuste.REPOSICION, -1, null), "a@b.com"));

        assertEquals(10, inventario.getCantidad());
    }

    @Test
    void ajustar_mermaPositiva_debeRechazar() {

        assertThrows(IllegalArgumentException.class, () -> servicio.ajustar(
                5L, ajuste(MotivoAjuste.MERMA, 1, null), "a@b.com"));

        assertEquals(10, inventario.getCantidad());
    }

    @Test
    void ajustar_sinMotivo_debeRechazar() {

        assertThrows(IllegalArgumentException.class, () -> servicio.ajustar(
                5L, ajuste(null, 3, null), "a@b.com"));
    }

    @Test
    void ajustar_sinResponsable_debeRechazar() {

        assertThrows(IllegalArgumentException.class, () -> servicio.ajustar(
                5L, ajuste(MotivoAjuste.REPOSICION, 3, null), " "));

        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void ajustar_conNotaMuyLarga_debeRechazar() {

        String nota = "x".repeat(InventarioService.NOTA_MAXIMA + 1);

        assertThrows(IllegalArgumentException.class, () -> servicio.ajustar(
                5L, ajuste(MotivoAjuste.REPOSICION, 3, nota), "a@b.com"));
    }

    @Test
    void ajustar_conIdentificadorInvalido_debeRechazar() {

        assertThrows(IllegalArgumentException.class, () -> servicio.ajustar(
                null, ajuste(MotivoAjuste.REPOSICION, 3, null), "a@b.com"));

        assertThrows(IllegalArgumentException.class, () -> servicio.ajustar(
                0L, ajuste(MotivoAjuste.REPOSICION, 3, null), "a@b.com"));
    }

    @Test
    void ajustar_formatoDigital_debeExplicarQueNoTieneInventario() {

        Libro libro = new Libro("Dune", "9780000000001",
                new Categoria("Ficción"), new Editorial("Ace", null));

        when(itemStockRepository.findByFormatoLibroId(7L))
                .thenReturn(Optional.empty());

        when(formatoLibroRepository.findById(7L))
                .thenReturn(Optional.of(new FormatoLibro(
                        libro, Formato.DIGITAL, new BigDecimal("99.00"))));

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> servicio.ajustar(
                        7L, ajuste(MotivoAjuste.REPOSICION, 3, null), "a@b.com")
        );

        assertTrue(e.getMessage().toLowerCase().contains("digital"));
    }

    @Test
    void ajustar_formatoInexistente_debeRechazar() {

        when(itemStockRepository.findByFormatoLibroId(9L))
                .thenReturn(Optional.empty());
        when(formatoLibroRepository.findById(9L))
                .thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> servicio.ajustar(
                        9L, ajuste(MotivoAjuste.REPOSICION, 3, null), "a@b.com")
        );

        assertTrue(e.getMessage().contains("no existe"));
    }

    @Test
    void listarMovimientos_conIdentificadorInvalido_debeRechazar() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.listarMovimientos(-1L));
    }
}
