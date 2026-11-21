
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.FormatoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración MVC del detalle público
 * de libros de Librería Raudskinna.
 *
 * Verifican el acceso anónimo mediante Spring Security
 * y el renderizado de la plantilla Thymeleaf.
 *
 * Utilizan el perfil integrationtest con H2 y simulan
 * LibroService para controlar los datos comerciales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class CatalogoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibroService libroService;

    /**
     * Comprueba que un visitante sin sesión pueda
     * consultar la información bibliográfica y comercial
     * de un libro activo.
     *
     * La vista debe mostrar los formatos, sus precios
     * y la disponibilidad correspondiente.
     */
    @Test
    void mostrarDetalleLibro_sinSesion_renderizaInformacionComercial()
            throws Exception {

        // ARRANGE: preparar la información bibliográfica.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                1L,
                "La llamada de Cthulhu",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("H. P. Lovecraft")
        );

        // Preparar tres presentaciones comerciales.
        List<FormatoPublicoDTO> formatos = List.of(
                new FormatoPublicoDTO(
                        10L,
                        Formato.PASTA_DURA,
                        new BigDecimal("350.00"),
                        true
                ),
                new FormatoPublicoDTO(
                        20L,
                        Formato.PASTA_BLANDA,
                        new BigDecimal("250.00"),
                        false
                ),
                new FormatoPublicoDTO(
                        30L,
                        Formato.DIGITAL,
                        new BigDecimal("149.00"),
                        true
                )
        );

        // Simular las respuestas de LibroService.
        when(libroService.consultarLibroPublico(1L))
                .thenReturn(libro);

        when(libroService.listarFormatosPublicos(1L))
                .thenReturn(formatos);

        // ACT y ASSERT: solicitar el detalle sin autenticación.
        mockMvc.perform(get("/detalle-libro/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("detalle-libro"))

                // Información bibliográfica.
                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("H. P. Lovecraft")
                ))
                .andExpect(content().string(
                        containsString("Relato de terror cósmico.")
                ))
                .andExpect(content().string(
                        containsString("Editorial de prueba")
                ))

                // Presentaciones comerciales.
                .andExpect(content().string(
                        containsString("Pasta dura")
                ))
                .andExpect(content().string(
                        containsString("Pasta blanda")
                ))
                .andExpect(content().string(
                        containsString("Digital")
                ))

                // Precios con formato monetario mexicano.
                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ))
                .andExpect(content().string(
                        containsString("$250.00 MXN")
                ))
                .andExpect(content().string(
                        containsString("$149.00 MXN")
                ))

                // Estados de disponibilidad.
                .andExpect(content().string(
                        containsString("Disponible")
                ))
                .andExpect(content().string(
                        containsString("Agotado")
                ));

        // Confirmar que la solicitud llegó al servicio.
        verify(libroService).consultarLibroPublico(1L);
        verify(libroService).listarFormatosPublicos(1L);
    }

    /**
     * Comprueba que un visitante no pueda consultar
     * el detalle de un libro inactivo o inexistente.
     *
     * La aplicación debe responder con HTTP 404
     * sin recuperar sus presentaciones comerciales.
     */
    @Test
    void mostrarDetalleLibro_libroInactivo_retorna404()
            throws Exception {

        // ARRANGE: simular un libro no disponible públicamente.
        when(libroService.consultarLibroPublico(99L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El libro solicitado no está disponible."
                ));

        // ACT y ASSERT: solicitar el detalle sin autenticación.
        mockMvc.perform(get("/detalle-libro/99"))
                .andExpect(status().isNotFound());

        // Confirmar que se realizó la consulta bibliográfica.
        verify(libroService).consultarLibroPublico(99L);

        // No deben consultarse las presentaciones comerciales.
        verify(libroService, never())
                .listarFormatosPublicos(anyLong());

        verifyNoMoreInteractions(libroService);
    }

    /**
     * Comprueba que un libro activo sin presentaciones
     * comerciales pueda consultarse públicamente.
     *
     * La vista debe mostrar su información bibliográfica
     * e informar que todavía no existen formatos registrados,
     * sin presentar precios ni disponibilidad ficticios.
     */
    @Test
    void mostrarDetalleLibro_sinFormatos_muestraMensajeInformativo()
            throws Exception {

        // ARRANGE: preparar un libro activo.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                2L,
                "En las montañas de la locura",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("H. P. Lovecraft")
        );

        when(libroService.consultarLibroPublico(2L))
                .thenReturn(libro);

        // El libro todavía no tiene presentaciones comerciales.
        when(libroService.listarFormatosPublicos(2L))
                .thenReturn(List.of());

        // ACT y ASSERT: consultar el detalle sin autenticación.
        mockMvc.perform(get("/detalle-libro/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("detalle-libro"))

                // La información bibliográfica debe permanecer visible.
                .andExpect(content().string(
                        containsString("En las montañas de la locura")
                ))
                .andExpect(content().string(
                        containsString("H. P. Lovecraft")
                ))

                // Informar la ausencia de presentaciones comerciales.
                .andExpect(content().string(
                        containsString(
                                "Este libro todavía no tiene "
                                        + "presentaciones comerciales registradas."
                        )
                ))

                // No deben mostrarse formatos ni precios ficticios.
                .andExpect(content().string(
                        not(containsString("class=\"formato-libro\""))
                ))
                .andExpect(content().string(
                        not(containsString("class=\"formato-disponible\""))
                ));

        // Confirmar las consultas realizadas al servicio.
        verify(libroService).consultarLibroPublico(2L);
        verify(libroService).listarFormatosPublicos(2L);

        verifyNoMoreInteractions(libroService);
    }

}
