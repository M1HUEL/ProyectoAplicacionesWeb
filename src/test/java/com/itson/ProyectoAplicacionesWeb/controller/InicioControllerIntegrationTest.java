
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LibroCatalogoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración MVC de la página principal de
 * Librería Raudskinna.
 *
 * Verifican el acceso público y el renderizado de Thymeleaf
 * utilizando el contexto real de Spring Boot.
 *
 * Se simula LibroService para controlar los datos del catálogo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class InicioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Simular la respuesta del servicio del catálogo público.
     */
    @MockitoBean
    private LibroService libroService;

    /**
     * Comprueba que un visitante sin sesión pueda acceder
     * al inicio y visualizar los libros con sus precios.
     */
    @Test
    void mostrarInicio_conLibros_renderizaCatalogoPublico()
            throws Exception {

        // ARRANGE: preparar un libro público de prueba.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                1L,
                "Drácula",
                "Novela de terror",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("Bram Stoker")
        );

        // Incorporar el resumen comercial calculado por el servicio.
        LibroCatalogoDTO tarjeta = new LibroCatalogoDTO(
                libro,
                new BigDecimal("249.00"),
                true
        );

        when(libroService.listarCatalogoPublico())
                .thenReturn(List.of(tarjeta));

        // ACT y ASSERT: solicitar la página sin autenticación.
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(
                        containsString("Drácula")
                ))
                .andExpect(content().string(
                        containsString("Bram Stoker")
                ))
                .andExpect(content().string(
                        containsString("Terror")
                ))
                .andExpect(content().string(
                        containsString("Desde")
                ))
                .andExpect(content().string(
                        containsString("249.00")
                ));

        // Verificar que el controlador consultó el servicio comercial.
        verify(libroService).listarCatalogoPublico();
    }

    /**
     * Comprueba que la página principal informe cuando
     * todavía no existen libros activos en el catálogo.
     */
    @Test
    void mostrarInicio_sinLibros_muestraMensajeCatalogoVacio()
            throws Exception {

        // ARRANGE: simular un catálogo comercial vacío.
        when(libroService.listarCatalogoPublico())
                .thenReturn(List.of());

        // ACT y ASSERT: verificar el mensaje de catálogo vacío.
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(
                        containsString(
                                "Todavía no hay libros disponibles en el catálogo."
                        )
                ))
                .andExpect(content().string(
                        not(containsString("<h3>Drácula</h3>"))
                ));

        verify(libroService).listarCatalogoPublico();
    }

}
