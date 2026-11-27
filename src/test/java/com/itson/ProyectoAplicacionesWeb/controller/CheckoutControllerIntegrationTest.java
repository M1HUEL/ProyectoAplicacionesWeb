
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.*;
import com.itson.ProyectoAplicacionesWeb.model.Direccion;
import com.itson.ProyectoAplicacionesWeb.model.MetodoPago;
import com.itson.ProyectoAplicacionesWeb.service.CarritoService;
import com.itson.ProyectoAplicacionesWeb.service.CheckoutService;
import com.itson.ProyectoAplicacionesWeb.service.DireccionService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración MVC del proceso de compra
 * de Librería Raudskinna.
 *
 * Comprueba la integración entre Spring Security,
 * CheckoutController y las plantillas Thymeleaf.
 *
 * Los servicios de negocio son simulados mediante
 * Mockito, pues sus reglas y operaciones transaccionales
 * cuentan con sus propias pruebas automatizadas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class CheckoutControllerIntegrationTest {

    private static final String CORREO =
            "cliente@raudskinna.test";

    private static final Long CARRITO_ID = 10L;
    private static final Long FORMATO_ID = 20L;
    private static final Long DIRECCION_ID = 30L;
    private static final Long ORDEN_ID = 40L;

    private static final BigDecimal PRECIO =
            new BigDecimal("350.00");

    private static final LocalDateTime FECHA =
            LocalDateTime.of(2026, 11, 21, 18, 11);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CarritoService carritoService;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private DireccionService direccionService;

    @MockitoBean
    private OrdenCompraService ordenCompraService;

    /**
     * Comprueba que el checkout requiere
     * una sesión previamente autenticada.
     */
    @Test
    void mostrarCheckout_sinAutenticacion_redirigeAlLogin()
            throws Exception {

        mockMvc.perform(get("/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iniciar-sesion"));

        verifyNoInteractions(
                carritoService,
                direccionService,
                checkoutService
        );
    }

    /**
     * Comprueba que no puede presentarse el checkout
     * cuando el carrito no contiene artículos.
     */
    @Test
    void mostrarCheckout_carritoVacio_redirigeAlCarrito()
            throws Exception {

        when(carritoService.consultarCarrito(CORREO))
                .thenReturn(new CarritoResponseDTO(
                        CARRITO_ID,
                        List.of(),
                        BigDecimal.ZERO
                ));

        mockMvc.perform(
                        get("/checkout")
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"))
                .andExpect(flash().attribute(
                        "mensajeError",
                        "No se puede confirmar un carrito vacío."
                ));

        verify(carritoService).consultarCarrito(CORREO);

        verifyNoInteractions(
                direccionService,
                checkoutService
        );
    }

    /**
     * Comprueba que una compra física sin direcciones
     * presenta el formulario de registro correspondiente.
     */
    @Test
    void mostrarCheckout_compraFisicaSinDirecciones_muestraFormulario()
            throws Exception {

        CarritoResponseDTO carrito = crearCarritoFisico();

        when(carritoService.consultarCarrito(CORREO))
                .thenReturn(carrito);

        when(direccionService.listarPorUsuario(CORREO))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/checkout")
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attribute("carrito", carrito))
                .andExpect(content().string(
                        containsString("Datos de entrega")
                ))
                .andExpect(content().string(
                        containsString("Todavía no tienes direcciones registradas.")
                ))
                .andExpect(content().string(
                        containsString("Registrar nueva dirección")
                ))
                .andExpect(content().string(
                        containsString("disabled=\"disabled\"")
                ))
                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ));

        verify(direccionService).listarPorUsuario(CORREO);

        verifyNoInteractions(checkoutService);
    }

    /**
     * Comprueba que una compra física con direcciones
     * permite seleccionar los datos de entrega.
     */
    @Test
    void mostrarCheckout_compraFisicaConDireccion_muestraSelector()
            throws Exception {

        Direccion direccion = crearDireccion();

        when(carritoService.consultarCarrito(CORREO))
                .thenReturn(crearCarritoFisico());

        when(direccionService.listarPorUsuario(CORREO))
                .thenReturn(List.of(direccion));

        mockMvc.perform(
                        get("/checkout")
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(content().string(
                        containsString("name=\"direccionId\"")
                ))
                .andExpect(content().string(
                        containsString("Howard Phillips Lovecraft")
                ))
                .andExpect(content().string(
                        containsString("Transferencia bancaria")
                ))
                .andExpect(content().string(
                        containsString("Tarjeta")
                ))
                .andExpect(content().string(
                        containsString("Efectivo")
                ))
                .andExpect(content().string(
                        not(containsString(
                                "Todavía no tienes direcciones registradas."
                        ))
                ));

        verify(direccionService).listarPorUsuario(CORREO);
    }

    /**
     * Comprueba que una compra exclusivamente digital
     * no presenta los campos de dirección de envío.
     */
    @Test
    void mostrarCheckout_compraDigital_noSolicitaDireccion()
            throws Exception {

        when(carritoService.consultarCarrito(CORREO))
                .thenReturn(crearCarritoDigital());

        when(direccionService.listarPorUsuario(CORREO))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/checkout")
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(content().string(
                        containsString("Entrega digital")
                ))
                .andExpect(content().string(
                        containsString("No necesitas registrar una dirección")
                ))
                .andExpect(content().string(
                        not(containsString("name=\"direccionId\""))
                ))
                .andExpect(content().string(
                        not(containsString("Registrar nueva dirección"))
                ))
                .andExpect(content().string(
                        not(containsString("disabled=\"disabled\""))
                ));
    }

    /**
     * Comprueba que una compra mixta requiere
     * dirección de envío por sus artículos físicos.
     */
    @Test
    void mostrarCheckout_compraMixta_solicitaDireccion()
            throws Exception {

        CarritoResponseDTO carrito = new CarritoResponseDTO(
                CARRITO_ID,
                List.of(
                        crearCarritoFisico().items().get(0),
                        crearCarritoDigital().items().get(0)
                ),
                new BigDecimal("550.00")
        );

        when(carritoService.consultarCarrito(CORREO))
                .thenReturn(carrito);

        when(direccionService.listarPorUsuario(CORREO))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/checkout")
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Datos de entrega")
                ))
                .andExpect(content().string(
                        containsString("Registrar nueva dirección")
                ))
                .andExpect(content().string(
                        containsString("$550.00 MXN")
                ))
                .andExpect(content().string(
                        not(containsString("Entrega digital"))
                ));
    }

    /**
     * Comprueba el registro de una dirección propia
     * utilizando los datos proporcionados en el formulario.
     */
    @Test
    void registrarDireccion_datosValidos_redirigeConMensajeExito()
            throws Exception {

        DireccionRequestDTO datos = crearDatosDireccion();

        // Preparar completamente la dirección simulada
        // antes de configurar el servicio.
        Direccion direccion = crearDireccion();

        // Configurar la respuesta del servicio utilizando
        // la dirección previamente preparada.
        when(direccionService.registrar(CORREO, datos))
                .thenReturn(direccion);

        mockMvc.perform(
                        post("/checkout/direcciones")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("nombreDestinatario",
                                        datos.nombreDestinatario())
                                .param("calle", datos.calle())
                                .param("numeroExterior",
                                        datos.numeroExterior())
                                .param("numeroInterior", "")
                                .param("colonia", datos.colonia())
                                .param("codigoPostal", datos.codigoPostal())
                                .param("ciudad", datos.ciudad())
                                .param("estado", datos.estado())
                                .param("pais", datos.pais())
                                .param("referencias", datos.referencias())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout"))
                .andExpect(flash().attribute(
                        "direccionSeleccionadaId",
                        DIRECCION_ID
                ))
                .andExpect(flash().attribute(
                        "mensajeExito",
                        "La dirección se registró correctamente."
                ));

        verify(direccionService).registrar(CORREO, datos);

        verifyNoInteractions(checkoutService);
    }

    /**
     * Comprueba que una dirección inválida conserva
     * los datos capturados y presenta el error de negocio.
     */
    @Test
    void registrarDireccion_datosInvalidos_redirigeConError()
            throws Exception {

        when(direccionService.registrar(
                eq(CORREO),
                any(DireccionRequestDTO.class)
        )).thenThrow(new IllegalArgumentException(
                "El código postal debe contener 5 dígitos."
        ));

        mockMvc.perform(
                        post("/checkout/direcciones")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("codigoPostal", "123")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout"))
                .andExpect(flash().attributeExists("datosDireccion"))
                .andExpect(flash().attribute(
                        "mensajeError",
                        "El código postal debe contener 5 dígitos."
                ));

        verify(direccionService).registrar(
                eq(CORREO),
                any(DireccionRequestDTO.class)
        );

        verifyNoInteractions(checkoutService);
    }

    /**
     * Comprueba que el formulario de registro de
     * direcciones permanece protegido mediante CSRF.
     */
    @Test
    void registrarDireccion_sinCsrf_retorna403()
            throws Exception {

        mockMvc.perform(
                        post("/checkout/direcciones")
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(direccionService);
    }

    /**
     * Comprueba que una compra exitosa redirige
     * hacia la orden registrada por el servicio.
     */
    @Test
    void confirmarCompra_solicitudValida_redirigeAConfirmacion()
            throws Exception {

        when(checkoutService.confirmarCompra(
                CORREO,
                DIRECCION_ID,
                MetodoPago.TRANSFERENCIA
        )).thenReturn(crearRespuestaCheckout());

        mockMvc.perform(
                        post("/checkout/confirmar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("direccionId",
                                        DIRECCION_ID.toString())
                                .param("metodoPago", "TRANSFERENCIA")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/checkout/confirmacion/" + ORDEN_ID
                ));

        verify(checkoutService).confirmarCompra(
                CORREO,
                DIRECCION_ID,
                MetodoPago.TRANSFERENCIA
        );

        verifyNoMoreInteractions(checkoutService);
    }

    /**
     * Comprueba que una compra digital puede
     * confirmarse sin seleccionar una dirección.
     */
    @Test
    void confirmarCompra_compraDigital_admiteDireccionNula()
            throws Exception {

        when(checkoutService.confirmarCompra(
                CORREO,
                null,
                MetodoPago.TARJETA
        )).thenReturn(crearRespuestaCheckout());

        mockMvc.perform(
                        post("/checkout/confirmar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("metodoPago", "TARJETA")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/checkout/confirmacion/" + ORDEN_ID
                ));

        verify(checkoutService).confirmarCompra(
                CORREO,
                null,
                MetodoPago.TARJETA
        );
    }

    /**
     * Comprueba que una regla de negocio rechazada
     * devuelve al cliente al checkout.
     */
    @Test
    void confirmarCompra_stockInsuficiente_redirigeConError()
            throws Exception {

        String mensaje = "Existencias insuficientes.";

        when(checkoutService.confirmarCompra(
                CORREO,
                DIRECCION_ID,
                MetodoPago.TRANSFERENCIA
        )).thenThrow(new IllegalArgumentException(mensaje));

        mockMvc.perform(
                        post("/checkout/confirmar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("direccionId",
                                        DIRECCION_ID.toString())
                                .param("metodoPago", "TRANSFERENCIA")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout"))
                .andExpect(flash().attribute(
                        "mensajeError",
                        mensaje
                ))
                .andExpect(flash().attribute(
                        "direccionSeleccionadaId",
                        DIRECCION_ID
                ))
                .andExpect(flash().attribute(
                        "metodoPagoSeleccionado",
                        MetodoPago.TRANSFERENCIA
                ));

        verify(checkoutService).confirmarCompra(
                CORREO,
                DIRECCION_ID,
                MetodoPago.TRANSFERENCIA
        );
    }

    /**
     * Comprueba que confirmar una compra requiere
     * un token CSRF válido.
     */
    @Test
    void confirmarCompra_sinCsrf_retorna403()
            throws Exception {

        mockMvc.perform(
                        post("/checkout/confirmar")
                                .with(user(CORREO).roles("USER"))
                                .param("metodoPago", "TRANSFERENCIA")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(checkoutService);
    }

    /**
     * Comprueba que un correo manipulado desde el
     * navegador no sustituye la identidad autenticada.
     */
    @Test
    void confirmarCompra_correoManipulado_utilizaIdentidadAutenticada()
            throws Exception {

        when(checkoutService.confirmarCompra(
                CORREO,
                DIRECCION_ID,
                MetodoPago.TRANSFERENCIA
        )).thenReturn(crearRespuestaCheckout());

        mockMvc.perform(
                        post("/checkout/confirmar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("correo", "otro@raudskinna.test")
                                .param("direccionId",
                                        DIRECCION_ID.toString())
                                .param("metodoPago", "TRANSFERENCIA")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/checkout/confirmacion/" + ORDEN_ID
                ));

        verify(checkoutService).confirmarCompra(
                CORREO,
                DIRECCION_ID,
                MetodoPago.TRANSFERENCIA
        );

        verifyNoMoreInteractions(checkoutService);
    }

    /**
     * Comprueba que la confirmación presenta la orden
     * persistida y sus detalles comerciales.
     */
    @Test
    void mostrarConfirmacion_ordenPropia_renderizaLosDatos()
            throws Exception {

        OrdenCompraResponseDTO orden = crearOrden();

        when(ordenCompraService.consultarOrdenPropia(
                CORREO,
                ORDEN_ID
        )).thenReturn(orden);

        mockMvc.perform(
                        get("/checkout/confirmacion/{ordenId}", ORDEN_ID)
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("orden-confirmada"))
                .andExpect(model().attribute("orden", orden))
                .andExpect(content().string(
                        containsString("¡Gracias por tu compra!")
                ))
                .andExpect(content().string(
                        containsString("Orden #" + ORDEN_ID)
                ))
                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ))
                .andExpect(content().string(
                        containsString("Pendiente")
                ));

        verify(ordenCompraService).consultarOrdenPropia(
                CORREO,
                ORDEN_ID
        );

        // La confirmación no debe procesar otra compra.
        verifyNoInteractions(checkoutService);
    }

    /**
     * Comprueba que actualizar la confirmación
     * únicamente consulta la orden existente.
     */
    @Test
    void mostrarConfirmacion_consultasRepetidas_noGeneraOtraCompra()
            throws Exception {

        when(ordenCompraService.consultarOrdenPropia(
                CORREO,
                ORDEN_ID
        )).thenReturn(crearOrden());

        for (int i = 0; i < 2; i++) {

            mockMvc.perform(
                            get("/checkout/confirmacion/{ordenId}", ORDEN_ID)
                                    .with(user(CORREO).roles("USER"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(view().name("orden-confirmada"));
        }

        verify(ordenCompraService, times(2))
                .consultarOrdenPropia(CORREO, ORDEN_ID);

        verifyNoInteractions(checkoutService);
    }

    /**
     * Comprueba que una orden ajena o inexistente
     * devuelve HTTP 404, sin presentar sus datos.
     */
    @Test
    void mostrarConfirmacion_ordenNoEncontrada_retorna404()
            throws Exception {

        when(ordenCompraService.consultarOrdenPropia(
                CORREO,
                ORDEN_ID
        )).thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No se encontró la orden solicitada."
        ));

        mockMvc.perform(
                        get("/checkout/confirmacion/{ordenId}", ORDEN_ID)
                                .with(user(CORREO).roles("USER"))
                )
                .andExpect(status().isNotFound());

        verify(ordenCompraService).consultarOrdenPropia(
                CORREO,
                ORDEN_ID
        );

        verifyNoInteractions(checkoutService);
    }

    /**
     * Comprueba que la confirmación de compra
     * también requiere autenticación.
     */
    @Test
    void mostrarConfirmacion_sinAutenticacion_redirigeAlLogin()
            throws Exception {

        mockMvc.perform(
                        get("/checkout/confirmacion/{ordenId}", ORDEN_ID)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iniciar-sesion"));

        verifyNoInteractions(ordenCompraService);
    }

    // ======================================================
    // Métodos auxiliares para preparar datos de prueba.
    // ======================================================

    /**
     * Construye un carrito con un artículo físico.
     */
    private CarritoResponseDTO crearCarritoFisico() {

        ItemCarritoResponseDTO articulo =
                new ItemCarritoResponseDTO(
                        FORMATO_ID,
                        "La llamada de Cthulhu",
                        "PASTA_DURA",
                        1,
                        PRECIO,
                        PRECIO
                );

        return new CarritoResponseDTO(
                CARRITO_ID,
                List.of(articulo),
                PRECIO
        );
    }

    /**
     * Construye un carrito exclusivamente digital.
     */
    private CarritoResponseDTO crearCarritoDigital() {

        BigDecimal precioDigital = new BigDecimal("200.00");

        ItemCarritoResponseDTO articulo =
                new ItemCarritoResponseDTO(
                        21L,
                        "La llamada de Cthulhu",
                        "DIGITAL",
                        1,
                        precioDigital,
                        precioDigital
                );

        return new CarritoResponseDTO(
                CARRITO_ID,
                List.of(articulo),
                precioDigital
        );
    }

    /**
     * Construye una dirección simulada perteneciente
     * al cliente autenticado.
     */
    private Direccion crearDireccion() {

        Direccion direccion = mock(Direccion.class);

        when(direccion.getId()).thenReturn(DIRECCION_ID);

        when(direccion.getNombreDestinatario())
                .thenReturn("Howard Phillips Lovecraft");

        when(direccion.getCalle()).thenReturn("Avenida R'lyeh");
        when(direccion.getNumeroExterior()).thenReturn("666");

        when(direccion.getColonia())
                .thenReturn("Las Montañas de la Locura");

        when(direccion.getCodigoPostal()).thenReturn("85000");
        when(direccion.getCiudad()).thenReturn("Ciudad Obregón");
        when(direccion.getEstado()).thenReturn("Sonora");

        return direccion;
    }

    /**
     * Construye los datos capturados en el
     * formulario de registro de dirección.
     */
    private DireccionRequestDTO crearDatosDireccion() {

        return new DireccionRequestDTO(
                "Howard Phillips Lovecraft",
                "Avenida R'lyeh",
                "666",
                "",
                "Las Montañas de la Locura",
                "85000",
                "Ciudad Obregón",
                "Sonora",
                "México",
                "Frente al templo de Cthulhu"
        );
    }

    /**
     * Simula la respuesta del procesamiento
     * exitoso de una compra.
     */
    private CheckoutResponseDTO crearRespuestaCheckout() {

        return new CheckoutResponseDTO(
                ORDEN_ID,
                FECHA,
                "PENDIENTE",
                PRECIO
        );
    }

    /**
     * Construye la respuesta de una orden persistida
     * con sus detalles e importes históricos.
     */
    private OrdenCompraResponseDTO crearOrden() {

        DetalleOrdenResponseDTO detalle =
                new DetalleOrdenResponseDTO(
                        FORMATO_ID,
                        "La llamada de Cthulhu",
                        "PASTA_DURA",
                        1,
                        PRECIO,
                        PRECIO
                );

        return new OrdenCompraResponseDTO(
                ORDEN_ID,
                FECHA,
                "PENDIENTE",
                PRECIO,
                List.of(detalle)
        );
    }
}
