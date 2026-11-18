
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import com.itson.ProyectoAplicacionesWeb.model.Direccion;
import com.itson.ProyectoAplicacionesWeb.model.DireccionEnvio;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba la consulta que alimenta el historial de compras
 * de la API contra una base de datos en memoria.
 */
@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect="
                + "org.hibernate.dialect.H2Dialect",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class OrdenCompraRepositoryTest {

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Comprueba que la consulta solo devuelve las órdenes
     * del correo solicitado y excluye las de otros clientes.
     */
    @Test
    void listarPorCorreoUsuario_soloDevuelveLasOrdenesDelClienteIndicado() {

        Usuario ana = usuarioRepository.save(
                new Usuario(
                        null,
                        "Ana",
                        "ana@raudskinna.com",
                        "hash",
                        Rol.USER
                )
        );

        Usuario luis = usuarioRepository.save(
                new Usuario(
                        null,
                        "Luis",
                        "luis@raudskinna.com",
                        "hash",
                        Rol.USER
                )
        );

        ordenCompraRepository.save(
                new OrdenCompra(ana, new BigDecimal("100.00"))
        );
        ordenCompraRepository.save(
                new OrdenCompra(ana, new BigDecimal("200.00"))
        );
        ordenCompraRepository.save(
                new OrdenCompra(luis, new BigDecimal("300.00"))
        );

        List<OrdenCompra> ordenesAna =
                ordenCompraRepository.listarPorCorreoUsuario(
                        "ana@raudskinna.com"
                );

        assertEquals(2, ordenesAna.size());

        assertTrue(ordenesAna.stream().allMatch(
                orden -> orden.getUsuario().getId().equals(ana.getId())
        ));

        assertTrue(ordenesAna.stream().noneMatch(
                orden -> orden.getUsuario().getId().equals(luis.getId())
        ));

        List<OrdenCompra> ordenesLuis =
                ordenCompraRepository.listarPorCorreoUsuario(
                        "luis@raudskinna.com"
                );

        assertEquals(1, ordenesLuis.size());

        assertEquals(
                luis.getId(),
                ordenesLuis.get(0).getUsuario().getId()
        );
    }

    /**
     * Comprueba que un correo sin compras no recibe órdenes
     * pertenecientes a otros clientes.
     */
    @Test
    void listarPorCorreoUsuario_correoSinOrdenes_retornaListaVacia() {

        Usuario luis = usuarioRepository.save(
                new Usuario(
                        null,
                        "Luis",
                        "luis@raudskinna.com",
                        "hash",
                        Rol.USER
                )
        );

        ordenCompraRepository.save(
                new OrdenCompra(luis, new BigDecimal("300.00"))
        );

        List<OrdenCompra> resultado =
                ordenCompraRepository.listarPorCorreoUsuario(
                        "otro@raudskinna.com"
                );

        assertTrue(resultado.isEmpty());
    }

    /**
     * Comprueba que una orden conserve los datos históricos
     * de entrega aunque el cliente modifique posteriormente
     * su dirección guardada.
     *
     * Utiliza JPA y H2 en memoria.
     */
    @Test
    void guardarOrden_conDireccionHistorica_conservaDatosOriginales() {

        // 1. Registrar un usuario de prueba.
        Usuario usuario = usuarioRepository.save(
                new Usuario(
                        null,
                        "H. P. Lovecraft",
                        "lovecraft@raudskinna.com",
                        "hash",
                        Rol.USER
                )
        );

        // 2. Registrar una dirección del cliente.
        Direccion direccion = new Direccion(usuario);

        direccion.setNombreDestinatario("H. P. Lovecraft");
        direccion.setCalle("Calle Hidalgo");
        direccion.setNumeroExterior("123");
        direccion.setColonia("Centro");
        direccion.setCodigoPostal("85000");
        direccion.setCiudad("Ciudad Obregón");
        direccion.setEstado("Sonora");
        direccion.setPais("México");

        direccion = direccionRepository.saveAndFlush(direccion);

        // 3. Crear una copia independiente de sus datos.
        DireccionEnvio copiaHistorica = new DireccionEnvio(
                direccion.getNombreDestinatario(),
                direccion.getCalle(),
                direccion.getNumeroExterior(),
                direccion.getNumeroInterior(),
                direccion.getColonia(),
                direccion.getCodigoPostal(),
                direccion.getCiudad(),
                direccion.getEstado(),
                direccion.getPais(),
                direccion.getReferencias()
        );

        // 4. Registrar la orden con la dirección histórica.
        OrdenCompra orden = new OrdenCompra(
                usuario,
                new BigDecimal("350.00")
        );

        orden.setDireccionEnvio(copiaHistorica);

        OrdenCompra ordenGuardada =
                ordenCompraRepository.saveAndFlush(orden);

        Long ordenId = ordenGuardada.getId();

        // 5. El cliente modifica su dirección guardada.
        direccion.setCalle("Calle Morelos");
        direccion.setNumeroExterior("456");

        direccionRepository.saveAndFlush(direccion);

        // 6. Limpiar el contexto para forzar nuevas lecturas.
        entityManager.clear();

        // 7. Recuperar la orden desde la base de datos.
        OrdenCompra ordenRecuperada =
                ordenCompraRepository.findById(ordenId)
                        .orElseThrow();

        DireccionEnvio envioRecuperado =
                ordenRecuperada.getDireccionEnvio();

        // 8. Comprobar que existe la copia histórica.
        assertNotNull(envioRecuperado);

        // 9. Comprobar que conserva los valores originales.
        assertEquals(
                "Calle Hidalgo",
                envioRecuperado.getCalle()
        );

        assertEquals(
                "123",
                envioRecuperado.getNumeroExterior()
        );

        assertEquals(
                "85000",
                envioRecuperado.getCodigoPostal()
        );

        // 10. Verificar que la dirección del cliente sí cambió.
        Direccion direccionActualizada =
                direccionRepository.findById(direccion.getId())
                        .orElseThrow();

        assertEquals(
                "Calle Morelos",
                direccionActualizada.getCalle()
        );

        assertEquals(
                "456",
                direccionActualizada.getNumeroExterior()
        );
    }

}
