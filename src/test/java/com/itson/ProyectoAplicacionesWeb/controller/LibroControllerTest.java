
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LibroFormularioDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del controlador administrativo de libros.
 *
 * Comprueban la preparación de los datos enviados a Thymeleaf,
 * sin acceder a la base de datos ni iniciar un servidor HTTP.
 */
@ExtendWith(MockitoExtension.class)
class LibroControllerTest {

    @Mock
    private LibroService libroService;

    @Mock
    private AutorService autorService;

    @Mock
    private EditorialService editorialService;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private PortadaService portadaService;

    @Mock
    private Model model;

    private LibroController libroController;

    /**
     * Construye el controlador con sus dependencias simuladas.
     */
    @BeforeEach
    void configurar() {
        libroController = new LibroController(
                libroService,
                autorService,
                editorialService,
                categoriaService,
                portadaService
        );
    }

    /**
     * Comprueba que el formulario de edición incluya la categoría
     * original del libro, aunque se encuentre desactivada.
     *
     * Las demás categorías disponibles deben permanecer activas.
     */
    @Test
    void mostrarFormularioEdicion_conCategoriaOriginalInactiva_debeIncluirla()
    {

        // ARRANGE: categoría original desactivada.
        Categoria categoriaOriginal = new Categoria("Terror");
        categoriaOriginal.setActivo(false);
        ReflectionTestUtils.setField(categoriaOriginal, "id", 1L);

        // Categoría activa que puede seleccionarse como alternativa.
        Categoria categoriaActiva = new Categoria("Literatura clásica");
        ReflectionTestUtils.setField(categoriaActiva, "id", 2L);

        Editorial editorial = new Editorial(
                "Raudskinna - Edición de prueba",
                null
        );

        Libro libro = new Libro(
                "La llamada de Cthulhu",
                "TEST-RLYEH-001",
                categoriaOriginal,
                editorial
        );

        ReflectionTestUtils.setField(libro, "id", 1L);

        // Presentación digital para evitar preparar inventario físico.
        FormatoLibro formato = new FormatoLibro(
                libro,
                Formato.DIGITAL,
                new BigDecimal("149.00")
        );

        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setCategoriaId(1L);

        // Simular las consultas del controlador.
        when(libroService.buscarFormatoDeLibro(1L, 2L))
                .thenReturn(formato);

        when(libroService.prepararFormularioEdicion(1L))
                .thenReturn(formulario);

        when(categoriaService.listarActivas())
                .thenReturn(List.of(categoriaActiva));

        when(categoriaService.buscarPorId(1L))
                .thenReturn(categoriaOriginal);

        when(autorService.listarTodos())
                .thenReturn(List.of());

        when(editorialService.listarTodas())
                .thenReturn(List.of(editorial));

        // ACT: preparar el formulario administrativo de edición.
        String vista = libroController.mostrarFormularioEdicion(
                1L,
                2L,
                model
        );

        // ASSERT: comprobar que se conserva la plantilla.
        assertEquals("administrador-libros-formulario", vista);

        // Capturar las categorías enviadas a Thymeleaf.
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List> captor =
                ArgumentCaptor.forClass(List.class);

        verify(model).addAttribute(
                eq("categorias"),
                captor.capture()
        );

        List<?> categorias = captor.getValue();

        // La categoría original inactiva debe estar disponible.
        assertTrue(categorias.contains(categoriaOriginal));

        // También debe estar la categoría activa.
        assertTrue(categorias.contains(categoriaActiva));

        // No deben aparecer categorías duplicadas.
        assertEquals(2, categorias.size());

        // El formulario debe conservar la categoría seleccionada.
        assertEquals(1L, formulario.getCategoriaId());

        verify(model).addAttribute(
                "libroFormulario",
                formulario
        );
    }

    /**
     * Comprueba que el formulario de registro de libros nuevos
     * solamente proporcione categorías activas.
     *
     * La excepción que permite conservar una categoría inactiva
     * corresponde exclusivamente a la edición de libros existentes.
     */
    @Test
    void mostrarFormularioNuevo_debeMostrarSoloCategoriasActivas() {

        // ARRANGE: categoría activa disponible para nuevos libros.
        Categoria categoriaActiva = new Categoria("Literatura clásica");
        ReflectionTestUtils.setField(categoriaActiva, "id", 2L);

        when(categoriaService.listarActivas())
                .thenReturn(List.of(categoriaActiva));

        // ACT: solicitar el formulario de registro de libros.
        String vista = libroController.mostrarFormularioNuevo(model);

        // ASSERT: verificar la plantilla utilizada.
        assertEquals("administrador-libros-formulario", vista);

        // Capturar las categorías proporcionadas a Thymeleaf.
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List> captor =
                ArgumentCaptor.forClass(List.class);

        verify(model).addAttribute(
                eq("categorias"),
                captor.capture()
        );

        List<?> categorias = captor.getValue();

        // Solamente debe mostrarse la categoría activa.
        assertEquals(1, categorias.size());
        assertTrue(categorias.contains(categoriaActiva));

        // La creación no debe recuperar categorías inactivas.
        verify(categoriaService).listarActivas();
        verify(categoriaService, never()).listarTodas();
        verify(categoriaService, never()).buscarPorId(anyLong());
    }

}
