
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.exception.InventarioInsuficienteException;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integrationtest")
class CheckoutConcurrencyIntegrationTest {

    private static final String CORREO_A =
            "concurrencia-a@raudskinna.test";

    private static final String CORREO_B =
            "concurrencia-b@raudskinna.test";

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
    @Autowired private MovimientoInventarioRepository movimientoInventarioRepository;
    @Autowired private CheckoutService checkoutService;

    private record Datos(
            Long stockId,
            Long direccionAId,
            Long direccionBId
    ) {}

    @Test
    void dosClientes_compitenPorUltimaUnidad_soloUnaCompraExitosa()
            throws Exception {

        // ARRANGE: preparar todos los datos y confirmar la transacción.
        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        Datos datos = tx.execute(estado -> {

            Usuario clienteA = usuarioRepository.save(
                    new Usuario(null, "Cliente A", CORREO_A, "hash", Rol.USER)
            );

            Usuario clienteB = usuarioRepository.save(
                    new Usuario(null, "Cliente B", CORREO_B, "hash", Rol.USER)
            );

            Categoria categoria = categoriaRepository.save(
                    new Categoria("Concurrencia")
            );

            Editorial editorial = editorialRepository.save(
                    new Editorial("Editorial Concurrencia", "Sonora")
            );

            Libro libro = libroRepository.save(
                    new Libro(
                            "Último ejemplar",
                            "CONC-ISBN-001",
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

            // Solamente existe una unidad disponible.
            ItemStock stock = itemStockRepository.save(
                    new ItemStock(formato, 1, 1)
            );

            CarritoCompra carritoA = new CarritoCompra(clienteA);
            carritoA.getItems().add(
                    new ItemCarrito(carritoA, formato, 1)
            );
            carritoCompraRepository.save(carritoA);

            CarritoCompra carritoB = new CarritoCompra(clienteB);
            carritoB.getItems().add(
                    new ItemCarrito(carritoB, formato, 1)
            );
            carritoCompraRepository.save(carritoB);

            Direccion direccionA = crearDireccion(clienteA);
            Direccion direccionB = crearDireccion(clienteB);

            direccionA = direccionRepository.save(direccionA);
            direccionB = direccionRepository.save(direccionB);

            return new Datos(
                    stock.getId(),
                    direccionA.getId(),
                    direccionB.getId()
            );
        });

        assertNotNull(datos);

        long ordenesAntes = ordenCompraRepository.count();
        long movimientosAntes = movimientoInventarioRepository.count();

        // ACT: preparar dos hilos independientes.
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);

        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);

        try {
            Future<Boolean> compraA = ejecutor.submit(() ->
                    intentarCompra(
                            CORREO_A,
                            datos.direccionAId(),
                            preparados,
                            inicio
                    )
            );

            Future<Boolean> compraB = ejecutor.submit(() ->
                    intentarCompra(
                            CORREO_B,
                            datos.direccionBId(),
                            preparados,
                            inicio
                    )
            );

            // Ambos hilos deben estar preparados.
            assertTrue(preparados.await(10, TimeUnit.SECONDS));

            // Liberar ambos para iniciar su checkout.
            inicio.countDown();

            boolean resultadoA = compraA.get(20, TimeUnit.SECONDS);
            boolean resultadoB = compraB.get(20, TimeUnit.SECONDS);

            // ASSERT: exactamente una compra debe tener éxito.
            int comprasExitosas =
                    (resultadoA ? 1 : 0) + (resultadoB ? 1 : 0);

            assertEquals(1, comprasExitosas);

        } finally {
            inicio.countDown();
            ejecutor.shutdownNow();
        }

        // Verificar datos persistidos después de ambas transacciones.
        ItemStock stockFinal = itemStockRepository
                .findById(datos.stockId())
                .orElseThrow();

        assertEquals(0, stockFinal.getCantidad());

        assertEquals(
                ordenesAntes + 1,
                ordenCompraRepository.count()
        );

        assertEquals(
                movimientosAntes + 1,
                movimientoInventarioRepository.count()
        );
    }

    private boolean intentarCompra(
            String correo,
            Long direccionId,
            CountDownLatch preparados,
            CountDownLatch inicio
    ) throws InterruptedException {

        preparados.countDown();
        inicio.await();

        try {
            checkoutService.confirmarCompra(correo, direccionId);
            return true;

        } catch (InventarioInsuficienteException ex) {
            return false;
        }
    }

    private Direccion crearDireccion(Usuario usuario) {

        Direccion direccion = new Direccion(usuario);

        direccion.setNombreDestinatario(usuario.getNombre());
        direccion.setCalle("Calle Hidalgo");
        direccion.setNumeroExterior("123");
        direccion.setColonia("Centro");
        direccion.setCodigoPostal("85000");
        direccion.setCiudad("Ciudad Obregón");
        direccion.setEstado("Sonora");
        direccion.setPais("México");

        return direccion;
    }
}
