
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.DireccionService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica con la configuración real de Spring Security que las
 * direcciones no puedan consultarse ni modificarse sin JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class DireccionSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DireccionService direccionService;

    @Test
    void direcciones_sinJwt_devuelve401EnTodosLosMetodos()
            throws Exception {

        mockMvc.perform(get("/api/direcciones"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));

        mockMvc.perform(get("/api/direcciones/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/direcciones")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        put("/api/direcciones/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/direcciones/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(direccionService);
    }
}
