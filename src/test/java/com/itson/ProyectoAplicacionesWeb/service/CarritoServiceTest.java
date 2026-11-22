
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.*;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio encargado de gestionar
 * el carrito de compras de Librería Raudskinna.
 *
 * Se comprueban las operaciones de consulta, incorporación,
 * modificación y eliminación de artículos, así como las
 * reglas de disponibilidad comercial e inventario físico.
 *
 * Los repositorios se simulan mediante Mockito, por lo que
 * estas pruebas no requieren conexión a una base de datos.
 */
@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    private static final String CORREO = "cliente@raudskinna.test";

    private static final Long USUARIO_ID = 1L;
    private static final Long CARRITO_ID = 10L;
    private static final Long FORMATO_ID = 20L;
    private static final Long ITEM_ID = 30L;

    private static final BigDecimal PRECIO =
            new BigDecimal("350.00");

    @Mock
    private CarritoCompraRepository carritoCompraRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ItemCarritoRepository itemCarritoRepository;

    @Mock
    private FormatoLibroRepository formatoLibroRepository;

    @Mock
    private ItemStockRepository itemStockRepository;

    @InjectMocks
    private CarritoService carritoService;

    private Usuario usuario;
    private Libro libro;
    private FormatoLibro formatoLibro;
    private CarritoCompra carrito;

    /**
     * Prepara entidades independientes antes de cada prueba.
     *
     * Los identificadores normalmente generados por JPA
     * se asignan mediante ReflectionTestUtils.
     */
    @BeforeEach
    void setUp() {

        usuario = new Usuario(
                USUARIO_ID,
                "Cliente de prueba",
                CORREO,
                "contrasena-de-prueba",
                Rol.USER
        );

        Categoria categoria = new Categoria("Terror");

        Editorial editorial = new Editorial(
                "Editorial de prueba",
                "Dirección de prueba"
        );

        libro = new Libro(
                "La llamada de Cthulhu",
                "9781234567890",
                categoria,
                editorial
        );

        libro.setActivo(true);

        formatoLibro = new FormatoLibro(
                libro,
                Formato.PASTA_DURA,
                PRECIO
        );

        ReflectionTestUtils.setField(
                formatoLibro,
                "id",
                FORMATO_ID
        );

        carrito = new CarritoCompra(usuario);

        ReflectionTestUtils.setField(
                carrito,
                "id",
                CARRITO_ID
        );
    }

    // =====================================================
    // MÉTODOS AUXILIARES PARA PREPARAR ESCENARIOS
    // =====================================================

    /**
     * Simula la existencia del usuario autenticado
     * y de su carrito de compras.
     */
    private void prepararCarritoExistente() {

        when(usuarioRepository.findByCorreo(CORREO))
                .thenReturn(Optional.of(usuario));

        when(carritoCompraRepository.findByUsuarioId(USUARIO_ID))
                .thenReturn(Optional.of(carrito));
    }

    /**
     * Simula la recuperación de la presentación comercial
     * solicitada por el cliente.
     */
    private void prepararFormatoExistente() {

        when(formatoLibroRepository.findById(FORMATO_ID))
                .thenReturn(Optional.of(formatoLibro));
    }

    /**
     * Simula las existencias físicas de una presentación.
     */
    private ItemStock prepararInventario(int cantidad) {

        ItemStock inventario = new ItemStock(
                formatoLibro,
                cantidad,
                1
        );

        when(itemStockRepository.findByFormatoLibroId(FORMATO_ID))
                .thenReturn(Optional.of(inventario));

        return inventario;
    }

    /**
     * Incorpora un artículo al carrito utilizado en las pruebas
     * y simula su recuperación desde el repositorio.
     */
    private ItemCarrito prepararItemExistente(int cantidad) {

        ItemCarrito item = new ItemCarrito(
                carrito,
                formatoLibro,
                cantidad
        );

        ReflectionTestUtils.setField(
                item,
                "id",
                ITEM_ID
        );

        carrito.getItems().add(item);

        when(itemCarritoRepository
                .findByCarritoCompraIdAndFormatoLibroId(
                        CARRITO_ID,
                        FORMATO_ID
                ))
                .thenReturn(Optional.of(item));

        return item;
    }

    /**
     * Simula que la presentación comercial todavía
     * no se encuentra agregada al carrito.
     */
    private void prepararItemInexistente() {

        when(itemCarritoRepository
                .findByCarritoCompraIdAndFormatoLibroId(
                        CARRITO_ID,
                        FORMATO_ID
                ))
                .thenReturn(Optional.empty());
    }

    // =====================================================
    // PRUEBAS DE CONSULTA DEL CARRITO
    // =====================================================

    /**
     * PC-01: Un cliente sin carrito debe recibir
     * una respuesta vacía con total igual a cero.
     */
    @Test
    void consultarCarrito_sinCarrito_retornaCarritoVacio() {

        // ARRANGE
        when(usuarioRepository.findByCorreo(CORREO))
                .thenReturn(Optional.of(usuario));

        when(carritoCompraRepository.findByUsuarioId(USUARIO_ID))
                .thenReturn(Optional.empty());

        // ACT
        CarritoResponseDTO respuesta =
                carritoService.consultarCarrito(CORREO);

        // ASSERT
        assertNull(respuesta.carritoId());
        assertTrue(respuesta.items().isEmpty());
        assertEquals(BigDecimal.ZERO, respuesta.total());

        verifyNoInteractions(itemCarritoRepository);
        verifyNoInteractions(itemStockRepository);
    }

    /**
     * PC-02: El servicio debe calcular los subtotales
     * y el total del carrito utilizando los precios
     * comerciales registrados en el servidor.
     */
    @Test
    void consultarCarrito_conArticulos_calculaTotalCorrectamente() {

        // ARRANGE
        prepararCarritoExistente();

        carrito.getItems().add(
                new ItemCarrito(carrito, formatoLibro, 2)
        );

        FormatoLibro formatoDigital = new FormatoLibro(
                libro,
                Formato.DIGITAL,
                new BigDecimal("150.00")
        );

        ReflectionTestUtils.setField(
                formatoDigital,
                "id",
                21L
        );

        carrito.getItems().add(
                new ItemCarrito(carrito, formatoDigital, 1)
        );

        // ACT
        CarritoResponseDTO respuesta =
                carritoService.consultarCarrito(CORREO);

        // ASSERT
        assertEquals(CARRITO_ID, respuesta.carritoId());
        assertEquals(2, respuesta.items().size());

        ItemCarritoResponseDTO primerItem =
                respuesta.items().getFirst();

        assertEquals(FORMATO_ID, primerItem.formatoLibroId());
        assertEquals("La llamada de Cthulhu", primerItem.titulo());
        assertEquals("PASTA_DURA", primerItem.formato());
        assertEquals(2, primerItem.cantidad());
        assertEquals(PRECIO, primerItem.precioUnitario());

        assertEquals(
                new BigDecimal("700.00"),
                primerItem.subtotal()
        );

        assertEquals(
                new BigDecimal("850.00"),
                respuesta.total()
        );
    }

    // =====================================================
    // PRUEBAS DE INCORPORACIÓN DE ARTÍCULOS
    // =====================================================

    /**
     * PC-03: Un formato digital puede agregarse
     * sin consultar existencias físicas.
     *
     * También comprueba la creación automática
     * del carrito cuando el cliente todavía no tiene uno.
     */
    @Test
    void agregarItem_formatoDigital_creaCarritoSinConsultarStock() {

        // ARRANGE
        formatoLibro.setFormato(Formato.DIGITAL);

        when(usuarioRepository.findByCorreo(CORREO))
                .thenReturn(Optional.of(usuario));

        when(carritoCompraRepository.findByUsuarioId(USUARIO_ID))
                .thenReturn(
                        Optional.empty(),
                        Optional.of(carrito)
                );

        when(carritoCompraRepository.save(any(CarritoCompra.class)))
                .thenAnswer(invocacion -> {

                    CarritoCompra nuevoCarrito =
                            invocacion.getArgument(0);

                    ReflectionTestUtils.setField(
                            nuevoCarrito,
                            "id",
                            CARRITO_ID
                    );

                    carrito = nuevoCarrito;

                    when(carritoCompraRepository
                            .findByUsuarioId(USUARIO_ID))
                            .thenReturn(Optional.of(nuevoCarrito));

                    return nuevoCarrito;
                });

        prepararFormatoExistente();
        prepararItemInexistente();

        AgregarItemCarritoRequestDTO solicitud =
                new AgregarItemCarritoRequestDTO(FORMATO_ID, 1);

        // ACT
        CarritoResponseDTO respuesta =
                carritoService.agregarItem(CORREO, solicitud);

        // ASSERT
        assertEquals(CARRITO_ID, respuesta.carritoId());
        assertEquals(1, respuesta.items().size());
        assertEquals("DIGITAL", respuesta.items().getFirst().formato());
        assertEquals(PRECIO, respuesta.total());

        verify(carritoCompraRepository)
                .save(any(CarritoCompra.class));

        verify(itemCarritoRepository)
                .save(any(ItemCarrito.class));

        verifyNoInteractions(itemStockRepository);
    }

    /**
     * PC-04: Una presentación física puede agregarse
     * cuando existen suficientes unidades disponibles.
     *
     * Agregar al carrito no debe descontar inventario.
     */
    @Test
    void agregarItem_formatoFisicoDisponible_agregaArticulo() {

        // ARRANGE
        prepararCarritoExistente();
        prepararFormatoExistente();
        prepararItemInexistente();

        ItemStock inventario = prepararInventario(5);

        AgregarItemCarritoRequestDTO solicitud =
                new AgregarItemCarritoRequestDTO(FORMATO_ID, 2);

        // ACT
        CarritoResponseDTO respuesta =
                carritoService.agregarItem(CORREO, solicitud);

        // ASSERT
        assertEquals(1, respuesta.items().size());
        assertEquals(2, respuesta.items().getFirst().cantidad());

        assertEquals(
                new BigDecimal("700.00"),
                respuesta.total()
        );

        assertEquals(5, inventario.getCantidad());

        verify(itemCarritoRepository)
                .save(any(ItemCarrito.class));

        verify(itemStockRepository)
                .findByFormatoLibroId(FORMATO_ID);

        verify(itemStockRepository, never())
                .save(any(ItemStock.class));
    }

    /**
     * PC-05: Un libro inactivo no debe incorporarse
     * al carrito de compras.
     */
    @Test
    void agregarItem_libroInactivo_rechazaSolicitud() {

        // ARRANGE
        prepararCarritoExistente();
        prepararFormatoExistente();
        prepararItemInexistente();

        libro.setActivo(false);

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.agregarItem(
                        CORREO,
                        new AgregarItemCarritoRequestDTO(FORMATO_ID, 1)
                )
        );

        // ASSERT
        assertEquals(
                "El libro solicitado ya no está disponible.",
                excepcion.getMessage()
        );

        assertTrue(carrito.getItems().isEmpty());

        verifyNoInteractions(itemStockRepository);

        verify(itemCarritoRepository, never())
                .save(any(ItemCarrito.class));
    }

    /**
     * PC-06: Una presentación física sin registro
     * de inventario debe rechazarse.
     */
    @Test
    void agregarItem_sinInventario_rechazaSolicitud() {

        // ARRANGE
        prepararCarritoExistente();
        prepararFormatoExistente();
        prepararItemInexistente();

        when(itemStockRepository.findByFormatoLibroId(FORMATO_ID))
                .thenReturn(Optional.empty());

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.agregarItem(
                        CORREO,
                        new AgregarItemCarritoRequestDTO(FORMATO_ID, 1)
                )
        );

        // ASSERT
        assertEquals(
                "La presentación no tiene inventario registrado.",
                excepcion.getMessage()
        );

        assertTrue(carrito.getItems().isEmpty());

        verify(itemCarritoRepository, never())
                .save(any(ItemCarrito.class));
    }

    /**
     * PC-07: Una presentación agotada no puede
     * incorporarse al carrito.
     */
    @Test
    void agregarItem_formatoAgotado_rechazaSolicitud() {

        // ARRANGE
        prepararCarritoExistente();
        prepararFormatoExistente();
        prepararItemInexistente();
        prepararInventario(0);

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.agregarItem(
                        CORREO,
                        new AgregarItemCarritoRequestDTO(FORMATO_ID, 1)
                )
        );

        // ASSERT
        assertEquals(
                "La presentación solicitada está agotada.",
                excepcion.getMessage()
        );

        assertTrue(carrito.getItems().isEmpty());

        verify(itemCarritoRepository, never())
                .save(any(ItemCarrito.class));
    }

    /**
     * PC-08: La cantidad solicitada no debe superar
     * las existencias físicas disponibles.
     */
    @Test
    void agregarItem_cantidadSuperiorAlStock_rechazaSolicitud() {

        // ARRANGE
        prepararCarritoExistente();
        prepararFormatoExistente();
        prepararItemInexistente();
        prepararInventario(3);

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.agregarItem(
                        CORREO,
                        new AgregarItemCarritoRequestDTO(FORMATO_ID, 4)
                )
        );

        // ASSERT
        assertEquals(
                "La cantidad solicitada supera las existencias disponibles.",
                excepcion.getMessage()
        );

        assertTrue(carrito.getItems().isEmpty());

        verify(itemCarritoRepository, never())
                .save(any(ItemCarrito.class));
    }

    /**
     * PC-09: Agregar una presentación ya existente
     * debe incrementar la cantidad sin duplicar artículos.
     *
     * Se comprueba también el límite exacto del inventario.
     */
    @Test
    void agregarItem_cantidadAcumuladaValida_incrementaCantidad() {

        // ARRANGE
        prepararCarritoExistente();
        prepararFormatoExistente();

        ItemCarrito item = prepararItemExistente(2);
        ItemStock inventario = prepararInventario(5);

        // ACT
        CarritoResponseDTO respuesta = carritoService.agregarItem(
                CORREO,
                new AgregarItemCarritoRequestDTO(FORMATO_ID, 3)
        );

        // ASSERT
        assertEquals(1, carrito.getItems().size());
        assertEquals(5, item.getCantidad());
        assertEquals(5, respuesta.items().getFirst().cantidad());

        assertEquals(
                new BigDecimal("1750.00"),
                respuesta.total()
        );

        assertEquals(5, inventario.getCantidad());

        verify(itemCarritoRepository).save(item);
    }

    /**
     * PC-10: La suma de la cantidad existente y la nueva
     * no debe superar las existencias disponibles.
     *
     * Una solicitud rechazada no debe alterar el carrito.
     */
    @Test
    void agregarItem_cantidadAcumuladaSuperiorAlStock_rechazaSolicitud() {

        // ARRANGE
        prepararCarritoExistente();
        prepararFormatoExistente();

        ItemCarrito item = prepararItemExistente(3);

        prepararInventario(5);

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.agregarItem(
                        CORREO,
                        new AgregarItemCarritoRequestDTO(FORMATO_ID, 3)
                )
        );

        // ASSERT
        assertEquals(
                "La cantidad solicitada supera las existencias disponibles.",
                excepcion.getMessage()
        );

        assertEquals(3, item.getCantidad());
        assertEquals(1, carrito.getItems().size());

        verify(itemCarritoRepository, never())
                .save(any(ItemCarrito.class));
    }

    // =====================================================
    // PRUEBAS DE MODIFICACIÓN DE CANTIDADES
    // =====================================================

    /**
     * PC-11: Modificar la cantidad de un artículo
     * debe reemplazar la cantidad anterior y recalcular
     * el importe total del carrito.
     */
    @Test
    void modificarCantidad_cantidadValida_actualizaCarrito() {

        // ARRANGE
        prepararCarritoExistente();

        ItemCarrito item = prepararItemExistente(2);
        ItemStock inventario = prepararInventario(5);

        // ACT
        CarritoResponseDTO respuesta =
                carritoService.modificarCantidad(
                        CORREO,
                        FORMATO_ID,
                        new ModificarCantidadCarritoRequestDTO(4)
                );

        // ASSERT
        assertEquals(4, item.getCantidad());
        assertEquals(4, respuesta.items().getFirst().cantidad());

        assertEquals(
                new BigDecimal("1400.00"),
                respuesta.total()
        );

        assertEquals(5, inventario.getCantidad());

        verify(itemStockRepository)
                .findByFormatoLibroId(FORMATO_ID);
    }

    /**
     * PC-12: Modificar la cantidad por encima del stock
     * debe rechazarse sin alterar la cantidad anterior.
     */
    @Test
    void modificarCantidad_cantidadSuperiorAlStock_rechazaSolicitud() {

        // ARRANGE
        prepararCarritoExistente();

        ItemCarrito item = prepararItemExistente(2);

        prepararInventario(5);

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.modificarCantidad(
                        CORREO,
                        FORMATO_ID,
                        new ModificarCantidadCarritoRequestDTO(6)
                )
        );

        // ASSERT
        assertEquals(
                "La cantidad solicitada supera las existencias disponibles.",
                excepcion.getMessage()
        );

        assertEquals(2, item.getCantidad());
    }

    // =====================================================
    // PRUEBAS DE ELIMINACIÓN DE ARTÍCULOS
    // =====================================================

    /**
     * PC-13: Un cliente debe poder retirar un artículo,
     * incluso cuando su libro ya no está activo.
     *
     * La eliminación no requiere consultar inventario.
     */
    @Test
    void eliminarItem_libroInactivo_retiraArticuloDelCarrito() {

        // ARRANGE
        prepararCarritoExistente();
        prepararItemExistente(2);

        libro.setActivo(false);

        // ACT
        CarritoResponseDTO respuesta =
                carritoService.eliminarItem(
                        CORREO,
                        FORMATO_ID
                );

        // ASSERT
        assertEquals(CARRITO_ID, respuesta.carritoId());
        assertTrue(carrito.getItems().isEmpty());
        assertTrue(respuesta.items().isEmpty());
        assertEquals(BigDecimal.ZERO, respuesta.total());

        verifyNoInteractions(itemStockRepository);
    }

    // =====================================================
    // PRUEBAS DE VALIDACIÓN DE DATOS
    // =====================================================

    /**
     * PC-14: No se permite agregar artículos
     * con una cantidad igual a cero.
     */
    @Test
    void agregarItem_cantidadCero_rechazaSolicitud() {

        // ARRANGE
        AgregarItemCarritoRequestDTO solicitud =
                new AgregarItemCarritoRequestDTO(FORMATO_ID, 0);

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.agregarItem(CORREO, solicitud)
        );

        // ASSERT
        assertEquals(
                "Los datos del artículo no son válidos.",
                excepcion.getMessage()
        );

        verifyNoInteractions(
                usuarioRepository,
                carritoCompraRepository,
                formatoLibroRepository,
                itemCarritoRepository,
                itemStockRepository
        );
    }

    /**
     * PC-15: No se permite agregar artículos
     * con cantidades negativas.
     */
    @Test
    void agregarItem_cantidadNegativa_rechazaSolicitud() {

        // ARRANGE
        AgregarItemCarritoRequestDTO solicitud =
                new AgregarItemCarritoRequestDTO(FORMATO_ID, -1);

        // ACT
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.agregarItem(CORREO, solicitud)
        );

        // ASSERT
        assertEquals(
                "Los datos del artículo no son válidos.",
                excepcion.getMessage()
        );

        verifyNoInteractions(
                usuarioRepository,
                carritoCompraRepository,
                formatoLibroRepository,
                itemCarritoRepository,
                itemStockRepository
        );
    }
}
