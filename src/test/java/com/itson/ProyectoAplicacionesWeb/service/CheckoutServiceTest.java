
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

    @Mock
    private PagoService pagoService;

    @BeforeEach
    void configurar() {

        checkoutService = new CheckoutService(
                carritoCompraRepository,
                ordenCompraRepository,
                detalleOrdenRepository,
                usuarioRepository,
                inventarioService,
                direccionRepository,
                pagoService
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
                        null,
                        MetodoPago.TRANSFERENCIA
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

        verifyNoInteractions(pagoService);
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
                        direccionAjenaId,
                        MetodoPago.TRANSFERENCIA
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
        verifyNoInteractions(pagoService);
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
        checkoutService.confirmarCompra(correo, direccionId, MetodoPago.TRANSFERENCIA);

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

        verify(pagoService).registrarPago(
                any(OrdenCompra.class),
                eq(MetodoPago.TRANSFERENCIA)
        );

        // El carrito debe quedar vacío.
        assertTrue(articulos.isEmpty());
    }

    @Test
    void confirmarCompra_digitalSinDireccion_noDescuentaInventario() {

        // ARRANGE: preparar una compra exclusivamente digital.
        String correo = "cliente@raudskinna.com";
        Long usuarioId = 1L;

        Usuario usuario = mock(Usuario.class);
        CarritoCompra carrito = mock(CarritoCompra.class);
        ItemCarrito item = mock(ItemCarrito.class);
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        Libro libro = mock(Libro.class);

        List<ItemCarrito> articulos = new ArrayList<>();
        articulos.add(item);

        when(usuarioRepository.findByCorreo(correo))
                .thenReturn(Optional.of(usuario));

        when(usuario.getId()).thenReturn(usuarioId);

        when(carritoCompraRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(carrito));

        when(carrito.getItems()).thenReturn(articulos);

        when(item.getFormatoLibro()).thenReturn(formatoLibro);
        when(item.getCantidad()).thenReturn(1);

        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(Formato.DIGITAL);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("199.00"));

        when(libro.isActivo()).thenReturn(true);

        when(ordenCompraRepository.save(any(OrdenCompra.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        // ACT: confirmar sin seleccionar dirección.
        checkoutService.confirmarCompra(correo, null, MetodoPago.TRANSFERENCIA);

        // ASSERT: capturar la orden registrada.
        ArgumentCaptor<OrdenCompra> captor =
                ArgumentCaptor.forClass(OrdenCompra.class);

        verify(ordenCompraRepository).save(captor.capture());

        OrdenCompra orden = captor.getValue();

        // Una compra digital no necesita domicilio.
        assertNull(orden.getDireccionEnvio());

        // El total debe calcularse correctamente.
        assertEquals(
                0,
                new BigDecimal("199.00").compareTo(orden.getTotal())
        );

        // Debe registrarse el detalle de compra.
        verify(detalleOrdenRepository)
                .save(any(DetalleOrden.class));

        // No debe consultar direcciones ni descontar inventario.
        verifyNoInteractions(direccionRepository);
        verifyNoInteractions(inventarioService);
        verify(pagoService).registrarPago(
                any(OrdenCompra.class),
                eq(MetodoPago.TRANSFERENCIA)
        );

        // El carrito queda vacío al finalizar.
        assertTrue(articulos.isEmpty());
    }

    @Test
    void confirmarCompra_mixtaConDireccion_descuentaSoloInventarioFisico() {

        // ARRANGE: preparar usuario y carrito mixto.
        String correo = "cliente@raudskinna.com";
        Long usuarioId = 1L;
        Long direccionId = 25L;

        Usuario usuario = mock(Usuario.class);
        CarritoCompra carrito = mock(CarritoCompra.class);

        ItemCarrito itemFisico = mock(ItemCarrito.class);
        ItemCarrito itemDigital = mock(ItemCarrito.class);

        FormatoLibro formatoFisico = mock(FormatoLibro.class);
        FormatoLibro formatoDigital = mock(FormatoLibro.class);

        Libro libroFisico = mock(Libro.class);
        Libro libroDigital = mock(Libro.class);

        Direccion direccion = mock(Direccion.class);

        Formato tipoFisico = Arrays.stream(Formato.values())
                .filter(f -> f != Formato.DIGITAL)
                .findFirst()
                .orElseThrow();

        List<ItemCarrito> articulos = new ArrayList<>();
        articulos.add(itemFisico);
        articulos.add(itemDigital);

        when(usuarioRepository.findByCorreo(correo))
                .thenReturn(Optional.of(usuario));
        when(usuario.getId()).thenReturn(usuarioId);

        when(carritoCompraRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(carrito));
        when(carrito.getItems()).thenReturn(articulos);

        // Libro físico.
        when(itemFisico.getFormatoLibro()).thenReturn(formatoFisico);
        when(itemFisico.getCantidad()).thenReturn(1);

        when(formatoFisico.getId()).thenReturn(10L);
        when(formatoFisico.getLibro()).thenReturn(libroFisico);
        when(formatoFisico.getFormato()).thenReturn(tipoFisico);
        when(formatoFisico.getPrecio())
                .thenReturn(new BigDecimal("350.00"));

        when(libroFisico.isActivo()).thenReturn(true);

        // Libro digital.
        when(itemDigital.getFormatoLibro()).thenReturn(formatoDigital);
        when(itemDigital.getCantidad()).thenReturn(1);

        when(formatoDigital.getId()).thenReturn(20L);
        when(formatoDigital.getLibro()).thenReturn(libroDigital);
        when(formatoDigital.getFormato()).thenReturn(Formato.DIGITAL);
        when(formatoDigital.getPrecio())
                .thenReturn(new BigDecimal("199.00"));

        when(libroDigital.isActivo()).thenReturn(true);

        // Dirección válida del cliente.
        when(direccionRepository.findByIdAndUsuario_Id(
                direccionId, usuarioId
        )).thenReturn(Optional.of(direccion));

        when(direccion.getNombreDestinatario())
                .thenReturn("H. P. Lovecraft");
        when(direccion.getCalle()).thenReturn("Calle Hidalgo");
        when(direccion.getNumeroExterior()).thenReturn("123");
        when(direccion.getCodigoPostal()).thenReturn("85000");
        when(direccion.getCiudad()).thenReturn("Ciudad Obregón");

        // Simular guardado de la orden.
        when(ordenCompraRepository.save(any(OrdenCompra.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // ACT: confirmar la compra mixta.
        checkoutService.confirmarCompra(correo, direccionId, MetodoPago.TRANSFERENCIA);

        // ASSERT: capturar la orden.
        ArgumentCaptor<OrdenCompra> captor =
                ArgumentCaptor.forClass(OrdenCompra.class);

        verify(ordenCompraRepository).save(captor.capture());

        OrdenCompra orden = captor.getValue();

        assertEquals(
                0,
                new BigDecimal("549.00").compareTo(orden.getTotal())
        );

        // La orden conserva su dirección histórica.
        assertNotNull(orden.getDireccionEnvio());
        assertEquals(
                "Calle Hidalgo",
                orden.getDireccionEnvio().getCalle()
        );

        // Se registran ambos artículos.
        verify(detalleOrdenRepository, times(2))
                .save(any(DetalleOrden.class));

        // Solamente el libro físico descuenta inventario.
        verify(inventarioService).descontarPorVenta(
                10L, 1, correo
        );

        verifyNoMoreInteractions(inventarioService);

        verify(pagoService).registrarPago(
                any(OrdenCompra.class),
                eq(MetodoPago.TRANSFERENCIA)
        );

        // El carrito queda vacío.
        assertTrue(articulos.isEmpty());
    }

    @Test
    void confirmarCompra_direccionInexistente_rechazaOperacion() {

        // ARRANGE
        String correo = "cliente@raudskinna.com";
        Long usuarioId = 1L;
        Long direccionInexistenteId = 999L;

        Usuario usuario = mock(Usuario.class);
        CarritoCompra carrito = mock(CarritoCompra.class);
        ItemCarrito item = mock(ItemCarrito.class);
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        Libro libro = mock(Libro.class);

        Formato formatoFisico = Arrays.stream(Formato.values())
                .filter(f -> f != Formato.DIGITAL)
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

        // No existe una dirección con ese ID para el usuario.
        when(direccionRepository.findByIdAndUsuario_Id(
                direccionInexistenteId,
                usuarioId
        )).thenReturn(Optional.empty());

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.confirmarCompra(
                        correo,
                        direccionInexistenteId,
                        MetodoPago.TRANSFERENCIA
                )
        );

        // ASSERT
        assertEquals(
                "La dirección no existe o no pertenece al cliente.",
                excepcion.getMessage()
        );

        verify(direccionRepository).findByIdAndUsuario_Id(
                direccionInexistenteId,
                usuarioId
        );

        // La compra no debe continuar.
        verifyNoInteractions(ordenCompraRepository);
        verifyNoInteractions(detalleOrdenRepository);
        verifyNoInteractions(inventarioService);
        verifyNoInteractions(pagoService);
    }

    @Test
    void confirmarCompra_direccionIdInvalido_rechazaOperacion() {

        // ARRANGE
        String correo = "cliente@raudskinna.com";
        Long usuarioId = 1L;
        Long direccionIdInvalido = 0L;

        Usuario usuario = mock(Usuario.class);
        CarritoCompra carrito = mock(CarritoCompra.class);
        ItemCarrito item = mock(ItemCarrito.class);
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        Libro libro = mock(Libro.class);

        Formato formatoFisico = Arrays.stream(Formato.values())
                .filter(f -> f != Formato.DIGITAL)
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

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.confirmarCompra(
                        correo,
                        direccionIdInvalido,
                        MetodoPago.TRANSFERENCIA
                )
        );

        // ASSERT
        assertEquals(
                "El identificador de la dirección no es válido.",
                excepcion.getMessage()
        );

        // El ID se rechaza antes de consultar la dirección.
        verifyNoInteractions(direccionRepository);

        // No se debe registrar nada ni descontar inventario.
        verifyNoInteractions(ordenCompraRepository);
        verifyNoInteractions(detalleOrdenRepository);
        verifyNoInteractions(inventarioService);
        verifyNoInteractions(pagoService);
    }

    @Test
    void confirmarCompra_inventarioInsuficiente_noVaciaCarrito() {

        // ARRANGE: preparar una compra física.
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

        List<ItemCarrito> articulos = new ArrayList<>();
        articulos.add(item);

        when(usuarioRepository.findByCorreo(correo))
                .thenReturn(Optional.of(usuario));

        when(usuario.getId()).thenReturn(usuarioId);

        when(carritoCompraRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(carrito));

        when(carrito.getItems()).thenReturn(articulos);

        when(item.getFormatoLibro()).thenReturn(formatoLibro);
        when(item.getCantidad()).thenReturn(5);

        when(formatoLibro.getId()).thenReturn(formatoId);
        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("350.00"));

        Formato formatoFisico = Arrays.stream(Formato.values())
                .filter(f -> f != Formato.DIGITAL)
                .findFirst()
                .orElseThrow();

        when(formatoLibro.getFormato()).thenReturn(formatoFisico);
        when(libro.isActivo()).thenReturn(true);

        // La dirección pertenece al cliente.
        when(direccionRepository.findByIdAndUsuario_Id(
                direccionId, usuarioId
        )).thenReturn(Optional.of(direccion));

        // Simular que el repositorio devuelve la orden creada.
        when(ordenCompraRepository.save(any(OrdenCompra.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Simular un error durante el descuento de inventario.
        doThrow(new IllegalArgumentException(
                "Existencias insuficientes. Disponibles: 2"
        )).when(inventarioService).descontarPorVenta(
                formatoId, 5, correo
        );

        // ACT: confirmar la compra.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.confirmarCompra(
                        correo,
                        direccionId,
                        MetodoPago.TRANSFERENCIA
                )
        );

        // ASSERT: comprobar que el error se propagó.
        assertEquals(
                "Existencias insuficientes. Disponibles: 2",
                excepcion.getMessage()
        );

        // El checkout alcanzó las operaciones de persistencia.
        verify(ordenCompraRepository)
                .save(any(OrdenCompra.class));

        verify(detalleOrdenRepository)
                .save(any(DetalleOrden.class));

        // Se intentó descontar inventario.
        verify(inventarioService).descontarPorVenta(
                formatoId, 5, correo
        );

        verifyNoInteractions(pagoService);

        // El carrito NO debe haberse vaciado.
        assertEquals(1, articulos.size());
        assertSame(item, articulos.get(0));
    }

}
