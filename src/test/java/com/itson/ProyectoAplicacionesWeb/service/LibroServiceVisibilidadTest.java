package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.LibroFormularioDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class LibroServiceVisibilidadTest {
    @Mock
    private LibroRepository libroRepository;

    @Mock
    private FormatoLibroRepository formatoLibroRepository;

    @Mock
    private ItemStockRepository itemStockRepository;

    @Mock
    private AutorRepository autorRepository;

    @Mock
    private EditorialRepository editorialRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    private LibroService servicio;

    private Categoria categoria;
    private Editorial editorial;
    private Autor autor;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);

        servicio = new LibroService(
                libroRepository,
                formatoLibroRepository,
                itemStockRepository,
                autorRepository,
                editorialRepository,
                categoriaRepository
        );

        categoria = new Categoria("Terror");
        editorial = new Editorial("Penguin", null);
        autor = new Autor("Bram Stoker");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(editorialRepository.findById(1L)).thenReturn(Optional.of(editorial));
        when(autorRepository.findAllById(Set.of(1L))).thenReturn(List.of(autor));
        when(libroRepository.save(any(Libro.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(formatoLibroRepository.save(any(FormatoLibro.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private LibroFormularioDTO formularioDigital(boolean activo) {
        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setFormato(Formato.DIGITAL);
        formulario.setPrecio(new BigDecimal("99.00"));
        formulario.setActivo(activo);
        return formulario;
    }

    @Test
    void registrar_porDefectoElLibroQuedaVisible() {

        Libro libro = servicio.registrar(formularioDigital(true));

        assertTrue(libro.isActivo());
    }

    @Test
    void registrar_conActivoFalso_guardaElLibroOculto() {

        Libro libro = servicio.registrar(formularioDigital(false));

        assertFalse(libro.isActivo());
    }

    @Test
    void cambiarVisibilidad_ocultaYMuestraSinEliminar() {

        Libro libro = new Libro("Drácula", "9781234567897", categoria, editorial);
        ReflectionTestUtils.setField(libro, "id", 1L);

        when(libroRepository.findById(1L)).thenReturn(Optional.of(libro));

        assertFalse(servicio.cambiarVisibilidad(1L, false).isActivo());
        assertTrue(servicio.cambiarVisibilidad(1L, true).isActivo());
    }

    @Test
    void cambiarVisibilidad_conLibroInexistente_debeRechazar() {

        when(libroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarVisibilidad(99L, false));
    }

    @Test
    void prepararFormularioEdicion_reflejaLaVisibilidadDelLibro() {

        Libro libro = new Libro("Drácula", "9781234567897", categoria, editorial);
        ReflectionTestUtils.setField(libro, "id", 1L);
        ReflectionTestUtils.setField(categoria, "id", 1L);
        ReflectionTestUtils.setField(editorial, "id", 1L);
        libro.setActivo(false);

        when(libroRepository.findById(1L)).thenReturn(Optional.of(libro));

        assertFalse(servicio.prepararFormularioEdicion(1L).isActivo());
    }
}