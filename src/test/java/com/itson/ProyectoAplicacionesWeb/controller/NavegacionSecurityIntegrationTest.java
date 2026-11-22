
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.model.Rol;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración de autenticación web
 * y recuperación de navegación comercial.
 *
 * Utiliza Spring Security, MockMvc y una base
 * de datos H2 para comprobar el proceso real
 * de autenticación mediante formularios.
 *
 * No utiliza credenciales de producción
 * ni establece conexiones con MySQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
@Transactional
class NavegacionSecurityIntegrationTest {

    private static final String CORREO_CLIENTE =
            "navegacion-cliente@raudskinna.test";

    private static final String CORREO_ADMIN =
            "navegacion-admin@raudskinna.test";

    private static final String CONTRASENA =
            "RaudskinnaTest123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registra dos usuarios independientes para
     * comprobar los flujos de autenticación.
     *
     * Las contraseñas se almacenan codificadas
     * mediante el componente real de seguridad.
     */
    @BeforeEach
    void configurar() {

        usuarioRepository.saveAndFlush(
                new Usuario(
                        null,
                        "Cliente de prueba",
                        CORREO_CLIENTE,
                        passwordEncoder.encode(CONTRASENA),
                        Rol.USER
                )
        );

        usuarioRepository.saveAndFlush(
                new Usuario(
                        null,
                        "Administrador de prueba",
                        CORREO_ADMIN,
                        passwordEncoder.encode(CONTRASENA),
                        Rol.ADMIN
                )
        );
    }

    /**
     * Comprueba que el cliente que inicia sesión
     * voluntariamente regresa al inicio.
     */
    @Test
    void iniciarSesion_clienteSinSolicitudPrevia_redirigeAlInicio()
            throws Exception {

        mockMvc.perform(
                        post("/iniciar-sesion")
                                .param("correo", CORREO_CLIENTE)
                                .param("contrasena", CONTRASENA)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(CORREO_CLIENTE))
                .andExpect(redirectedUrl("/"));
    }

    /**
     * Comprueba que un administrador conserva
     * el acceso directo al panel de gestión.
     */
    @Test
    void iniciarSesion_administrador_redirigeAlPanel()
            throws Exception {

        mockMvc.perform(
                        post("/iniciar-sesion")
                                .param("correo", CORREO_ADMIN)
                                .param("contrasena", CONTRASENA)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(CORREO_ADMIN))
                .andExpect(redirectedUrl("/admin"));
    }

    /**
     * Comprueba que la solicitud de acceso al carrito
     * se conserva durante la autenticación.
     *
     * Reproduce el escenario validado manualmente
     * desde una ventana privada del navegador.
     */
    @Test
    void iniciarSesion_desdeCarrito_recuperaSolicitud()
            throws Exception {

        // 1. Solicitar el carrito sin autenticación.
        MockHttpSession sesion = solicitarRutaProtegida(
                "/carrito"
        );

        // 2. Autenticar al cliente utilizando la sesión
        // que contiene la solicitud previamente guardada.
        mockMvc.perform(
                        post("/iniciar-sesion")
                                .session(sesion)
                                .param("correo", CORREO_CLIENTE)
                                .param("contrasena", CONTRASENA)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(CORREO_CLIENTE))
                .andExpect(header().string(
                        "Location",
                        endsWith("/carrito")
                ));
    }

    /**
     * Comprueba que el cliente recupera la navegación
     * hacia el checkout después de autenticarse.
     */
    @Test
    void iniciarSesion_desdeCheckout_recuperaSolicitud()
            throws Exception {

        MockHttpSession sesion = solicitarRutaProtegida(
                "/checkout"
        );

        mockMvc.perform(
                        post("/iniciar-sesion")
                                .session(sesion)
                                .param("correo", CORREO_CLIENTE)
                                .param("contrasena", CONTRASENA)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(CORREO_CLIENTE))
                .andExpect(header().string(
                        "Location",
                        endsWith("/checkout")
                ));
    }

    /**
     * Comprueba que un visitante puede regresar
     * al detalle del libro que estaba consultando
     * antes de iniciar sesión.
     *
     * La ruta intermedia es exclusivamente GET
     * y no modifica el contenido del carrito.
     */
    @Test
    void iniciarSesion_desdeDetalleLibro_recuperaNavegacion()
            throws Exception {

        // 1. Solicitar la ruta intermedia protegida.
        MockHttpSession sesion = solicitarRutaProtegida(
                "/carrito/continuar/1"
        );

        // 2. Autenticar al cliente.
        MvcResult resultadoLogin = mockMvc.perform(
                        post("/iniciar-sesion")
                                .session(sesion)
                                .param("correo", CORREO_CLIENTE)
                                .param("contrasena", CONTRASENA)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(CORREO_CLIENTE))
                .andExpect(header().string(
                        "Location",
                        endsWith("/carrito/continuar/1")
                ))
                .andReturn();

        // 3. Recuperar la sesión después del login.
        MockHttpSession sesionAutenticada =
                (MockHttpSession) resultadoLogin
                        .getRequest()
                        .getSession(false);

        assertNotNull(sesionAutenticada);

        // 4. Continuar la navegación utilizando
        // la sesión realmente autenticada.
        mockMvc.perform(
                        get("/carrito/continuar/1")
                                .session(sesionAutenticada)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/detalle-libro/1"));
    }

    /**
     * Comprueba que una solicitud POST interrumpida
     * por falta de autenticación no se almacena
     * para ejecutarla posteriormente.
     */
    @Test
    void iniciarSesion_despuesDePostNoAutenticado_noRecuperaPost()
            throws Exception {

        MockHttpSession sesion = new MockHttpSession();

        // 1. Intentar agregar un artículo sin autenticación.
        mockMvc.perform(
                        post("/carrito/agregar")
                                .session(sesion)
                                .param("formatoLibroId", "1")
                                .param("cantidad", "1")
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iniciar-sesion"))
                .andExpect(unauthenticated());

        // 2. Autenticar al cliente.
        // La solicitud POST no debe utilizarse
        // como destino posterior al login.
        mockMvc.perform(
                        post("/iniciar-sesion")
                                .session(sesion)
                                .param("correo", CORREO_CLIENTE)
                                .param("contrasena", CONTRASENA)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(CORREO_CLIENTE))
                .andExpect(redirectedUrl("/"));
    }

    /**
     * Comprueba que un intento de autenticación
     * con credenciales incorrectas es rechazado.
     */
    @Test
    void iniciarSesion_contrasenaIncorrecta_rechazaAutenticacion()
            throws Exception {

        mockMvc.perform(
                        post("/iniciar-sesion")
                                .param("correo", CORREO_CLIENTE)
                                .param("contrasena", "incorrecta")
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iniciar-sesion?error"))
                .andExpect(unauthenticated());
    }

    /**
     * Comprueba que el formulario de autenticación
     * permanece protegido contra solicitudes CSRF.
     */
    @Test
    void iniciarSesion_sinCsrf_retorna403()
            throws Exception {

        mockMvc.perform(
                        post("/iniciar-sesion")
                                .param("correo", CORREO_CLIENTE)
                                .param("contrasena", CONTRASENA)
                )
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
    }

    /**
     * Solicita un recurso comercial protegido
     * sin una sesión previamente autenticada.
     *
     * Recupera la sesión generada por Spring Security
     * para utilizarla posteriormente durante el login.
     *
     * @param ruta recurso protegido solicitado.
     * @return sesión que conserva la solicitud.
     */
    private MockHttpSession solicitarRutaProtegida(
            String ruta) throws Exception {

        MvcResult resultado = mockMvc.perform(get(ruta))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iniciar-sesion"))
                .andExpect(unauthenticated())
                .andReturn();

        MockHttpSession sesion =
                (MockHttpSession) resultado
                        .getRequest()
                        .getSession(false);

        assertNotNull(
                sesion,
                "Spring Security debe conservar la solicitud."
        );

        return sesion;
    }
}
