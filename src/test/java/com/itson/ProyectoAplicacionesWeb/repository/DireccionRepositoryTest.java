
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Direccion;
import com.itson.ProyectoAplicacionesWeb.model.Rol;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del repositorio de direcciones.
 *
 * Utiliza H2 en memoria para comprobar las consultas JPA
 * sin modificar los registros reales de MySQL.
 */
@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect="
                + "org.hibernate.dialect.H2Dialect",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class DireccionRepositoryTest {

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Construye una dirección válida para las pruebas.
     */
    private Direccion crearDireccion(Usuario usuario) {

        Direccion direccion = new Direccion(usuario);

        direccion.setNombreDestinatario("H. P. Lovecraft");
        direccion.setCalle("Calle Hidalgo");
        direccion.setNumeroExterior("123");
        direccion.setColonia("Centro");
        direccion.setCodigoPostal("85000");
        direccion.setCiudad("Ciudad Obregón");
        direccion.setEstado("Sonora");
        direccion.setPais("México");

        return direccion;
    }

    /**
     * Comprueba que cada cliente recupere únicamente
     * las direcciones que le pertenecen.
     */
    @Test
    void findByUsuario_Id_soloDevuelveDireccionesPropias() {

        Usuario ana = usuarioRepository.save(
                new Usuario(
                        null, "Ana",
                        "ana@raudskinna.com",
                        "hash", Rol.USER
                )
        );

        Usuario luis = usuarioRepository.save(
                new Usuario(
                        null, "Luis",
                        "luis@raudskinna.com",
                        "hash", Rol.USER
                )
        );

        direccionRepository.save(crearDireccion(ana));
        direccionRepository.save(crearDireccion(ana));
        direccionRepository.save(crearDireccion(luis));

        List<Direccion> direccionesAna =
                direccionRepository.findByUsuario_Id(ana.getId());

        assertEquals(2, direccionesAna.size());

        assertTrue(direccionesAna.stream().allMatch(
                direccion -> direccion.getUsuario()
                        .getId().equals(ana.getId())
        ));

        List<Direccion> direccionesLuis =
                direccionRepository.findByUsuario_Id(luis.getId());

        assertEquals(1, direccionesLuis.size());
    }

    /**
     * Comprueba que una dirección no pueda recuperarse
     * asociándola al identificador de otro cliente.
     */
    @Test
    void findByIdAndUsuario_Id_rechazaPropietarioIncorrecto() {

        Usuario ana = usuarioRepository.save(
                new Usuario(
                        null, "Ana",
                        "ana@raudskinna.com",
                        "hash", Rol.USER
                )
        );

        Usuario luis = usuarioRepository.save(
                new Usuario(
                        null, "Luis",
                        "luis@raudskinna.com",
                        "hash", Rol.USER
                )
        );

        Direccion direccion = direccionRepository.saveAndFlush(
                crearDireccion(ana)
        );

        Optional<Direccion> resultadoPropio =
                direccionRepository.findByIdAndUsuario_Id(
                        direccion.getId(),
                        ana.getId()
                );

        Optional<Direccion> resultadoAjeno =
                direccionRepository.findByIdAndUsuario_Id(
                        direccion.getId(),
                        luis.getId()
                );

        assertTrue(resultadoPropio.isPresent());
        assertTrue(resultadoAjeno.isEmpty());

        assertTrue(direccionRepository.existsByIdAndUsuario_Id(
                direccion.getId(), ana.getId()
        ));

        assertFalse(direccionRepository.existsByIdAndUsuario_Id(
                direccion.getId(), luis.getId()
        ));
    }
}
