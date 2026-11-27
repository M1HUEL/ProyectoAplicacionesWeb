package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.DescargaDigitalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas del endpoint REST de descarga digital.
 */
class DescargaDigitalRestControllerTest {

    @Mock
    private DescargaDigitalService descargaDigitalService;

    private DescargaDigitalRestController controlador;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        controlador = new DescargaDigitalRestController(descargaDigitalService);
    }

    @Test
    void descargar_usaElCorreoDelJwtYDevuelvePdf() {

        Authentication autenticacion = mock(Authentication.class);
        when(autenticacion.getName()).thenReturn("ana@raudskinna.com");

        byte[] pdf = new byte[] { 1, 2, 3 };
        when(descargaDigitalService.descargar(
                "ana@raudskinna.com",
                10L,
                20L
        )).thenReturn(pdf);

        ResponseEntity<byte[]> respuesta = controlador.descargar(
                10L,
                20L,
                autenticacion
        );

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, respuesta.getHeaders().getContentType());
        assertEquals(
                "attachment; filename=\"raudskinna-muestra-digital.pdf\"",
                respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
        assertArrayEquals(pdf, respuesta.getBody());

        verify(descargaDigitalService).descargar(
                "ana@raudskinna.com",
                10L,
                20L
        );
        verifyNoMoreInteractions(descargaDigitalService);
    }
}
