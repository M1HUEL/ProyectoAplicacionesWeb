
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.AgregarItemCarritoRequestDTO;
import com.itson.ProyectoAplicacionesWeb.dto.CarritoResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.ItemCarritoResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.ModificarCantidadCarritoRequestDTO;
import com.itson.ProyectoAplicacionesWeb.service.CarritoService;

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
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.*;

/**
 * Pruebas de integración del controlador MVC encargado
 * del carrito de compras de Librería Raudskinna.
 *
 * Se comprueba la integración entre:
 *
 * - Spring MVC.
 * - Spring Security.
 * - Thymeleaf.
 * - CarritoService.
 *
 * Se utiliza la cadena real de filtros de seguridad,
 * simulando CarritoService mediante Mockito.
 *
 * El perfil integrationtest utiliza H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class CarritoControllerIntegrationTest {

    private static final String CORREO =
            "cliente@raudskinna.test";

    private static final Long CARRITO_ID = 10L;
    private static final Long FORMATO_ID = 20L;

    private static final BigDecimal PRECIO =
            new BigDecimal("350.00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CarritoService carritoService;

    // =====================================================
    // MÉTODOS AUXILIARES
    // =====================================================

    /**
     * Construye una respuesta representando
     * un carrito sin artículos.
     */
    private CarritoResponseDTO crearCarritoVacio() {

        return new CarritoResponseDTO(
                CARRITO_ID,
                List.of(),
                BigDecimal.ZERO
        );
    }

    /**
     * Construye una respuesta simulada con una presentación
     * comercial y la cantidad indicada.
     *
     * Los importes representan los datos que normalmente
     * proporciona CarritoService.
     *
     * @param cantidad unidades del artículo.
     * @return respuesta simulada del carrito.
     */
    private CarritoResponseDTO crearCarritoConArticulo(
            int cantidad) {

        BigDecimal subtotal = PRECIO.multiply(
                BigDecimal.valueOf(cantidad)
        );

        ItemCarritoResponseDTO item =
                new ItemCarritoResponseDTO(
                        FORMATO_ID,
                        "La llamada de Cthulhu",
                        "PASTA_DURA",
                        cantidad,
                        PRECIO,
                        subtotal
                );

        return new CarritoResponseDTO(
                CARRITO_ID,
                List.of(item),
                subtotal
        );
    }

    // =====================================================
    // PRUEBAS DE CONSULTA DEL CARRITO
    // =====================================================

    /**
     * CI-01: Un visitante no autenticado debe ser
     * redirigido al formulario de inicio de sesión.
     */
    @Test
    void mostrarCarrito_sinAutenticacion_redirigeAlLogin()
            throws Exception {

        // ACT y ASSERT
        mockMvc.perform(get("/carrito"))

                .andExpect(status().is3xxRedirection())

                .andExpect(redirectedUrl("/iniciar-sesion"));

        // Spring Security debe bloquear el acceso al servicio.
        verifyNoInteractions(carritoService);
    }

    /**
     * CI-02: Un cliente autenticado sin artículos debe
     * visualizar el estado correspondiente al carrito vacío.
     */
    @Test
    void mostrarCarrito_vacio_renderizaMensajeInformativo()
            throws Exception {

        // ARRANGE
        CarritoResponseDTO carrito = crearCarritoVacio();

        when(carritoService.consultarCarrito(CORREO))
                .thenReturn(carrito);

        // ACT y ASSERT
        mockMvc.perform(
                        get("/carrito")
                                .with(user(CORREO).roles("USER"))
                )

                .andExpect(status().isOk())
                .andExpect(view().name("carrito"))
                .andExpect(model().attribute("carrito", carrito))

                .andExpect(content().string(
                        containsString("Tu carrito está vacío")
                ))

                .andExpect(content().string(
                        containsString("Explorar catálogo")
                ))

                .andExpect(content().string(
                        not(containsString("Resumen de compra"))
                ));

        verify(carritoService).consultarCarrito(CORREO);

        verifyNoMoreInteractions(carritoService);
    }

    /**
     * CI-03: Un carrito con artículos debe presentar
     * los datos comerciales proporcionados por el servicio.
     *
     * También se comprueba que los formularios incluyan
     * el token CSRF generado por Spring Security.
     */
    @Test
    void mostrarCarrito_conArticulos_renderizaDatosComerciales()
            throws Exception {

        // ARRANGE
        CarritoResponseDTO carrito =
                crearCarritoConArticulo(3);

        when(carritoService.consultarCarrito(CORREO))
                .thenReturn(carrito);

        // ACT y ASSERT
        mockMvc.perform(
                        get("/carrito")
                                .with(user(CORREO).roles("USER"))
                )

                .andExpect(status().isOk())
                .andExpect(view().name("carrito"))
                .andExpect(model().attribute("carrito", carrito))

                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))

                .andExpect(content().string(
                        containsString("Pasta dura")
                ))

                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ))

                .andExpect(content().string(
                        containsString("$1,050.00 MXN")
                ))

                .andExpect(content().string(
                        containsString("Resumen de compra")
                ))

                .andExpect(content().string(
                        containsString("action=\"/carrito/modificar\"")
                ))

                .andExpect(content().string(
                        containsString("action=\"/carrito/eliminar\"")
                ))

                .andExpect(content().string(
                        containsString("name=\"_csrf\"")
                ))

                .andExpect(content().string(
                        not(containsString("Tu carrito está vacío"))
                ));

        verify(carritoService).consultarCarrito(CORREO);

        verifyNoMoreInteractions(carritoService);
    }

    // =====================================================
    // PRUEBAS DE INCORPORACIÓN DE ARTÍCULOS
    // =====================================================

    /**
     * CI-04: Agregar un artículo correctamente debe
     * aplicar el patrón Post/Redirect/Get y proporcionar
     * un mensaje temporal de éxito.
     */
    @Test
    void agregarItem_solicitudValida_redirigeConMensajeExito()
            throws Exception {

        // ARRANGE
        AgregarItemCarritoRequestDTO solicitud =
                new AgregarItemCarritoRequestDTO(
                        FORMATO_ID,
                        1
                );

        when(carritoService.agregarItem(CORREO, solicitud))
                .thenReturn(crearCarritoConArticulo(1));

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/agregar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("formatoLibroId", "20")
                                .param("cantidad", "1")
                )

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"))

                .andExpect(flash().attribute(
                        "mensajeExito",
                        "El artículo se agregó correctamente al carrito."
                ))

                .andExpect(flash().attributeCount(1));

        verify(carritoService).agregarItem(
                CORREO,
                solicitud
        );

        verifyNoMoreInteractions(carritoService);
    }

    /**
     * CI-05: Cuando CarritoService rechaza una solicitud
     * por existencias insuficientes, el controlador debe
     * informar al cliente mediante un mensaje de error.
     */
    @Test
    void agregarItem_stockInsuficiente_redirigeConMensajeError()
            throws Exception {

        // ARRANGE
        AgregarItemCarritoRequestDTO solicitud =
                new AgregarItemCarritoRequestDTO(
                        FORMATO_ID,
                        6
                );

        String mensaje =
                "La cantidad solicitada supera las existencias disponibles.";

        when(carritoService.agregarItem(CORREO, solicitud))
                .thenThrow(new IllegalArgumentException(mensaje));

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/agregar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("formatoLibroId", "20")
                                .param("cantidad", "6")
                )

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"))

                .andExpect(flash().attribute(
                        "mensajeError",
                        mensaje
                ))

                .andExpect(flash().attributeCount(1));

        verify(carritoService).agregarItem(
                CORREO,
                solicitud
        );

        verifyNoMoreInteractions(carritoService);
    }

    /**
     * CI-06: Una solicitud POST autenticada que no incluya
     * un token CSRF válido debe ser rechazada.
     */
    @Test
    void agregarItem_sinCsrf_retorna403()
            throws Exception {

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/agregar")
                                .with(user(CORREO).roles("USER"))
                                .param("formatoLibroId", "20")
                                .param("cantidad", "1")
                )

                .andExpect(status().isForbidden());

        verifyNoInteractions(carritoService);
    }

    /**
     * CI-07: Un visitante no autenticado no puede
     * agregar artículos al carrito.
     *
     * Se incluye CSRF para comprobar específicamente
     * la restricción de autenticación.
     */
    @Test
    void agregarItem_sinAutenticacion_redirigeAlLogin()
            throws Exception {

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/agregar")
                                .with(csrf())
                                .param("formatoLibroId", "20")
                                .param("cantidad", "1")
                )

                .andExpect(status().is3xxRedirection())

                .andExpect(redirectedUrl("/iniciar-sesion"));

        verifyNoInteractions(carritoService);
    }

    // =====================================================
    // PRUEBAS DE MODIFICACIÓN DE CANTIDADES
    // =====================================================

    /**
     * CI-08: Una modificación válida debe delegarse
     * al servicio y producir un mensaje temporal de éxito.
     */
    @Test
    void modificarCantidad_solicitudValida_redirigeConMensajeExito()
            throws Exception {

        // ARRANGE
        ModificarCantidadCarritoRequestDTO solicitud =
                new ModificarCantidadCarritoRequestDTO(3);

        when(carritoService.modificarCantidad(
                CORREO,
                FORMATO_ID,
                solicitud
        )).thenReturn(crearCarritoConArticulo(3));

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/modificar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("formatoLibroId", "20")
                                .param("cantidad", "3")
                )

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"))

                .andExpect(flash().attribute(
                        "mensajeExito",
                        "La cantidad se actualizó correctamente."
                ))

                .andExpect(flash().attributeCount(1));

        verify(carritoService).modificarCantidad(
                CORREO,
                FORMATO_ID,
                solicitud
        );

        verifyNoMoreInteractions(carritoService);
    }

    /**
     * CI-09: Una cantidad inválida rechazada por el servicio
     * debe generar un mensaje de error para el cliente.
     */
    @Test
    void modificarCantidad_cantidadInvalida_redirigeConMensajeError()
            throws Exception {

        // ARRANGE
        ModificarCantidadCarritoRequestDTO solicitud =
                new ModificarCantidadCarritoRequestDTO(0);

        String mensaje =
                "La cantidad debe ser mayor que cero.";

        when(carritoService.modificarCantidad(
                CORREO,
                FORMATO_ID,
                solicitud
        )).thenThrow(new IllegalArgumentException(mensaje));

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/modificar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("formatoLibroId", "20")
                                .param("cantidad", "0")
                )

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"))

                .andExpect(flash().attribute(
                        "mensajeError",
                        mensaje
                ))

                .andExpect(flash().attributeCount(1));

        verify(carritoService).modificarCantidad(
                CORREO,
                FORMATO_ID,
                solicitud
        );

        verifyNoMoreInteractions(carritoService);
    }

    /**
     * CI-10: Si el artículo no pertenece al carrito
     * del cliente, el controlador debe presentar el
     * mensaje proporcionado por CarritoService.
     */
    @Test
    void modificarCantidad_articuloInexistente_redirigeConMensajeError()
            throws Exception {

        // ARRANGE
        ModificarCantidadCarritoRequestDTO solicitud =
                new ModificarCantidadCarritoRequestDTO(2);

        String mensaje =
                "El artículo no existe en el carrito.";

        when(carritoService.modificarCantidad(
                CORREO,
                FORMATO_ID,
                solicitud
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        mensaje
                )
        );

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/modificar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("formatoLibroId", "20")
                                .param("cantidad", "2")
                )

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"))

                .andExpect(flash().attribute(
                        "mensajeError",
                        mensaje
                ))

                .andExpect(flash().attributeCount(1));

        verify(carritoService).modificarCantidad(
                CORREO,
                FORMATO_ID,
                solicitud
        );

        verifyNoMoreInteractions(carritoService);
    }

    /**
     * CI-11: Modificar cantidades sin token CSRF
     * debe ser rechazado por Spring Security.
     */
    @Test
    void modificarCantidad_sinCsrf_retorna403()
            throws Exception {

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/modificar")
                                .with(user(CORREO).roles("USER"))
                                .param("formatoLibroId", "20")
                                .param("cantidad", "3")
                )

                .andExpect(status().isForbidden());

        verifyNoInteractions(carritoService);
    }

    // =====================================================
    // PRUEBAS DE ELIMINACIÓN DE ARTÍCULOS
    // =====================================================

    /**
     * CI-12: Una eliminación válida debe delegarse
     * al servicio y redirigir al carrito.
     */
    @Test
    void eliminarItem_solicitudValida_redirigeConMensajeExito()
            throws Exception {

        // ARRANGE
        when(carritoService.eliminarItem(
                CORREO,
                FORMATO_ID
        )).thenReturn(crearCarritoVacio());

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/eliminar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("formatoLibroId", "20")
                )

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"))

                .andExpect(flash().attribute(
                        "mensajeExito",
                        "El artículo se eliminó correctamente."
                ))

                .andExpect(flash().attributeCount(1));

        verify(carritoService).eliminarItem(
                CORREO,
                FORMATO_ID
        );

        verifyNoMoreInteractions(carritoService);
    }

    /**
     * CI-13: El intento de eliminar un artículo inexistente
     * debe presentar un mensaje de error.
     */
    @Test
    void eliminarItem_articuloInexistente_redirigeConMensajeError()
            throws Exception {

        // ARRANGE
        String mensaje =
                "El artículo no existe en el carrito.";

        when(carritoService.eliminarItem(
                CORREO,
                FORMATO_ID
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        mensaje
                )
        );

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/eliminar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("formatoLibroId", "20")
                )

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"))

                .andExpect(flash().attribute(
                        "mensajeError",
                        mensaje
                ))

                .andExpect(flash().attributeCount(1));

        verify(carritoService).eliminarItem(
                CORREO,
                FORMATO_ID
        );

        verifyNoMoreInteractions(carritoService);
    }

    /**
     * CI-14: Una eliminación sin token CSRF válido
     * debe ser rechazada antes de llegar al servicio.
     */
    @Test
    void eliminarItem_sinCsrf_retorna403()
            throws Exception {

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/eliminar")
                                .with(user(CORREO).roles("USER"))
                                .param("formatoLibroId", "20")
                )

                .andExpect(status().isForbidden());

        verifyNoInteractions(carritoService);
    }

    // =====================================================
    // PRUEBAS DE IDENTIDAD DEL CLIENTE
    // =====================================================

    /**
     * CI-15: El controlador debe obtener la identidad
     * del cliente desde el contexto de seguridad.
     *
     * Un correo proporcionado como parámetro del formulario
     * no debe permitir operar sobre el carrito de otro usuario.
     */
    @Test
    void agregarItem_conCorreoManipulado_utilizaIdentidadAutenticada()
            throws Exception {

        // ARRANGE
        String correoAjeno =
                "otro-cliente@raudskinna.test";

        AgregarItemCarritoRequestDTO solicitud =
                new AgregarItemCarritoRequestDTO(
                        FORMATO_ID,
                        1
                );

        when(carritoService.agregarItem(CORREO, solicitud))
                .thenReturn(crearCarritoConArticulo(1));

        // ACT y ASSERT
        mockMvc.perform(
                        post("/carrito/agregar")
                                .with(user(CORREO).roles("USER"))
                                .with(csrf())
                                .param("formatoLibroId", "20")
                                .param("cantidad", "1")

                                // Intento de manipular la identidad.
                                .param("correo", correoAjeno)
                )

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"))

                .andExpect(flash().attribute(
                        "mensajeExito",
                        "El artículo se agregó correctamente al carrito."
                ));

        // Comprobar el uso de la identidad autenticada.
        verify(carritoService).agregarItem(
                CORREO,
                solicitud
        );

        // El correo manipulado nunca debe utilizarse.
        verify(carritoService, never()).agregarItem(
                correoAjeno,
                solicitud
        );

        verifyNoMoreInteractions(carritoService);
    }
}
