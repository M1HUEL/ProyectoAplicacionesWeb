
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.CheckoutResponseDTO;
import com.itson.ProyectoAplicacionesWeb.exception.InventarioInsuficienteException;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integrationtest")
class CheckoutIntegrationTest {

    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private EditorialRepository editorialRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private FormatoLibroRepository formatoLibroRepository;
    @Autowired private ItemStockRepository itemStockRepository;
    @Autowired private CarritoCompraRepository carritoCompraRepository;
    @Autowired private DireccionRepository direccionRepository;
    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private DetalleOrdenRepository detalleOrdenRepository;
    @Autowired private MovimientoInventarioRepository movimientoInventarioRepository;
    @Autowired private CheckoutService checkoutService;
    @Autowired private PagoRepository pagoRepository;

    /**
     * Comprueba que las pruebas de integración
     * utilicen H2 y nunca la base MySQL del proyecto.
     */
    @Test
    void datasource_debeUtilizarH2() throws SQLException {

        try (Connection conexion = dataSource.getConnection()) {

            String url = conexion.getMetaData().getURL();
            String producto = conexion.getMetaData()
                    .getDatabaseProductName();

            assertEquals(
                    "H2",
                    producto,
                    "Las pruebas de integración deben utilizar H2."
            );

            assertTrue(
                    url.startsWith("jdbc:h2:mem:"),
                    "La base de pruebas debe estar en memoria."
            );
        }
    }

    @Test
    void confirmarCompra_inventarioInsuficiente_revierteTransaccion() {

        // ARRANGE: preparar datos reales en H2.
        TransactionTemplate transaccion =
                new TransactionTemplate(transactionManager);

        Long[] ids = transaccion.execute(estado -> {

            Usuario usuario = usuarioRepository.save(
                    new Usuario(
                            null,
                            "Cliente Rollback",
                            "rollback@raudskinna.com",
                            "hash",
                            Rol.USER
                    )
            );

            Categoria categoria = categoriaRepository.save(
                    new Categoria("Terror rollback")
            );

            Editorial editorial = editorialRepository.save(
                    new Editorial("Editorial rollback", "Sonora")
            );

            Libro libroA = libroRepository.save(
                    new Libro("Libro A", "RB-ISBN-A",
                            categoria, editorial)
            );

            Libro libroB = libroRepository.save(
                    new Libro("Libro B", "RB-ISBN-B",
                            categoria, editorial)
            );

            Formato tipoFisico = Arrays.stream(Formato.values())
                    .filter(f -> f != Formato.DIGITAL)
                    .findFirst()
                    .orElseThrow();

            FormatoLibro formatoA = formatoLibroRepository.saveAndFlush(
                    new FormatoLibro(
                            libroA, tipoFisico,
                            new BigDecimal("350.00")
                    )
            );

            FormatoLibro formatoB = formatoLibroRepository.saveAndFlush(
                    new FormatoLibro(
                            libroB, tipoFisico,
                            new BigDecimal("200.00")
                    )
            );

            // El primer formato debe procesarse antes.
            assertTrue(formatoA.getId() < formatoB.getId());

            ItemStock stockA = itemStockRepository.save(
                    new ItemStock(formatoA, 5, 1)
            );

            ItemStock stockB = itemStockRepository.save(
                    new ItemStock(formatoB, 1, 1)
            );

            CarritoCompra carrito = new CarritoCompra(usuario);

            carrito.getItems().add(
                    new ItemCarrito(carrito, formatoA, 2)
            );

            carrito.getItems().add(
                    new ItemCarrito(carrito, formatoB, 3)
            );

            carritoCompraRepository.saveAndFlush(carrito);

            Direccion direccion = new Direccion(usuario);
            direccion.setNombreDestinatario("Cliente Rollback");
            direccion.setCalle("Calle Hidalgo");
            direccion.setNumeroExterior("123");
            direccion.setColonia("Centro");
            direccion.setCodigoPostal("85000");
            direccion.setCiudad("Ciudad Obregón");
            direccion.setEstado("Sonora");
            direccion.setPais("México");

            direccion = direccionRepository.save(direccion);

            return new Long[]{
                    usuario.getId(),
                    formatoA.getId(),
                    formatoB.getId(),
                    stockA.getId(),
                    stockB.getId(),
                    direccion.getId()
            };
        });

        assertNotNull(ids);

        Long usuarioId = ids[0];
        Long formatoAId = ids[1];
        Long formatoBId = ids[2];
        Long stockAId = ids[3];
        Long stockBId = ids[4];
        Long direccionId = ids[5];

        // Estado de la BD después de confirmar la preparación.
        long ordenesAntes = ordenCompraRepository.count();
        long detallesAntes = detalleOrdenRepository.count();
        long movimientosAntes = movimientoInventarioRepository.count();
        long pagosAntes = pagoRepository.count();

        // ACT: la segunda presentación tiene stock insuficiente.
        InventarioInsuficienteException excepcion = assertThrows(
                InventarioInsuficienteException.class,
                () -> checkoutService.confirmarCompra(
                        "rollback@raudskinna.com",
                        direccionId,
                        MetodoPago.TRANSFERENCIA
                )
        );

        assertEquals(
                "Existencias insuficientes. Disponibles: 1",
                excepcion.getMessage()
        );

        // ASSERT: comprobar el estado DESPUÉS del rollback.
        assertEquals(ordenesAntes, ordenCompraRepository.count());
        assertEquals(detallesAntes, detalleOrdenRepository.count());
        assertEquals(movimientosAntes,
                movimientoInventarioRepository.count());

        // Ningún descuento debe persistir.
        ItemStock stockA = itemStockRepository.findById(stockAId)
                .orElseThrow();

        ItemStock stockB = itemStockRepository.findById(stockBId)
                .orElseThrow();

        assertEquals(5, stockA.getCantidad());
        assertEquals(1, stockB.getCantidad());

        assertEquals(formatoAId, stockA.getFormatoLibro().getId());
        assertEquals(formatoBId, stockB.getFormatoLibro().getId());

        // Los artículos del carrito deben conservarse.
        List<ItemCarrito> items = transaccion.execute(estado ->
                List.copyOf(
                        carritoCompraRepository.findByUsuarioId(usuarioId)
                                .orElseThrow()
                                .getItems()
                )
        );

        assertNotNull(items);
        assertEquals(2, items.size());
        assertEquals(
                pagosAntes,
                pagoRepository.count(),
                "Una compra fallida no debe dejar pagos registrados."
        );
    }

    @Test
    void confirmarCompra_exitosa_registraPagoPendiente() {

        TransactionTemplate transaccion =
                new TransactionTemplate(transactionManager);

        // ARRANGE: preparar una compra física.
        Long[] ids = transaccion.execute(estado -> {

            Usuario usuario = usuarioRepository.save(
                    new Usuario(
                            null,
                            "Cliente Pago",
                            "pago-integracion@raudskinna.test",
                            "hash",
                            Rol.USER
                    )
            );

            Categoria categoria = categoriaRepository.save(
                    new Categoria("Pagos integración")
            );

            Editorial editorial = editorialRepository.save(
                    new Editorial("Editorial pagos", "Sonora")
            );

            Libro libro = libroRepository.save(
                    new Libro(
                            "Libro de prueba pago",
                            "PAGO-ISBN-001",
                            categoria,
                            editorial
                    )
            );

            Formato formatoFisico = Arrays.stream(Formato.values())
                    .filter(f -> f != Formato.DIGITAL)
                    .findFirst()
                    .orElseThrow();

            FormatoLibro formato = formatoLibroRepository.saveAndFlush(
                    new FormatoLibro(
                            libro,
                            formatoFisico,
                            new BigDecimal("350.00")
                    )
            );

            ItemStock stock = itemStockRepository.save(
                    new ItemStock(formato, 2, 1)
            );

            CarritoCompra carrito = new CarritoCompra(usuario);
            carrito.getItems().add(
                    new ItemCarrito(carrito, formato, 1)
            );
            carritoCompraRepository.saveAndFlush(carrito);

            Direccion direccion = new Direccion(usuario);
            direccion.setNombreDestinatario("Cliente Pago");
            direccion.setCalle("Calle Hidalgo");
            direccion.setNumeroExterior("123");
            direccion.setColonia("Centro");
            direccion.setCodigoPostal("85000");
            direccion.setCiudad("Ciudad Obregón");
            direccion.setEstado("Sonora");
            direccion.setPais("México");

            direccion = direccionRepository.save(direccion);

            return new Long[]{
                    stock.getId(),
                    direccion.getId()
            };
        });

        assertNotNull(ids);

        Long stockId = ids[0];
        Long direccionId = ids[1];

        long pagosAntes = pagoRepository.count();

        // ACT: ejecutar el checkout real.
        var respuesta = checkoutService.confirmarCompra(
                "pago-integracion@raudskinna.test",
                direccionId,
                MetodoPago.TRANSFERENCIA
        );

        // ASSERT: consultar el pago persistido.
        assertNotNull(respuesta.ordenId());

        Pago pago = pagoRepository
                .findByOrdenCompraId(respuesta.ordenId())
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("350.00").compareTo(pago.getMonto())
        );

        assertEquals(MetodoPago.TRANSFERENCIA, pago.getMetodo());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        assertNotNull(pago.getFechaRegistro());

        assertEquals(pagosAntes + 1, pagoRepository.count());

        ItemStock stockFinal = itemStockRepository
                .findById(stockId)
                .orElseThrow();

        assertEquals(1, stockFinal.getCantidad());
    }

    @Test
    void confirmarCompra_falloPosteriorAlPago_revierteTodaLaTransaccion() {

        TransactionTemplate transaccion =
                new TransactionTemplate(transactionManager);

        // ARRANGE: preparar una compra física en H2.
        Long[] ids = transaccion.execute(estado -> {

            Usuario usuario = usuarioRepository.save(
                    new Usuario(
                            null,
                            "Cliente Rollback Pago",
                            "rollback-pago@raudskinna.test",
                            "hash",
                            Rol.USER
                    )
            );

            Categoria categoria = categoriaRepository.save(
                    new Categoria("Rollback pago")
            );

            Editorial editorial = editorialRepository.save(
                    new Editorial("Editorial rollback pago", "Sonora")
            );

            Libro libro = libroRepository.save(
                    new Libro(
                            "Libro rollback pago",
                            "RB-PAGO-ISBN-001",
                            categoria,
                            editorial
                    )
            );

            Formato formatoFisico = Arrays.stream(Formato.values())
                    .filter(f -> f != Formato.DIGITAL)
                    .findFirst()
                    .orElseThrow();

            FormatoLibro formato = formatoLibroRepository.saveAndFlush(
                    new FormatoLibro(
                            libro,
                            formatoFisico,
                            new BigDecimal("350.00")
                    )
            );

            ItemStock stock = itemStockRepository.save(
                    new ItemStock(formato, 2, 1)
            );

            CarritoCompra carrito = new CarritoCompra(usuario);
            carrito.getItems().add(
                    new ItemCarrito(carrito, formato, 1)
            );

            carritoCompraRepository.saveAndFlush(carrito);

            Direccion direccion = new Direccion(usuario);
            direccion.setNombreDestinatario("Cliente Rollback Pago");
            direccion.setCalle("Calle Hidalgo");
            direccion.setNumeroExterior("123");
            direccion.setColonia("Centro");
            direccion.setCodigoPostal("85000");
            direccion.setCiudad("Ciudad Obregón");
            direccion.setEstado("Sonora");
            direccion.setPais("México");

            direccion = direccionRepository.save(direccion);

            return new Long[]{
                    usuario.getId(),
                    stock.getId(),
                    direccion.getId()
            };
        });

        assertNotNull(ids);

        Long usuarioId = ids[0];
        Long stockId = ids[1];
        Long direccionId = ids[2];

        long ordenesAntes = ordenCompraRepository.count();
        long detallesAntes = detalleOrdenRepository.count();
        long movimientosAntes = movimientoInventarioRepository.count();
        long pagosAntes = pagoRepository.count();

        // ACT: ejecutar checkout dentro de una transacción externa.
        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class,
                () -> transaccion.execute(estado -> {

                    CheckoutResponseDTO respuesta =
                            checkoutService.confirmarCompra(
                                    "rollback-pago@raudskinna.test",
                                    direccionId,
                                    MetodoPago.TRANSFERENCIA
                            );

                    // Comprobar que el pago ya existe
                    // dentro de la transacción activa.
                    Pago pago = pagoRepository
                            .findByOrdenCompraId(respuesta.ordenId())
                            .orElseThrow();

                    assertEquals(
                            EstadoPago.PENDIENTE,
                            pago.getEstado()
                    );

                    // Simular una falla posterior al registro del pago.
                    throw new IllegalStateException(
                            "Fallo simulado después de registrar el pago."
                    );
                })
        );

        assertEquals(
                "Fallo simulado después de registrar el pago.",
                excepcion.getMessage()
        );

        // ASSERT: comprobar el estado tras el rollback.
        assertEquals(ordenesAntes, ordenCompraRepository.count());
        assertEquals(detallesAntes, detalleOrdenRepository.count());
        assertEquals(movimientosAntes,
                movimientoInventarioRepository.count());
        assertEquals(pagosAntes, pagoRepository.count());

        // El inventario debe regresar a su cantidad original.
        ItemStock stockFinal = itemStockRepository.findById(stockId)
                .orElseThrow();

        assertEquals(2, stockFinal.getCantidad());

        // El carrito debe conservar su artículo.
        Integer cantidadItems = transaccion.execute(estado ->
                carritoCompraRepository.findByUsuarioId(usuarioId)
                        .orElseThrow()
                        .getItems()
                        .size()
        );

        assertEquals(1, cantidadItems);
    }

}
