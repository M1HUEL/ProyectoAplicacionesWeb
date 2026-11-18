
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para la confirmación de compras.
 *
 * Utiliza Mockito para comprobar las reglas
 * de negocio sin modificar MySQL.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CarritoCompraRepository carritoCompraRepository;

    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @Mock
    private DetalleOrdenRepository detalleOrdenRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private DireccionRepository direccionRepository;

    private CheckoutService checkoutService;

    @BeforeEach
    void configurar() {

        checkoutService = new CheckoutService(
                carritoCompraRepository,
                ordenCompraRepository,
                detalleOrdenRepository,
                usuarioRepository,
                inventarioService,
                direccionRepository
        );
    }

    /**
     * Una compra que contiene un libro físico
     * debe rechazarse si no tiene dirección de envío.
     *
     * No debe registrar órdenes, detalles ni
     * realizar descuentos de inventario.
     */
    @Test
    void confirmarCompra_fisicaSinDireccion_rechazaOperacion() {

        // ARRANGE: preparar el escenario.

        String correo = "cliente@raudskinna.com";
        Long usuarioId = 1L;

        Usuario usuario = mock(Usuario.class);
        CarritoCompra carrito = mock(CarritoCompra.class);
        ItemCarrito item = mock(ItemCarrito.class);
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        Libro libro = mock(Libro.class);

        // Seleccionar un formato físico del enum existente.
        Formato formatoFisico = Arrays.stream(Formato.values())
                .filter(formato -> formato != Formato.DIGITAL)
                .findFirst()
                .orElseThrow();

        when(usuarioRepository.findByCorreo(correo))
                .thenReturn(Optional.of(usuario));

        when(usuario.getId()).thenReturn(usuarioId);

        when(carritoCompraRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(carrito));

        when(carrito.getItems()).thenReturn(List.of(item));

        when(item.getFormatoLibro()).thenReturn(formatoLibro);
        when(item.getCantidad()).thenReturn(1);

        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(formatoFisico);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("350.00"));

        when(libro.isActivo()).thenReturn(true);

        // ACT: intentar confirmar sin dirección.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.confirmarCompra(
                        correo,
                        null
                )
        );

        // ASSERT: verificar el rechazo.
        assertEquals(
                "Debes seleccionar una dirección de envío.",
                excepcion.getMessage()
        );

        // No debió consultar direcciones ni guardar la orden.
        verifyNoInteractions(direccionRepository);
        verifyNoInteractions(ordenCompraRepository);
        verifyNoInteractions(detalleOrdenRepository);

        // Tampoco debió descontar inventario.
        verifyNoInteractions(inventarioService);
    }

    @Test
    void confirmarCompra_direccionAjena_rechazaOperacion() {

        // ARRANGE
        String correo = "cliente@raudskinna.com";
        Long usuarioId = 1L;
        Long direccionAjenaId = 25L;

        Usuario usuario = mock(Usuario.class);
        CarritoCompra carrito = mock(CarritoCompra.class);
        ItemCarrito item = mock(ItemCarrito.class);
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        Libro libro = mock(Libro.class);

        Formato formatoFisico = Arrays.stream(Formato.values())
                .filter(formato -> formato != Formato.DIGITAL)
                .findFirst()
                .orElseThrow();

        when(usuarioRepository.findByCorreo(correo))
                .thenReturn(Optional.of(usuario));

        when(usuario.getId()).thenReturn(usuarioId);

        when(carritoCompraRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(carrito));

        when(carrito.getItems()).thenReturn(List.of(item));

        when(item.getFormatoLibro()).thenReturn(formatoLibro);
        when(item.getCantidad()).thenReturn(1);

        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(formatoFisico);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("350.00"));

        when(libro.isActivo()).thenReturn(true);

        // La dirección solicitada no pertenece al usuario.
        when(direccionRepository.findByIdAndUsuario_Id(
                direccionAjenaId,
                usuarioId
        )).thenReturn(Optional.empty());

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.confirmarCompra(
                        correo,
                        direccionAjenaId
                )
        );

        // ASSERT
        assertEquals(
                "La dirección no existe o no pertenece al cliente.",
                excepcion.getMessage()
        );

        verify(direccionRepository).findByIdAndUsuario_Id(
                direccionAjenaId,
                usuarioId
        );

        verifyNoInteractions(ordenCompraRepository);
        verifyNoInteractions(detalleOrdenRepository);
        verifyNoInteractions(inventarioService);
    }

    @Test
    void confirmarCompra_fisicaConDireccion_guardaCopiaHistorica() {

        // ARRANGE: usuario y carrito.
        String correo = "cliente@raudskinna.com";
        Long usuarioId = 1L;
        Long direccionId = 25L;
        Long formatoId = 10L;

        Usuario usuario = mock(Usuario.class);
        CarritoCompra carrito = mock(CarritoCompra.class);
        ItemCarrito item = mock(ItemCarrito.class);
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        Libro libro = mock(Libro.class);
        Direccion direccion = mock(Direccion.class);

        Formato formatoFisico = Arrays.stream(Formato.values())
                .filter(formato -> formato != Formato.DIGITAL)
                .findFirst()
                .orElseThrow();

        List<ItemCarrito> articulos = new ArrayList<>();
        articulos.add(item);

        when(usuarioRepository.findByCorreo(correo))
                .thenReturn(Optional.of(usuario));
        when(usuario.getId()).thenReturn(usuarioId);

        when(carritoCompraRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(carrito));
        when(carrito.getItems()).thenReturn(articulos);

        when(item.getFormatoLibro()).thenReturn(formatoLibro);
        when(item.getCantidad()).thenReturn(2);

        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(formatoFisico);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("350.00"));
        when(formatoLibro.getId()).thenReturn(formatoId);

        when(libro.isActivo()).thenReturn(true);

        // Dirección perteneciente al cliente autenticado.
        when(direccionRepository.findByIdAndUsuario_Id(
                direccionId, usuarioId
        )).thenReturn(Optional.of(direccion));

        when(direccion.getNombreDestinatario())
                .thenReturn("H. P. Lovecraft");
        when(direccion.getCalle()).thenReturn("Calle Hidalgo");
        when(direccion.getNumeroExterior()).thenReturn("123");
        when(direccion.getColonia()).thenReturn("Centro");
        when(direccion.getCodigoPostal()).thenReturn("85000");
        when(direccion.getCiudad()).thenReturn("Ciudad Obregón");
        when(direccion.getEstado()).thenReturn("Sonora");
        when(direccion.getPais()).thenReturn("México");

        // Simular la persistencia de la orden.
        when(ordenCompraRepository.save(any(OrdenCompra.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        // ACT: confirmar la compra.
        checkoutService.confirmarCompra(correo, direccionId);

        // ASSERT: recuperar la orden enviada al repositorio.
        ArgumentCaptor<OrdenCompra> captor =
                ArgumentCaptor.forClass(OrdenCompra.class);

        verify(ordenCompraRepository).save(captor.capture());

        OrdenCompra ordenGuardada = captor.getValue();
        DireccionEnvio envio = ordenGuardada.getDireccionEnvio();

        assertNotNull(envio);

        // Comprobar que se copiaron los datos.
        assertEquals("H. P. Lovecraft",
                envio.getNombreDestinatario());
        assertEquals("Calle Hidalgo", envio.getCalle());
        assertEquals("123", envio.getNumeroExterior());
        assertEquals("85000", envio.getCodigoPostal());
        assertEquals("Ciudad Obregón", envio.getCiudad());

        // Verificar el importe calculado por el servidor.
        assertEquals(0, new BigDecimal("700.00")
                .compareTo(ordenGuardada.getTotal()));

        // Comprobar que se registró el detalle.
        verify(detalleOrdenRepository)
                .save(any(DetalleOrden.class));

        // Los libros físicos descuentan inventario.
        verify(inventarioService).descontarPorVenta(
                formatoId, 2, correo
        );

        // El carrito debe quedar vacío.
        assertTrue(articulos.isEmpty());
    }

}
