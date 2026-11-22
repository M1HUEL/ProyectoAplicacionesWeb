
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.DetalleOrdenResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.OrdenCompraResponseDTO;
import com.itson.ProyectoAplicacionesWeb.service.OrdenCompraService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración MVC del historial de compras
 * de Librería Raudskinna.
 *
 * Comprueba la integración entre Spring Security,
 * MisOrdenesController y las plantillas Thymeleaf.
 *
 * Las reglas de negocio y las consultas de persistencia
 * cuentan con sus propias pruebas automatizadas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class MisOrdenesControllerIntegrationTest {

    private static final String CORREO =
            "cliente@raudskinna.test";

    private static final String OTRO_CORREO =
            "otro@raudskinna.test";

    private static final Long ORDEN_ID = 5L;

    private static final BigDecimal PRECIO =
            new BigDecimal("350.00");

    private static final LocalDateTime FECHA =
            LocalDateTime.of(2026, 11, 21, 18, 11);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdenCompraService ordenCompraService;

    /**
     * Comprueba que el historial presenta las órdenes
     * recuperadas para el cliente autenticado.
     *
     * También verifica que se conserve el ordenamiento
     * proporcionado por la capa Service.
     */
    @Test
    void mostrarHistorial_clienteConCompras_muestraOrdenes()
            throws Exception {

        // 1. Preparar dos órdenes de distintas fechas.
        OrdenCompraResponseDTO reciente = crearOrden(
                5L,
                FECHA,
                "PENDIENTE"
        );

        OrdenCompraResponseDTO anterior = crearOrden(
                1L,
                FECHA.minusDays(8),
                "ENTREGADO"
        );

        List<OrdenCompraResponseDTO> ordenes =
                List.of(reciente, anterior);

        // 2. Simular la respuesta del servicio.
        when(ordenCompraService.consultarHistorial(CORREO))
                .thenReturn(ordenes);

        // 3. Consultar el historial autenticado.
        mockMvc.perform(
                        get("/mis-ordenes")
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("mis-ordenes"))
                .andExpect(model().attribute("ordenes", ordenes))
                .andExpect(content().string(
                        containsString("Historial de órdenes")
                ))
                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ))
                .andExpect(content().string(
                        containsString("Pendiente")
                ))
                .andExpect(content().string(
                        containsString("Entregado")
                ))
                .andExpect(content().string(
                        containsString("href=\"/mis-ordenes/5\"")
                ))
                .andExpect(content().string(
                        containsString("href=\"/mis-ordenes/1\"")
                ))
                .andExpect(content().string(
                        not(containsString(
                                "Todavía no tienes órdenes registradas"
                        ))
                ))
                .andExpect(resultado -> {

                    String html = resultado.getResponse()
                            .getContentAsString();

                    // La orden más reciente debe aparecer
                    // antes que la orden anterior.
                    org.junit.jupiter.api.Assertions.assertTrue(
                            html.indexOf("href=\"/mis-ordenes/5\"")
                                    < html.indexOf(
                                    "href=\"/mis-ordenes/1\""
                            )
                    );
                });

        verify(ordenCompraService)
                .consultarHistorial(CORREO);

        verifyNoMoreInteractions(ordenCompraService);
    }

    /**
     * Comprueba que un cliente sin compras recibe
     * un mensaje informativo y acceso al catálogo.
     */
    @Test
    void mostrarHistorial_sinCompras_muestraMensajeInformativo()
            throws Exception {

        when(ordenCompraService.consultarHistorial(CORREO))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/mis-ordenes")
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("mis-ordenes"))
                .andExpect(model().attribute(
                        "ordenes",
                        List.of()
                ))
                .andExpect(content().string(
                        containsString(
                                "Todavía no tienes órdenes registradas"
                        )
                ))
                .andExpect(content().string(
                        containsString("Explorar catálogo")
                ))
                .andExpect(content().string(
                        not(containsString("<table"))
                ));

        verify(ordenCompraService)
                .consultarHistorial(CORREO);
    }

    /**
     * Comprueba que un parámetro manipulado desde
     * el navegador no permite consultar el historial
     * perteneciente a otro usuario.
     */
    @Test
    void mostrarHistorial_correoManipulado_utilizaIdentidadAutenticada()
            throws Exception {

        when(ordenCompraService.consultarHistorial(CORREO))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/mis-ordenes")
                                .with(user(CORREO).roles("USER"))
                                .param("correo", OTRO_CORREO)
                )
                .andExpect(status().isOk())
                .andExpect(view().name("mis-ordenes"));

        // Solo debe consultarse la identidad autenticada.
        verify(ordenCompraService)
                .consultarHistorial(CORREO);

        verify(ordenCompraService, never())
                .consultarHistorial(OTRO_CORREO);

        verifyNoMoreInteractions(ordenCompraService);
    }

    /**
     * Comprueba que el propietario de una orden
     * puede consultar sus detalles históricos.
     */
    @Test
    void mostrarDetalle_ordenPropia_muestraDatos()
            throws Exception {

        OrdenCompraResponseDTO orden = crearOrden(
                ORDEN_ID,
                FECHA,
                "PENDIENTE"
        );

        when(ordenCompraService.consultarOrdenPropia(
                CORREO,
                ORDEN_ID
        )).thenReturn(orden);

        mockMvc.perform(
                        get("/mis-ordenes/{ordenId}", ORDEN_ID)
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("detalle-orden-cliente"))
                .andExpect(model().attribute("orden", orden))
                .andExpect(content().string(
                        containsString("Detalle de tu orden")
                ))
                .andExpect(content().string(
                        containsString("Orden #5")
                ))
                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("Pasta dura")
                ))
                .andExpect(content().string(
                        containsString("Precio unitario: $350.00 MXN")
                ))
                .andExpect(content().string(
                        containsString("Total de la orden")
                ))
                .andExpect(content().string(
                        containsString("Volver a mis órdenes")
                ))
                .andExpect(content().string(
                        not(containsString("¡Gracias por tu compra!"))
                ));

        verify(ordenCompraService)
                .consultarOrdenPropia(CORREO, ORDEN_ID);

        verifyNoMoreInteractions(ordenCompraService);
    }

    /**
     * Comprueba que el controlador no presenta
     * información cuando el servicio rechaza
     * la consulta de una orden ajena.
     *
     * Complementa las pruebas reales de aislamiento
     * de órdenes realizadas en Service y Repository.
     */
    @Test
    void mostrarDetalle_ordenAjena_retorna404()
            throws Exception {

        when(ordenCompraService.consultarOrdenPropia(
                OTRO_CORREO,
                ORDEN_ID
        )).thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No se encontró la orden solicitada."
        ));

        mockMvc.perform(
                        get("/mis-ordenes/{ordenId}", ORDEN_ID)
                                .with(user(OTRO_CORREO).roles("USER"))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        not(containsString("La llamada de Cthulhu"))
                ));

        verify(ordenCompraService)
                .consultarOrdenPropia(OTRO_CORREO, ORDEN_ID);

        verifyNoMoreInteractions(ordenCompraService);
    }

    /**
     * Comprueba que una orden inexistente devuelve
     * el mismo código HTTP que una orden ajena.
     *
     * Esto evita revelar la existencia de órdenes
     * pertenecientes a otros clientes.
     */
    @Test
    void mostrarDetalle_ordenInexistente_retorna404()
            throws Exception {

        Long ordenInexistente = Long.MAX_VALUE;

        when(ordenCompraService.consultarOrdenPropia(
                CORREO,
                ordenInexistente
        )).thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No se encontró la orden solicitada."
        ));

        mockMvc.perform(
                        get("/mis-ordenes/{ordenId}", ordenInexistente)
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isNotFound());

        verify(ordenCompraService)
                .consultarOrdenPropia(CORREO, ordenInexistente);
    }

    /**
     * Comprueba que el historial requiere
     * una sesión previamente autenticada.
     */
    @Test
    void mostrarHistorial_sinAutenticacion_redirigeAlLogin()
            throws Exception {

        mockMvc.perform(get("/mis-ordenes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iniciar-sesion"));

        verifyNoInteractions(ordenCompraService);
    }

    /**
     * Comprueba que conocer el identificador
     * de una orden no permite consultar sus datos
     * sin autenticarse previamente.
     */
    @Test
    void mostrarDetalle_sinAutenticacion_redirigeAlLogin()
            throws Exception {

        mockMvc.perform(
                        get("/mis-ordenes/{ordenId}", ORDEN_ID)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iniciar-sesion"));

        verifyNoInteractions(ordenCompraService);
    }

    // ======================================================
    // Método auxiliar para preparar datos de prueba.
    // ======================================================

    /**
     * Construye una orden de prueba con los datos
     * históricos de sus artículos e importes.
     */
    private OrdenCompraResponseDTO crearOrden(
            Long ordenId,
            LocalDateTime fecha,
            String estado) {

        DetalleOrdenResponseDTO detalle =
                new DetalleOrdenResponseDTO(
                        20L,
                        "La llamada de Cthulhu",
                        "PASTA_DURA",
                        1,
                        PRECIO,
                        PRECIO
                );

        return new OrdenCompraResponseDTO(
                ordenId,
                fecha,
                estado,
                PRECIO,
                List.of(detalle)
        );
    }
}
