
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.PagoRepository;
import com.itson.ProyectoAplicacionesWeb.service.OrdenCompraService;
import com.itson.ProyectoAplicacionesWeb.service.PagoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración del detalle administrativo
 * de órdenes de Librería Raudskinna.
 *
 * Comprueba la presentación de los artículos,
 * la dirección histórica, el registro de pago
 * y las restricciones de acceso administrativo.
 *
 * Utiliza el servicio real de pagos para comprobar
 * la seguridad aplicada mediante @PreAuthorize.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class OrdenCompraControllerIntegrationTest {

    private static final String ADMIN =
            "admin@raudskinna.test";

    private static final String CLIENTE =
            "cliente@raudskinna.test";

    private static final Long ORDEN_ID = 5L;

    private static final BigDecimal TOTAL =
            new BigDecimal("350.00");

    private static final LocalDateTime FECHA =
            LocalDateTime.of(2026, 11, 21, 18, 11);

    private static final String RUTA =
            "/admin/ordenes/{ordenId}/detalle";

    @Autowired
    private MockMvc mockMvc;

    // Se simula la recuperación de las órdenes.
    @MockitoBean
    private OrdenCompraService ordenCompraService;

    // Se simula únicamente la persistencia de pagos.
    @MockitoBean
    private PagoRepository pagoRepository;

    // Servicio real, incluyendo seguridad por métodos.
    @Autowired
    private PagoService pagoService;

    /**
     * Comprueba que una orden física reciente
     * presenta sus datos históricos de entrega
     * y el registro de pago correspondiente.
     */
    @Test
    void verDetalle_ordenConDireccionYPago_muestraDatos()
            throws Exception {

        // 1. Preparar los datos de prueba.
        OrdenCompra orden = crearOrden(
                Formato.PASTA_DURA,
                true
        );

        Pago pago = crearPago(orden);

        // 2. Configurar las respuestas simuladas.
        when(ordenCompraService.buscarPorId(ORDEN_ID))
                .thenReturn(orden);

        when(pagoRepository.findByOrdenCompraId(ORDEN_ID))
                .thenReturn(Optional.of(pago));

        // 3. Consultar el detalle como administrador.
        mockMvc.perform(
                        get(RUTA, ORDEN_ID)
                                .with(user(ADMIN).roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("detalle-pedido"))
                .andExpect(model().attribute("orden", orden))
                .andExpect(model().attribute("pago", pago))
                .andExpect(content().string(
                        containsString("Orden #5")
                ))
                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("Datos de entrega")
                ))
                .andExpect(content().string(
                        containsString("Howard Phillips Lovecraft")
                ))
                .andExpect(content().string(
                        containsString("Las Montañas de la Locura")
                ))
                .andExpect(content().string(
                        containsString("85000")
                ))
                .andExpect(content().string(
                        containsString("Frente al templo de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("Información del pago")
                ))
                .andExpect(content().string(
                        containsString("Transferencia bancaria")
                ))
                .andExpect(content().string(
                        containsString("PENDIENTE")
                ))
                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ));

        // 4. Verificar las consultas realizadas.
        verify(ordenCompraService).buscarPorId(ORDEN_ID);

        verify(pagoRepository)
                .findByOrdenCompraId(ORDEN_ID);

        verifyNoMoreInteractions(pagoRepository);
    }

    /**
     * Comprueba la compatibilidad con órdenes
     * anteriores a la implementación de los
     * módulos de direcciones y pagos.
     */
    @Test
    void verDetalle_ordenAnterior_muestraMensajesInformativos()
            throws Exception {

        OrdenCompra orden = crearOrden(
                Formato.PASTA_DURA,
                false
        );

        when(ordenCompraService.buscarPorId(ORDEN_ID))
                .thenReturn(orden);

        when(pagoRepository.findByOrdenCompraId(ORDEN_ID))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get(RUTA, ORDEN_ID)
                                .with(user(ADMIN).roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("detalle-pedido"))
                .andExpect(content().string(
                        containsString("Datos de entrega")
                ))
                .andExpect(content().string(
                        containsString(
                                "Esta orden no tiene una dirección de envío registrada."
                        )
                ))
                .andExpect(content().string(
                        containsString("Información del pago")
                ))
                .andExpect(content().string(
                        containsString(
                                "Esta orden no tiene un registro de pago asociado."
                        )
                ));

        verify(ordenCompraService).buscarPorId(ORDEN_ID);

        verify(pagoRepository)
                .findByOrdenCompraId(ORDEN_ID);
    }

    /**
     * Comprueba que una compra exclusivamente
     * digital puede presentar su registro de pago
     * sin disponer de una dirección de envío.
     */
    @Test
    void verDetalle_ordenDigital_muestraPagoSinDireccion()
            throws Exception {

        OrdenCompra orden = crearOrden(
                Formato.DIGITAL,
                false
        );

        Pago pago = crearPago(orden);

        when(ordenCompraService.buscarPorId(ORDEN_ID))
                .thenReturn(orden);

        when(pagoRepository.findByOrdenCompraId(ORDEN_ID))
                .thenReturn(Optional.of(pago));

        mockMvc.perform(
                        get(RUTA, ORDEN_ID)
                                .with(user(ADMIN).roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("detalle-pedido"))
                .andExpect(content().string(
                        containsString("DIGITAL")
                ))
                .andExpect(content().string(
                        containsString(
                                "Esta orden no tiene una dirección de envío registrada."
                        )
                ))
                .andExpect(content().string(
                        containsString("Transferencia bancaria")
                ))
                .andExpect(content().string(
                        not(containsString(
                                "Esta orden no tiene un registro de pago asociado."
                        ))
                ));

        verify(pagoRepository)
                .findByOrdenCompraId(ORDEN_ID);
    }

    /**
     * Comprueba que una orden inexistente
     * devuelve HTTP 404.
     *
     * No debe consultarse el registro de pago
     * si la orden solicitada no existe.
     */
    @Test
    void verDetalle_ordenInexistente_retorna404()
            throws Exception {

        when(ordenCompraService.buscarPorId(ORDEN_ID))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la orden solicitada."
                ));

        mockMvc.perform(
                        get(RUTA, ORDEN_ID)
                                .with(user(ADMIN).roles("ADMIN"))
                )
                .andExpect(status().isNotFound());

        verify(ordenCompraService).buscarPorId(ORDEN_ID);

        verifyNoInteractions(pagoRepository);
    }

    /**
     * Comprueba que un cliente autenticado
     * no puede acceder al detalle administrativo.
     */
    @Test
    void verDetalle_usuarioSinRolAdmin_retorna403()
            throws Exception {

        mockMvc.perform(
                        get(RUTA, ORDEN_ID)
                                .with(user(CLIENTE).roles("USER"))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                ordenCompraService,
                pagoRepository
        );
    }

    /**
     * Comprueba que un visitante no autenticado
     * es enviado al formulario administrativo.
     */
    @Test
    void verDetalle_sinAutenticacion_redirigeAlLoginAdmin()
            throws Exception {

        mockMvc.perform(get(RUTA, ORDEN_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        verifyNoInteractions(
                ordenCompraService,
                pagoRepository
        );
    }

    /**
     * Comprueba directamente la seguridad aplicada
     * al servicio real de consulta de pagos.
     *
     * Un usuario con rol USER no debe poder ejecutar
     * esta operación administrativa, incluso si
     * intenta acceder fuera del controlador.
     */
    @Test
    @WithMockUser(
            username = CLIENTE,
            roles = "USER"
    )
    void consultarPagoPorOrden_usuarioSinRolAdmin_deniegaAcceso() {

        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.consultarPagoPorOrden(ORDEN_ID)
        );

        // La seguridad debe impedir la consulta
        // antes de alcanzar el repositorio.
        verifyNoInteractions(pagoRepository);
    }

    // ======================================================
    // Métodos auxiliares para preparar datos de prueba.
    // ======================================================

    /**
     * Construye una orden simulada con sus detalles
     * y, opcionalmente, su dirección histórica.
     */
    private OrdenCompra crearOrden(
            Formato formato,
            boolean requiereDireccion) {

        // 1. Preparar al cliente.
        Usuario cliente = new Usuario(
                null,
                "Cliente de prueba",
                CLIENTE,
                "hash-no-utilizado",
                Rol.USER
        );

        // 2. Preparar el libro adquirido.
        Libro libro = mock(Libro.class);

        when(libro.getTitulo())
                .thenReturn("La llamada de Cthulhu");

        // 3. Preparar la presentación comercial.
        FormatoLibro formatoLibro = mock(FormatoLibro.class);

        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(formato);

        // 4. Preparar la orden.
        OrdenCompra orden = mock(OrdenCompra.class);

        when(orden.getId()).thenReturn(ORDEN_ID);
        when(orden.getUsuario()).thenReturn(cliente);
        when(orden.getFecha()).thenReturn(FECHA);
        when(orden.getEstado()).thenReturn(EstadoOrden.PENDIENTE);
        when(orden.getTotal()).thenReturn(TOTAL);

        // 5. Preparar el detalle con su precio histórico.
        DetalleOrden detalle = new DetalleOrden(
                orden,
                formatoLibro,
                1,
                TOTAL
        );

        when(orden.getDetalles())
                .thenReturn(List.of(detalle));

        // 6. Agregar la dirección histórica
        // únicamente cuando corresponda.
        if (requiereDireccion) {

            DireccionEnvio envio = new DireccionEnvio(
                    "Howard Phillips Lovecraft",
                    "Avenida R'lyeh",
                    "666",
                    null,
                    "Las Montañas de la Locura",
                    "85000",
                    "Ciudad Obregón",
                    "Sonora",
                    "México",
                    "Frente al templo de Cthulhu"
            );

            when(orden.getDireccionEnvio())
                    .thenReturn(envio);
        }

        return orden;
    }

    /**
     * Construye un registro de pago pendiente
     * asociado a la orden de prueba.
     *
     * No representa un cobro bancario real.
     */
    private Pago crearPago(OrdenCompra orden) {

        return new Pago(
                orden,
                TOTAL,
                MetodoPago.TRANSFERENCIA,
                EstadoPago.PENDIENTE,
                FECHA
        );
    }
}
