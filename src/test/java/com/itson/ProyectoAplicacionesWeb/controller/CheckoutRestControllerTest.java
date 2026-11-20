
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.CheckoutResponseDTO;
import com.itson.ProyectoAplicacionesWeb.exception.ApiExceptionHandler;
import com.itson.ProyectoAplicacionesWeb.exception.InventarioInsuficienteException;
import com.itson.ProyectoAplicacionesWeb.model.MetodoPago;
import com.itson.ProyectoAplicacionesWeb.service.CheckoutService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas HTTP del controlador REST de checkout.
 *
 * Utiliza MockMvc con el controlador y el manejador
 * global de excepciones.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutRestControllerTest {

    @Mock
    private CheckoutService checkoutService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {

        CheckoutRestController controlador =
                new CheckoutRestController(checkoutService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controlador)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void confirmarCompra_sinDireccion_retorna400()
            throws Exception {

        // ARRANGE
        String correo = "cliente@raudskinna.com";

        Authentication autenticacion =
                mock(Authentication.class);

        when(autenticacion.getName())
                .thenReturn(correo);

        when(checkoutService.confirmarCompra(
                correo,
                null,
                MetodoPago.TRANSFERENCIA
        )).thenThrow(new IllegalArgumentException(
                "Debes seleccionar una dirección de envío."
        ));

        // ACT + ASSERT
        mockMvc.perform(
                        post("/api/checkout")
                                .principal(autenticacion)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "direccionId": null,
                                    "metodoPago": "TRANSFERENCIA"
                                }
                                """)
                        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value(
                        "Debes seleccionar una dirección de envío."
                ))
                .andExpect(jsonPath("$.ruta").value("/api/checkout"));

        // Comprobar la delegación al servicio.
        verify(checkoutService).confirmarCompra(
                correo,
                null,
                MetodoPago.TRANSFERENCIA
        );

        verifyNoMoreInteractions(checkoutService);
    }

    @Test
    void confirmarCompra_conDireccionValida_retorna201()
            throws Exception {

        // ARRANGE: simular un cliente autenticado.
        String correo = "cliente@raudskinna.com";
        Long direccionId = 25L;

        Authentication autenticacion =
                mock(Authentication.class);

        when(autenticacion.getName())
                .thenReturn(correo);

        // Simular la respuesta de una compra confirmada.
        CheckoutResponseDTO respuestaEsperada =
                new CheckoutResponseDTO(
                        100L,
                        LocalDateTime.of(2026, 11, 20, 9, 30),
                        "PENDIENTE",
                        new BigDecimal("549.00")
                );

        when(checkoutService.confirmarCompra(
                correo,
                direccionId,
                MetodoPago.TRANSFERENCIA
        )).thenReturn(respuestaEsperada);

        // ACT + ASSERT: ejecutar la solicitud HTTP.
        mockMvc.perform(
                        post("/api/checkout")
                                .principal(autenticacion)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "direccionId": 25,
                                        "metodoPago": "TRANSFERENCIA"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordenId").value(100))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(549.00))
                .andExpect(jsonPath("$.fecha").exists());

        // Verificar que utilizó la identidad autenticada
        // y el identificador de dirección recibido.
        verify(checkoutService).confirmarCompra(
                correo,
                direccionId,
                MetodoPago.TRANSFERENCIA
        );

        verifyNoMoreInteractions(checkoutService);
    }

    @Test
    void confirmarCompra_inventarioInsuficiente_retorna409()
            throws Exception {

        // ARRANGE
        String correo = "cliente@raudskinna.com";

        Authentication autenticacion = mock(Authentication.class);
        when(autenticacion.getName()).thenReturn(correo);

        when(checkoutService.confirmarCompra(
                correo,
                25L,
                MetodoPago.TRANSFERENCIA
        )).thenThrow(new InventarioInsuficienteException(
                "Existencias insuficientes. Disponibles: 2"
        ));

        // ACT + ASSERT
        mockMvc.perform(
                        post("/api/checkout")
                                .principal(autenticacion)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                "direccionId": 25,
                                "metodoPago": "TRANSFERENCIA"
                                }
                                """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensaje").value(
                        "Existencias insuficientes. Disponibles: 2"
                ))
                .andExpect(jsonPath("$.ruta").value("/api/checkout"));

        verify(checkoutService).confirmarCompra(
                correo,
                25L,
                MetodoPago.TRANSFERENCIA
        );
        verifyNoMoreInteractions(checkoutService);
    }

}
