
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.DireccionRequestDTO;
import com.itson.ProyectoAplicacionesWeb.model.Direccion;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.DireccionRepository;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de DireccionService.
 *
 * Los repositorios se simulan mediante Mockito,
 * sin modificar registros reales de MySQL.
 */
@ExtendWith(MockitoExtension.class)
class DireccionServiceTest {

    @Mock
    private DireccionRepository direccionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private DireccionService direccionService;

    @BeforeEach
    void configurar() {
        direccionService = new DireccionService(
                direccionRepository,
                usuarioRepository
        );
    }

    /**
     * Datos válidos para las pruebas.
     */
    private DireccionRequestDTO datosValidos() {
        return new DireccionRequestDTO(
                "H. P. Lovecraft",
                "Calle Hidalgo",
                "123",
                null,
                "Centro",
                "85000",
                "Ciudad Obregón",
                "Sonora",
                "México",
                null
        );
    }

    @Test
    void registrar_direccionValida_guardaCorrectamente() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        direccionService.registrar(
                "cliente@raudskinna.com",
                datosValidos()
        );

        ArgumentCaptor<Direccion> captor =
                ArgumentCaptor.forClass(Direccion.class);

        verify(direccionRepository).save(captor.capture());

        Direccion direccion = captor.getValue();

        assertSame(usuario, direccion.getUsuario());
        assertEquals("H. P. Lovecraft",
                direccion.getNombreDestinatario());
        assertEquals("85000", direccion.getCodigoPostal());
        assertNull(direccion.getNumeroInterior());
    }

    @Test
    void registrar_codigoPostalInvalido_rechazaRegistro() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        DireccionRequestDTO datos = new DireccionRequestDTO(
                "H. P. Lovecraft",
                "Calle Hidalgo",
                "123",
                null,
                "Centro",
                "8500",
                "Ciudad Obregón",
                "Sonora",
                "México",
                null
        );

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> direccionService.registrar(
                        "cliente@raudskinna.com",
                        datos
                )
        );

        assertEquals(
                "El código postal debe contener 5 dígitos.",
                excepcion.getMessage()
        );

        verify(direccionRepository, never())
                .save(any(Direccion.class));
    }

    @Test
    void buscarPropia_direccionAjena_rechazaConsulta() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        when(direccionRepository.findByIdAndUsuario_Id(10L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> direccionService.buscarPropia(
                        "cliente@raudskinna.com",
                        10L
                )
        );

        assertEquals(
                "La dirección no existe o no pertenece al cliente.",
                excepcion.getMessage()
        );
    }

    /**
     * Comprueba que un cliente no pueda modificar
     * una dirección que no le pertenece.
     */
    @Test
    void actualizar_direccionAjena_rechazaModificacion() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        // No existe una dirección con ese ID perteneciente al usuario.
        when(direccionRepository.findByIdAndUsuario_Id(10L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> direccionService.actualizar(
                        "cliente@raudskinna.com",
                        10L,
                        datosValidos()
                )
        );

        assertEquals(
                "La dirección no existe o no pertenece al cliente.",
                excepcion.getMessage()
        );

        // No debe persistir modificaciones.
        verify(direccionRepository, never())
                .save(any(Direccion.class));
    }

    /**
     * Comprueba que un cliente no pueda eliminar
     * una dirección perteneciente a otra cuenta.
     */
    @Test
    void eliminar_direccionAjena_rechazaEliminacion() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        when(direccionRepository.findByIdAndUsuario_Id(10L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> direccionService.eliminar(
                        "cliente@raudskinna.com",
                        10L
                )
        );

        assertEquals(
                "La dirección no existe o no pertenece al cliente.",
                excepcion.getMessage()
        );

        // No debe realizar ninguna eliminación.
        verify(direccionRepository, never())
                .delete(any(Direccion.class));
    }

    /**
     * Comprueba que el cliente pueda actualizar
     * una dirección registrada a su nombre.
     */
    @Test
    void actualizar_direccionPropia_guardaModificaciones() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Direccion direccion = new Direccion(usuario);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        when(direccionRepository.findByIdAndUsuario_Id(10L, 1L))
                .thenReturn(Optional.of(direccion));

        when(direccionRepository.save(direccion))
                .thenReturn(direccion);

        Direccion resultado = direccionService.actualizar(
                "cliente@raudskinna.com",
                10L,
                datosValidos()
        );

        assertSame(direccion, resultado);
        assertEquals("Calle Hidalgo", resultado.getCalle());
        assertEquals("85000", resultado.getCodigoPostal());

        verify(direccionRepository).save(direccion);
    }

    /**
     * Comprueba que el cliente pueda eliminar
     * una dirección que le pertenece.
     */
    @Test
    void eliminar_direccionPropia_eliminaCorrectamente() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Direccion direccion = new Direccion(usuario);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        when(direccionRepository.findByIdAndUsuario_Id(10L, 1L))
                .thenReturn(Optional.of(direccion));

        direccionService.eliminar(
                "cliente@raudskinna.com",
                10L
        );

        verify(direccionRepository).delete(direccion);
    }

    /**
     * Comprueba que los campos opcionales vacíos
     * se almacenen como null.
     */
    @Test
    void registrar_camposOpcionalesVacios_seGuardanComoNull() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        DireccionRequestDTO datos = new DireccionRequestDTO(
                "H. P. Lovecraft",
                "Calle Hidalgo",
                "123",
                "   ",
                "Centro",
                "85000",
                "Ciudad Obregón",
                "Sonora",
                "México",
                "   "
        );

        direccionService.registrar(
                "cliente@raudskinna.com",
                datos
        );

        ArgumentCaptor<Direccion> captor =
                ArgumentCaptor.forClass(Direccion.class);

        verify(direccionRepository).save(captor.capture());

        Direccion direccion = captor.getValue();

        assertNull(direccion.getNumeroInterior());
        assertNull(direccion.getReferencias());
    }

    @Test
    void registrar_nombreDestinatarioVacio_rechazaRegistro() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        DireccionRequestDTO datos = new DireccionRequestDTO(
                "   ",
                "Calle Hidalgo",
                "123",
                null,
                "Centro",
                "85000",
                "Ciudad Obregón",
                "Sonora",
                "México",
                null
        );

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> direccionService.registrar(
                        "cliente@raudskinna.com",
                        datos
                )
        );

        assertEquals(
                "El nombre del destinatario es obligatorio.",
                excepcion.getMessage()
        );

        verify(direccionRepository, never())
                .save(any(Direccion.class));
    }

    /**
     * Comprueba que el servicio rechace una calle
     * que supere los 150 caracteres permitidos.
     */
    @Test
    void registrar_calleMayorA150Caracteres_rechazaRegistro() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        DireccionRequestDTO datos = new DireccionRequestDTO(
                "H. P. Lovecraft",
                "A".repeat(151),
                "123",
                null,
                "Centro",
                "85000",
                "Ciudad Obregón",
                "Sonora",
                "México",
                null
        );

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> direccionService.registrar(
                        "cliente@raudskinna.com",
                        datos
                )
        );

        assertEquals(
                "La calle supera los 150 caracteres.",
                excepcion.getMessage()
        );

        verify(direccionRepository, never())
                .save(any(Direccion.class));
    }

    /**
     * Comprueba que el código postal contenga
     * exclusivamente cinco dígitos numéricos.
     */
    @Test
    void registrar_codigoPostalConLetras_rechazaRegistro() {

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findByCorreo("cliente@raudskinna.com"))
                .thenReturn(Optional.of(usuario));

        DireccionRequestDTO datos = new DireccionRequestDTO(
                "H. P. Lovecraft",
                "Calle Hidalgo",
                "123",
                null,
                "Centro",
                "85A00",
                "Ciudad Obregón",
                "Sonora",
                "México",
                null
        );

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> direccionService.registrar(
                        "cliente@raudskinna.com",
                        datos
                )
        );

        assertEquals(
                "El código postal debe contener 5 dígitos.",
                excepcion.getMessage()
        );

        verify(direccionRepository, never())
                .save(any(Direccion.class));
    }
}
