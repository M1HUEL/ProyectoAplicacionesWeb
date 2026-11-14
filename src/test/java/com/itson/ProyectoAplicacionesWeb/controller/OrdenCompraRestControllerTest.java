
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.OrdenCompraResponseDTO;
import com.itson.ProyectoAplicacionesWeb.service.OrdenCompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas del endpoint de historial de compras.
 *
 * Verifican que la identidad utilizada para consultar las órdenes
 * proviene exclusivamente del JWT y no de datos enviados por el cliente.
 */
class OrdenCompraRestControllerTest {

    @Mock
    private OrdenCompraService ordenCompraService;

    private OrdenCompraRestController controlador;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        controlador = new OrdenCompraRestController(ordenCompraService);
    }

    @Test
    void consultarHistorial_usaElCorreoDelJwt() {

        Authentication autenticacion = mock(Authentication.class);
        when(autenticacion.getName()).thenReturn("ana@raudskinna.com");

        List<OrdenCompraResponseDTO> esperado = List.of();

        when(ordenCompraService.consultarHistorial("ana@raudskinna.com"))
                .thenReturn(esperado);

        ResponseEntity<List<OrdenCompraResponseDTO>> respuesta =
                controlador.consultarHistorial(autenticacion);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(esperado, respuesta.getBody());

        // El único criterio de búsqueda es el correo del token.
        verify(ordenCompraService)
                .consultarHistorial("ana@raudskinna.com");

        verifyNoMoreInteractions(ordenCompraService);
    }

    @Test
    void consultarHistorial_dosUsuarios_consultaCorreosDistintos() {

        Authentication ana = mock(Authentication.class);
        when(ana.getName()).thenReturn("ana@raudskinna.com");

        Authentication luis = mock(Authentication.class);
        when(luis.getName()).thenReturn("luis@raudskinna.com");

        when(ordenCompraService.consultarHistorial("ana@raudskinna.com"))
                .thenReturn(List.of());

        when(ordenCompraService.consultarHistorial("luis@raudskinna.com"))
                .thenReturn(List.of());

        controlador.consultarHistorial(ana);
        controlador.consultarHistorial(luis);

        verify(ordenCompraService)
                .consultarHistorial("ana@raudskinna.com");

        verify(ordenCompraService)
                .consultarHistorial("luis@raudskinna.com");

        verifyNoMoreInteractions(ordenCompraService);
    }
}
