
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.FormatoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica el acceso público al catálogo comercial
 * utilizando la configuración real de Spring Security.
 *
 * Utiliza MockMvc y el perfil integrationtest con H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class LibroPublicoSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibroService libroService;

    /**
     * Un visitante sin JWT debe poder consultar las
     * presentaciones comerciales de un libro activo.
     *
     * La respuesta debe contener formato, precio
     * y disponibilidad.
     */
    @Test
    void consultarFormatos_sinJwt_retorna200()
            throws Exception {

        // ARRANGE: preparar las presentaciones comerciales.
        List<FormatoPublicoDTO> formatos = List.of(
                new FormatoPublicoDTO(
                        2L,
                        Formato.PASTA_DURA,
                        new BigDecimal("350.00"),
                        true
                ),
                new FormatoPublicoDTO(
                        3L,
                        Formato.DIGITAL,
                        new BigDecimal("149.00"),
                        true
                )
        );

        when(libroService.listarFormatosPublicos(1L))
                .thenReturn(formatos);

        // ACT y ASSERT: consultar sin proporcionar JWT.
        mockMvc.perform(
                        get("/api/libros/1/formatos")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].formato").value("PASTA_DURA"))
                .andExpect(jsonPath("$[0].precio").value(350.00))
                .andExpect(jsonPath("$[0].disponible").value(true))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1].formato").value("DIGITAL"))
                .andExpect(jsonPath("$[1].precio").value(149.00))
                .andExpect(jsonPath("$[1].disponible").value(true));

        // Verificar que la solicitud llegó al servicio.
        verify(libroService).listarFormatosPublicos(1L);
    }

    /**
     * Los formatos de libros inactivos o inexistentes
     * no deben exponerse públicamente.
     */
    @Test
    void consultarFormatos_libroInactivo_retorna404()
            throws Exception {

        // ARRANGE: el servicio rechaza el libro solicitado.
        when(libroService.listarFormatosPublicos(99L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El libro solicitado no está disponible."
                ));

        // ACT y ASSERT: verificar la respuesta HTTP.
        mockMvc.perform(
                        get("/api/libros/99/formatos")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());

        verify(libroService).listarFormatosPublicos(99L);
    }

    /**
     * Abrir públicamente las consultas del catálogo
     * no debe permitir el acceso anónimo al checkout.
     */
    @Test
    void checkout_sinJwt_permaneceProtegido()
            throws Exception {

        mockMvc.perform(
                        get("/api/checkout")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());

        // No necesitamos simular CheckoutService:
        // la solicitud debe ser rechazada por Spring Security.
    }
}
