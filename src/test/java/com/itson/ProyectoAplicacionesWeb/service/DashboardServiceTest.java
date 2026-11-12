package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.DashboardResumenDTO;
import com.itson.ProyectoAplicacionesWeb.dto.ProductoMasVendidoDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.DetalleOrdenRepository;
import com.itson.ProyectoAplicacionesWeb.repository.ItemStockRepository;
import com.itson.ProyectoAplicacionesWeb.repository.LibroRepository;
import com.itson.ProyectoAplicacionesWeb.repository.OrdenCompraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DashboardServiceTest {
    @Mock
    private LibroRepository libroRepository;

    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @Mock
    private ItemStockRepository itemStockRepository;

    @Mock
    private DetalleOrdenRepository detalleOrdenRepository;

    private DashboardService servicio;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);

        servicio = new DashboardService(
                libroRepository,
                ordenCompraRepository,
                itemStockRepository,
                detalleOrdenRepository
        );
    }

    @Test
    void obtenerResumen_debeReunirLosIndicadores() {

        Libro libro = new Libro("Drácula", "9781234567897",
                new Categoria("Terror"), new Editorial("Penguin", null));
        ReflectionTestUtils.setField(libro, "id", 1L);

        FormatoLibro formato = new FormatoLibro(
                libro, Formato.PASTA_DURA, new BigDecimal("399.00"));
        ReflectionTestUtils.setField(formato, "id", 2L);

        ItemStock stockBajo = new ItemStock(formato, 1, 3);

        when(libroRepository.count()).thenReturn(8L);
        when(ordenCompraRepository.countByFechaBetween(any(), any())).thenReturn(2L);
        when(ordenCompraRepository.countByEstado(EstadoOrden.PENDIENTE)).thenReturn(4L);
        when(ordenCompraRepository.sumarVentas(
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(EstadoOrden.CANCELADO)))
                .thenReturn(new BigDecimal("1250.50"));
        when(itemStockRepository.listarConStockBajo()).thenReturn(List.of(stockBajo));
        when(detalleOrdenRepository.listarMasVendidos(
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(EstadoOrden.CANCELADO), any(Pageable.class)))
                .thenReturn(List.of(new ProductoMasVendidoDTO(1L, "Drácula", 7L)));

        DashboardResumenDTO resumen = servicio.obtenerResumen(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 30)
        );

        assertEquals(8L, resumen.totalLibros());
        assertEquals(2L, resumen.ordenesHoy());
        assertEquals(4L, resumen.ordenesPendientes());
        assertEquals(new BigDecimal("1250.50"), resumen.ventasPeriodo());
        assertEquals(LocalDate.of(2026, 10, 1), resumen.desde());
        assertEquals(LocalDate.of(2026, 10, 30), resumen.hasta());

        assertEquals(1, resumen.stockBajo().size());
        assertEquals("Drácula", resumen.stockBajo().get(0).titulo());
        assertEquals("PASTA_DURA", resumen.stockBajo().get(0).formato());
        assertEquals(1, resumen.stockBajo().get(0).cantidad());
        assertEquals(3, resumen.stockBajo().get(0).stockMinimo());

        assertEquals(1, resumen.masVendidos().size());
        assertEquals(7L, resumen.masVendidos().get(0).unidades());
    }

    @Test
    void obtenerResumen_sinVentas_debeDevolverCero() {

        when(ordenCompraRepository.sumarVentas(any(), any(), any()))
                .thenReturn(null);
        when(itemStockRepository.listarConStockBajo()).thenReturn(List.of());
        when(detalleOrdenRepository.listarMasVendidos(any(), any(), any(), any()))
                .thenReturn(List.of());

        DashboardResumenDTO resumen = servicio.obtenerResumen(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 30)
        );

        assertEquals(BigDecimal.ZERO, resumen.ventasPeriodo());
        assertTrue(resumen.stockBajo().isEmpty());
        assertTrue(resumen.masVendidos().isEmpty());
    }

    @Test
    void obtenerResumen_sinFechas_usaMesActualHastaHoy() {

        when(itemStockRepository.listarConStockBajo()).thenReturn(List.of());
        when(detalleOrdenRepository.listarMasVendidos(any(), any(), any(), any()))
                .thenReturn(List.of());

        DashboardResumenDTO resumen = servicio.obtenerResumen(null, null);

        LocalDate hoy = LocalDate.now();

        assertEquals(hoy.withDayOfMonth(1), resumen.desde());
        assertEquals(hoy, resumen.hasta());
    }

    @Test
    void obtenerResumen_conPeriodoInvertido_debeRechazar() {

        assertThrows(IllegalArgumentException.class, () ->
                servicio.obtenerResumen(
                        LocalDate.of(2026, 10, 30),
                        LocalDate.of(2026, 10, 1)
                ));
    }
}
