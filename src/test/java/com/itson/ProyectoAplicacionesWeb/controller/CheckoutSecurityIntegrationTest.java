
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.CheckoutResponseDTO;
import com.itson.ProyectoAplicacionesWeb.model.MetodoPago;
import com.itson.ProyectoAplicacionesWeb.service.CheckoutService;

import com.itson.ProyectoAplicacionesWeb.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica la protección JWT del checkout utilizando
 * la cadena real de filtros de Spring Security.
 *
 * Utiliza el perfil integrationtest con H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class CheckoutSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CheckoutService checkoutService;

    /**
     * Una solicitud sin JWT debe ser rechazada
     * antes de llegar al controlador.
     */
    @Test
    void confirmarCompra_sinJwt_retorna401()
            throws Exception {

        mockMvc.perform(
                        post("/api/ordenes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"direccionId\":25}")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value(
                        "Se requiere un token JWT válido "
                                + "para acceder a este recurso."
                ))
                .andExpect(jsonPath("$.ruta").value("/api/ordenes"));

        // La petición debe ser bloqueada por Spring Security.
        verifyNoInteractions(checkoutService);
    }

    /**
     * Un JWT inválido debe ser rechazado por Spring Security
     * antes de ejecutar el servicio de checkout.
     */
    @Test
    void confirmarCompra_conJwtInvalido_retorna401()
            throws Exception {

        mockMvc.perform(
                        post("/api/ordenes")
                                .header("Authorization",
                                        "Bearer token.invalido.falso")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"direccionId\":25}")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.ruta").value("/api/ordenes"));

        verifyNoInteractions(checkoutService);
    }


    /**
     * Un JWT válido debe permitir el acceso al checkout.
     *
     * La identidad utilizada debe provenir del subject
     * del token validado, no del contenido de la solicitud.
     */
    @Test
    void confirmarCompra_conJwtValido_utilizaIdentidadAutenticada()
            throws Exception {

        String correoAutenticado = "cliente@raudskinna.test";

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        correoAutenticado,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        String token = jwtService.generarToken(authentication);

        CheckoutResponseDTO respuestaSimulada = new CheckoutResponseDTO(
                99L,
                LocalDateTime.of(2026, 11, 20, 10, 0),
                "PENDIENTE",
                new BigDecimal("499.00")
        );

        when(checkoutService.confirmarCompra(
                eq(correoAutenticado),
                eq(25L),
                eq(MetodoPago.TRANSFERENCIA)
        )).thenReturn(respuestaSimulada);

        mockMvc.perform(
                        post("/api/ordenes")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                "direccionId": 25,
                                "metodoPago": "TRANSFERENCIA"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordenId").value(99));

        verify(checkoutService).confirmarCompra(
                correoAutenticado,
                25L,
                MetodoPago.TRANSFERENCIA
        );
    }


}
