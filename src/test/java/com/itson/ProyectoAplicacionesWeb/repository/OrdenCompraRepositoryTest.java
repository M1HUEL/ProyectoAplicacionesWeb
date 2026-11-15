
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.OrdenCompra;
import com.itson.ProyectoAplicacionesWeb.model.Rol;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
