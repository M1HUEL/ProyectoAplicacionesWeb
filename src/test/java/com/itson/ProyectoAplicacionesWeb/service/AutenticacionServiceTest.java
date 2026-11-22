
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Rol;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del registro público de clientes.
 *
 * Verifican la validación del teléfono incorporada
 * después de la retroalimentación del tercer avance.
 *
 * Los repositorios y el codificador de contraseñas
 * se simulan mediante Mockito.
 */
@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    private static final String CORREO =
            "cliente@raudskinna.test";

    private static final String TELEFONO_VALIDO =
            "6441234567";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AutenticacionService autenticacionService;

    /**
     * Construye un usuario con datos de prueba.
     *
     * Utilizamos el constructor sin argumentos
     * generado por Lombok.
     */
    private Usuario crearUsuario(String telefono) {

        Usuario usuario = new Usuario();

        usuario.setNombre("Cliente de prueba");
        usuario.setCorreo(CORREO);
        usuario.setTelefono(telefono);
        usuario.setContrasena("contrasena-segura");

        return usuario;
    }

    /**
     * Un teléfono válido permite registrar al cliente.
     *
     * También verificamos que el servidor:
     * - Ignore identificadores proporcionados.
     * - Imponga el rol USER.
     * - Codifique la contraseña.
     * - Persista el teléfono.
     */
    @Test
    void registrarConTelefonoValidoGuardaClienteSeguro() {

        Usuario usuario = crearUsuario(TELEFONO_VALIDO);

        usuario.setId(99L);
        usuario.setRol(Rol.ADMIN);

        when(usuarioRepository.existsByCorreo(CORREO))
                .thenReturn(false);

        when(passwordEncoder.encode("contrasena-segura"))
                .thenReturn("hash-bcrypt-simulado");

        autenticacionService.registrar(usuario);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario guardado = captor.getValue();

        assertNull(guardado.getId());
        assertEquals(Rol.USER, guardado.getRol());
        assertEquals(TELEFONO_VALIDO, guardado.getTelefono());
        assertEquals("hash-bcrypt-simulado",
                guardado.getContrasena());
    }

    /**
     * El teléfono es obligatorio para registros nuevos.
     */
    @Test
    void registrarSinTelefonoRechazaSolicitud() {

        Usuario usuario = crearUsuario(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> autenticacionService.registrar(usuario)
        );

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * No deben aceptarse teléfonos con menos
     * de diez dígitos.
     */
    @Test
    void registrarConTelefonoCortoRechazaSolicitud() {

        Usuario usuario = crearUsuario("644123456");

        assertThrows(
                IllegalArgumentException.class,
                () -> autenticacionService.registrar(usuario)
        );

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * El teléfono no debe contener letras.
     */
    @Test
    void registrarConTelefonoAlfanumericoRechazaSolicitud() {

        Usuario usuario = crearUsuario("64412345AB");

        assertThrows(
                IllegalArgumentException.class,
                () -> autenticacionService.registrar(usuario)
        );

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Los espacios exteriores se eliminan
     * antes de guardar el teléfono.
     */
    @Test
    void registrarNormalizaEspaciosDelTelefono() {

        Usuario usuario = crearUsuario(" 6441234567 ");

        when(usuarioRepository.existsByCorreo(CORREO))
                .thenReturn(false);

        when(passwordEncoder.encode(any()))
                .thenReturn("hash-bcrypt-simulado");

        autenticacionService.registrar(usuario);

        assertEquals(
                TELEFONO_VALIDO,
                usuario.getTelefono()
        );

        verify(usuarioRepository).save(usuario);
    }
}
