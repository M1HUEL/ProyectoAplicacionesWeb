package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.CatalogoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas del REST controller del catálogo con filtros completos.
 */
class CatalogoRestControllerTest {

    @Mock
    private LibroService libroService;

    private CatalogoRestController controlador;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        controlador = new CatalogoRestController(libroService);
    }

    @Test
    void consultar_enviaTodosLosFiltrosAlServicio() {

        CatalogoPublicoDTO esperado =
                new CatalogoPublicoDTO(List.of(), List.of(), List.of());

        when(libroService.consultarCatalogoPublico(
                "dune",
                "Ciencia ficción",
                "Frank Herbert",
                "precio-asc"
        )).thenReturn(esperado);

        CatalogoPublicoDTO respuesta = controlador.consultar(
                "dune",
                null,
                "Ciencia ficción",
                "Frank Herbert",
                "precio-asc"
        );

        assertSame(esperado, respuesta);
        verify(libroService).consultarCatalogoPublico(
                "dune",
                "Ciencia ficción",
                "Frank Herbert",
                "precio-asc"
        );
        verifyNoMoreInteractions(libroService);
    }

    @Test
    void consultar_aceptaBusquedaComoAliasDeQ() {

        CatalogoPublicoDTO esperado =
                new CatalogoPublicoDTO(List.of(), List.of(), List.of());

        when(libroService.consultarCatalogoPublico(
                "hobbit",
                null,
                null,
                "relevancia"
        )).thenReturn(esperado);

        CatalogoPublicoDTO respuesta = controlador.consultar(
                null,
                "hobbit",
                null,
                null,
                "relevancia"
        );

        assertSame(esperado, respuesta);
        verify(libroService).consultarCatalogoPublico(
                "hobbit",
                null,
                null,
                "relevancia"
        );
        verifyNoMoreInteractions(libroService);
    }
}
