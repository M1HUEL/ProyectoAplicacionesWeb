
package com.itson.ProyectoAplicacionesWeb.service;

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

        // ACT: la segunda presentación tiene stock insuficiente.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.confirmarCompra(
                        "rollback@raudskinna.com",
                        direccionId
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
    }

}
