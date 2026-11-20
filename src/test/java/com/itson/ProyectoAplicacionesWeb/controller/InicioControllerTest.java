
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LibroCatalogoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.math.BigDecimal;
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
     * con su resumen comercial y devuelva la plantilla correspondiente.
     */
    @Test
    void mostrarInicio_entregaLibrosPublicosYRetornaIndex() {

        // ARRANGE: preparar la información bibliográfica.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                1L,
                "Drácula",
                "Novela de terror",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("Bram Stoker")
        );

        // Preparar la tarjeta con su precio inicial y disponibilidad.
        List<LibroCatalogoDTO> libros = List.of(
                new LibroCatalogoDTO(
                        libro,
                        new BigDecimal("249.00"),
                        true
                )
        );

        // Simular la respuesta del nuevo servicio comercial.
        when(libroService.listarCatalogoPublico())
                .thenReturn(libros);

        // ACT: solicitar al controlador la página principal.
        String vista = inicioController.mostrarInicio(model);

        // ASSERT: verificar la plantilla y los datos proporcionados.
        assertEquals("index", vista);

        verify(libroService).listarCatalogoPublico();
        verify(model).addAttribute("libros", libros);
        verifyNoMoreInteractions(libroService, model);
    }

}
