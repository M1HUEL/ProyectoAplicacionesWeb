
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LibroPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del controlador de inicio de Librería Raudskinna.
 *
 * Verifican que el controlador recupere los libros públicos
 * y los entregue correctamente a la plantilla Thymeleaf.
 */
@ExtendWith(MockitoExtension.class)
class InicioControllerTest {

    /**
     * Dependencias simuladas para aislar el controlador.
     */
    @Mock
    private LibroService libroService;

    @Mock
    private Model model;

    private InicioController inicioController;

    /**
     * Preparar una instancia del controlador antes de cada prueba.
     */
    @BeforeEach
    void configurar() {
        inicioController = new InicioController(libroService);
    }

    /**
     * Comprueba que la página principal reciba los libros activos
     * y devuelva el nombre de la plantilla correspondiente.
     */
    @Test
    void mostrarInicio_entregaLibrosPublicosYRetornaIndex() {

        // ARRANGE: simular el listado devuelto por el servicio.
        List<LibroPublicoDTO> libros = List.of(
                new LibroPublicoDTO(
                        1L,
                        "Drácula",
                        "Novela de terror",
                        null,
                        "Terror",
                        "Editorial de prueba",
                        List.of("Bram Stoker")
                )
        );

        when(libroService.listarLibrosPublicos())
                .thenReturn(libros);

        // ACT: solicitar al controlador la página principal.
        String vista = inicioController.mostrarInicio(model);

        // ASSERT: verificar la plantilla y los datos proporcionados.
        assertEquals("index", vista);

        verify(libroService).listarLibrosPublicos();
        verify(model).addAttribute("libros", libros);
        verifyNoMoreInteractions(libroService, model);
    }
}
