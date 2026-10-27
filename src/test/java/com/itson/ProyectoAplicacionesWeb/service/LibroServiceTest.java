
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Libro;
import com.itson.ProyectoAplicacionesWeb.repository.FormatoLibroRepository;
import com.itson.ProyectoAplicacionesWeb.repository.ItemStockRepository;
import com.itson.ProyectoAplicacionesWeb.repository.LibroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para LibroService.
 *
 * Utiliza Mockito para simular el repositorio
 * sin conectarse a una base de datos real.
 *

 */
class LibroServiceTest {

    /** Repositorio simulado mediante Mockito. */
    @Mock
    private LibroRepository libroRepository;

    @Mock
    private FormatoLibroRepository formatoLibroRepository;

    @Mock
    private ItemStockRepository itemStockRepository;

    /** Servicio que se someterá a las pruebas. */
    private LibroService libroService;

    /**
     * Prepara las dependencias antes de cada prueba.
     */
    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        libroService = new LibroService(
                libroRepository,
                formatoLibroRepository,
                itemStockRepository
        );
    }

    /**
     * Comprueba que el servicio recupera los libros
     * proporcionados por el repositorio.
     */
    @Test
    void listarTodos_debeRetornarLosLibrosRegistrados() {

        // Se crean dos libros simulados para la prueba.
        Libro libro1 = mock(Libro.class);
        Libro libro2 = mock(Libro.class);

        // Se define lo que devolverá el repositorio simulado.
        when(libroRepository.findAll())
                .thenReturn(List.of(libro1, libro2));

        // Se ejecuta el método que queremos comprobar.
        List<Libro> resultado = libroService.listarTodos();

        // Se comprueba que se recuperaron los dos libros.
        assertEquals(2, resultado.size());

        // Se comprueba que el servicio consultó al repositorio.
        verify(libroRepository).findAll();
    }
}
