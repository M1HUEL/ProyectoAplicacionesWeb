
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.DireccionRequestDTO;
import com.itson.ProyectoAplicacionesWeb.exception.ApiExceptionHandler;
import com.itson.ProyectoAplicacionesWeb.model.Direccion;
import com.itson.ProyectoAplicacionesWeb.service.DireccionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas HTTP del controlador REST de direcciones.
 *
 * Verifican que la identidad del cliente provenga del JWT y que la
 * entidad JPA nunca se exponga directamente en la respuesta.
 */
@ExtendWith(MockitoExtension.class)
class DireccionRestControllerTest {

    private static final String CORREO = "cliente@raudskinna.com";
    private static final Long DIRECCION_ID = 7L;

    private static final String JSON_DIRECCION = """
            {
              "nombreDestinatario": "Ana López",
              "calle": "Reforma",
              "numeroExterior": "12",
              "numeroInterior": "A",
              "colonia": "Centro",
              "codigoPostal": "83000",
              "ciudad": "Hermosillo",
              "estado": "Sonora",
              "pais": "México",
              "referencias": "Portón negro"
            }
            """;

    @Mock
    private DireccionService direccionService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {

        DireccionRestController controlador =
                new DireccionRestController(direccionService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controlador)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private Authentication autenticacion() {

        Authentication autenticacion = mock(Authentication.class);
        when(autenticacion.getName()).thenReturn(CORREO);

        return autenticacion;
    }

    private Direccion direccionDePrueba() {

        Direccion direccion = new Direccion(null);
        ReflectionTestUtils.setField(direccion, "id", DIRECCION_ID);

        direccion.setNombreDestinatario("Ana López");
        direccion.setCalle("Reforma");
        direccion.setNumeroExterior("12");
        direccion.setNumeroInterior("A");
        direccion.setColonia("Centro");
        direccion.setCodigoPostal("83000");
        direccion.setCiudad("Hermosillo");
        direccion.setEstado("Sonora");
        direccion.setPais("México");
        direccion.setReferencias("Portón negro");

        return direccion;
    }

    @Test
    void listar_devuelveDireccionesDelClienteAutenticado()
            throws Exception {

        when(direccionService.listarPorUsuario(CORREO))
                .thenReturn(List.of(direccionDePrueba()));

        mockMvc.perform(
                        get("/api/direcciones")
                                .principal(autenticacion())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(DIRECCION_ID))
                .andExpect(jsonPath("$[0].nombreDestinatario")
                        .value("Ana López"))
                .andExpect(jsonPath("$[0].codigoPostal")
                        .value("83000"))
                .andExpect(jsonPath("$[0].usuario").doesNotExist());

        verify(direccionService).listarPorUsuario(CORREO);
        verifyNoMoreInteractions(direccionService);
    }

    @Test
    void consultar_devuelveUnaDireccionPropia() throws Exception {

        when(direccionService.buscarPropia(CORREO, DIRECCION_ID))
                .thenReturn(direccionDePrueba());

        mockMvc.perform(
                        get("/api/direcciones/{id}", DIRECCION_ID)
                                .principal(autenticacion())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DIRECCION_ID))
                .andExpect(jsonPath("$.calle").value("Reforma"))
                .andExpect(jsonPath("$.numeroInterior").value("A"))
                .andExpect(jsonPath("$.referencias")
                        .value("Portón negro"))
                .andExpect(jsonPath("$.usuario").doesNotExist());

        verify(direccionService).buscarPropia(CORREO, DIRECCION_ID);
        verifyNoMoreInteractions(direccionService);
    }

    @Test
    void consultar_direccionAjena_retorna400() throws Exception {

        when(direccionService.buscarPropia(CORREO, DIRECCION_ID))
                .thenThrow(new IllegalArgumentException(
                        "La dirección no existe o no pertenece al cliente."
                ));

        mockMvc.perform(
                        get("/api/direcciones/{id}", DIRECCION_ID)
                                .principal(autenticacion())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value(
                        "La dirección no existe o no pertenece al cliente."
                ))
                .andExpect(jsonPath("$.ruta")
                        .value("/api/direcciones/" + DIRECCION_ID));

        verify(direccionService).buscarPropia(CORREO, DIRECCION_ID);
        verifyNoMoreInteractions(direccionService);
    }

    @Test
    void registrar_creaDireccionYDevuelve201ConLocation()
            throws Exception {

        Authentication autenticacion = autenticacion();

        when(direccionService.registrar(
                eq(CORREO),
                any(DireccionRequestDTO.class)
        )).thenReturn(direccionDePrueba());

        mockMvc.perform(
                        post("/api/direcciones")
                                .principal(autenticacion)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON_DIRECCION)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/direcciones/" + DIRECCION_ID
                ))
                .andExpect(jsonPath("$.id").value(DIRECCION_ID))
                .andExpect(jsonPath("$.nombreDestinatario")
                        .value("Ana López"))
                .andExpect(jsonPath("$.usuario").doesNotExist());

        ArgumentCaptor<DireccionRequestDTO> captor =
                ArgumentCaptor.forClass(DireccionRequestDTO.class);

        verify(direccionService).registrar(eq(CORREO), captor.capture());
        verifyNoMoreInteractions(direccionService);

        DireccionRequestDTO solicitud = captor.getValue();
        assertEquals("Ana López", solicitud.nombreDestinatario());
        assertEquals("83000", solicitud.codigoPostal());
        assertEquals("Portón negro", solicitud.referencias());
    }

    @Test
    void registrar_datosInvalidos_retorna400() throws Exception {

        when(direccionService.registrar(
                eq(CORREO),
                any(DireccionRequestDTO.class)
        )).thenThrow(new IllegalArgumentException(
                "El código postal debe contener 5 dígitos."
        ));

        mockMvc.perform(
                        post("/api/direcciones")
                                .principal(autenticacion())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON_DIRECCION)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El código postal debe contener 5 dígitos."
                ));
    }

    @Test
    void registrar_sinCuerpo_retorna400DelServicio() throws Exception {

        when(direccionService.registrar(CORREO, null))
                .thenThrow(new IllegalArgumentException(
                        "Los datos de la dirección son obligatorios."
                ));

        mockMvc.perform(
                        post("/api/direcciones")
                                .principal(autenticacion())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "Los datos de la dirección son obligatorios."
                ));

        verify(direccionService).registrar(CORREO, null);
    }

    @Test
    void actualizar_modificaDireccionPropia() throws Exception {

        when(direccionService.actualizar(
                eq(CORREO),
                eq(DIRECCION_ID),
                any(DireccionRequestDTO.class)
        )).thenReturn(direccionDePrueba());

        mockMvc.perform(
                        put("/api/direcciones/{id}", DIRECCION_ID)
                                .principal(autenticacion())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON_DIRECCION)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DIRECCION_ID))
                .andExpect(jsonPath("$.ciudad").value("Hermosillo"))
                .andExpect(jsonPath("$.usuario").doesNotExist());

        verify(direccionService).actualizar(
                eq(CORREO),
                eq(DIRECCION_ID),
                any(DireccionRequestDTO.class)
        );
        verifyNoMoreInteractions(direccionService);
    }

    @Test
    void eliminar_devuelve204SinContenido() throws Exception {

        mockMvc.perform(
                        delete("/api/direcciones/{id}", DIRECCION_ID)
                                .principal(autenticacion())
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(direccionService).eliminar(CORREO, DIRECCION_ID);
        verifyNoMoreInteractions(direccionService);
    }
}
