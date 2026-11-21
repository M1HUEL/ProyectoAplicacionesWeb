
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.CatalogoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.FormatoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroCatalogoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del controlador MVC del catálogo
 * público de Librería Raudskinna.
 *
 * Verifican que el controlador coordine las consultas
 * bibliográficas y comerciales mediante LibroService,
 * sin incorporar reglas de negocio propias.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    @Mock
    private LibroService libroService;

    @Mock
    private Model model;

    private CatalogoController catalogoController;

    /**
     * Preparar el controlador antes de cada prueba.
     */
    @BeforeEach
    void configurar() {
        catalogoController = new CatalogoController(libroService);
    }

    /**
     * Comprueba que el controlador entregue la información
     * bibliográfica y comercial del libro solicitado.
     *
     * También verifica que se seleccione la plantilla
     * detalle-libro.
     */
    @Test
    void mostrarDetalleLibro_entregaLibroYFormatos() {

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

        // Preparar las presentaciones comerciales.
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
                )
        );

        // Simular las respuestas del servicio.
        when(libroService.consultarLibroPublico(1L))
                .thenReturn(libro);

        when(libroService.listarFormatosPublicos(1L))
                .thenReturn(formatos);

        // ACT: solicitar el detalle público del libro.
        String vista = catalogoController.mostrarDetalleLibro(
                1L,
                model
        );

        // ASSERT: verificar la plantilla seleccionada.
        assertEquals("detalle-libro", vista);

        // Verificar las consultas realizadas al servicio.
        verify(libroService).consultarLibroPublico(1L);
        verify(libroService).listarFormatosPublicos(1L);

        // Verificar los datos entregados a Thymeleaf.
        verify(model).addAttribute("libro", libro);
        verify(model).addAttribute("formatos", formatos);

        // El controlador no debe realizar operaciones adicionales.
        verifyNoMoreInteractions(libroService, model);
    }

    /**
     * Comprueba que el controlador no continúe procesando
     * la consulta cuando el libro solicitado está inactivo
     * o no existe.
     *
     * Debe propagar la excepción generada por LibroService
     * sin consultar las presentaciones comerciales ni
     * proporcionar información al modelo.
     */
    @Test
    void mostrarDetalleLibro_libroInactivo_debePropagar404() {

        // ARRANGE: simular que el libro no está disponible.
        when(libroService.consultarLibroPublico(99L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El libro solicitado no está disponible."
                ));

        // ACT: intentar consultar el detalle del libro.
        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> catalogoController.mostrarDetalleLibro(
                        99L,
                        model
                )
        );

        // ASSERT: comprobar que se conserva el estado HTTP 404.
        assertEquals(
                HttpStatus.NOT_FOUND,
                excepcion.getStatusCode()
        );

        // Confirmar que se realizó la consulta bibliográfica.
        verify(libroService).consultarLibroPublico(99L);

        // No deben consultarse los formatos comerciales.
        verify(libroService, never())
                .listarFormatosPublicos(anyLong());

        // No debe proporcionarse información a Thymeleaf.
        verifyNoInteractions(model);

        verifyNoMoreInteractions(libroService);
    }

    /**
     * Comprueba que el controlador recupere el catálogo
     * público mediante LibroService y entregue los libros
     * junto con su información comercial a Thymeleaf.
     *
     * El controlador no debe calcular precios ni determinar
     * la disponibilidad comercial de los libros.
     */
    @Test
    void mostrarCatalogo_entregaLibrosConInformacionComercial() {

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

        // Preparar la información comercial del libro.
        LibroCatalogoDTO tarjeta = new LibroCatalogoDTO(
                libro,
                new BigDecimal("350.00"),
                true
        );

        // Preparar la colección del catálogo público.
        List<LibroCatalogoDTO> libros = List.of(tarjeta);


        // Preparar las opciones dinámicas de filtrado.
        List<String> categorias = List.of("Terror");
        List<String> autores = List.of("H. P. Lovecraft");

        // Agrupar los resultados y las opciones del catálogo.
        CatalogoPublicoDTO catalogo = new CatalogoPublicoDTO(
                libros,
                categorias,
                autores
        );

        // Simular la respuesta proporcionada por LibroService.
        when(libroService.consultarCatalogoPublico(
                null,
                null,
                null,
                "relevancia"
        )).thenReturn(catalogo);

        // ACT: consultar el catálogo sin criterios bibliográficos.
        String vista = catalogoController.mostrarCatalogo(
                null,
                null,
                null,
                "relevancia",
                model
        );

        // ASSERT: verificar la plantilla seleccionada.
        assertEquals("catalogo", vista);

        // Confirmar la delegación de la consulta a LibroService.
        verify(libroService).consultarCatalogoPublico(
                null,
                null,
                null,
                "relevancia"
        );

        // Verificar los resultados y opciones entregados a Thymeleaf.
        verify(model).addAttribute("libros", libros);
        verify(model).addAttribute("categorias", categorias);
        verify(model).addAttribute("autores", autores);

        // Verificar los criterios conservados en el modelo.
        verify(model).addAttribute("busqueda", null);
        verify(model).addAttribute("categoria", null);
        verify(model).addAttribute("autor", null);
        verify(model).addAttribute("orden", "relevancia");

        // El controlador no debe realizar operaciones adicionales.
        verifyNoMoreInteractions(libroService, model);

    }

}
