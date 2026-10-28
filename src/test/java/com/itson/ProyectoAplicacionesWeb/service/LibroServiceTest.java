
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.LibroListadoDTO;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import com.itson.ProyectoAplicacionesWeb.model.ItemStock;
import com.itson.ProyectoAplicacionesWeb.model.Libro;
import com.itson.ProyectoAplicacionesWeb.repository.FormatoLibroRepository;
import com.itson.ProyectoAplicacionesWeb.repository.ItemStockRepository;
import com.itson.ProyectoAplicacionesWeb.repository.LibroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

    /**
     * Comprueba que el listado administrativo devuelve
     * una lista vacía cuando no hay presentaciones
     * comerciales registradas.
     *
     */
    @Test
    void listarParaAdministracion_sinFormatos_debeRetornarListaVacia() {

        // Simula una base de datos sin formatos comerciales.
        when(formatoLibroRepository.listarConLibroYAutores())
                .thenReturn(List.of());

        // Simula que tampoco existen registros de inventario.
        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of());

        // Ejecuta el método que queremos comprobar.
        List<LibroListadoDTO> resultado =
                libroService.listarParaAdministracion();

        // Verifica que el resultado no contiene registros.
        assertTrue(resultado.isEmpty());

        // Comprueba que el servicio consultó ambos repositorios.
        verify(formatoLibroRepository).listarConLibroYAutores();
        verify(itemStockRepository).listarConFormatoLibro();
    }


    /**
     * Comprueba que una presentación física se muestra
     * como disponible cuando sus existencias superan
     * el stock mínimo establecido.
     *
     */
    @Test
    void listarParaAdministracion_conStockSuficiente_debeMostrarDisponible() {

        // Simula los datos bibliográficos del libro.
        Libro libro = mock(Libro.class);
        when(libro.getId()).thenReturn(1L);
        when(libro.getTitulo()).thenReturn("Drácula");
        when(libro.getAutores()).thenReturn(new HashSet<>());

        // Simula una presentación comercial de pasta blanda.
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        when(formatoLibro.getId()).thenReturn(10L);
        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(Formato.PASTA_BLANDA);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("279.00"));

        // Simula un inventario con 12 unidades y mínimo de 3.
        ItemStock inventario = mock(ItemStock.class);
        when(inventario.getFormatoLibro()).thenReturn(formatoLibro);
        when(inventario.getCantidad()).thenReturn(12);
        when(inventario.getStockMinimo()).thenReturn(3);

        // Configura las respuestas de los repositorios simulados.
        when(formatoLibroRepository.listarConLibroYAutores())
                .thenReturn(List.of(formatoLibro));

        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of(inventario));

        // Ejecuta el listado administrativo.
        List<LibroListadoDTO> resultado =
                libroService.listarParaAdministracion();

        // Comprueba que se generó una sola fila.
        assertEquals(1, resultado.size());

        // Comprueba los valores obtenidos en el DTO.
        LibroListadoDTO dto = resultado.get(0);

        assertEquals(1L, dto.libroId());
        assertEquals("Drácula", dto.titulo());
        assertEquals("PASTA_BLANDA", dto.formato());
        assertEquals(new BigDecimal("279.00"), dto.precio());
        assertEquals(12, dto.stock());
        assertEquals("Disponible", dto.estado());
    }

    /**
     * Comprueba que una presentación física se identifica
     * con stock bajo cuando sus existencias son iguales
     * al mínimo establecido.
     *
     */
    @Test
    void listarParaAdministracion_stockIgualAlMinimo_debeMostrarStockBajo() {

        // Simula un libro con sus datos bibliográficos.
        Libro libro = mock(Libro.class);
        when(libro.getId()).thenReturn(2L);
        when(libro.getTitulo()).thenReturn("El Hobbit");
        when(libro.getAutores()).thenReturn(new HashSet<>());

        // Simula la presentación comercial del libro.
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        when(formatoLibro.getId()).thenReturn(20L);
        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(Formato.PASTA_DURA);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("399.00"));

        // Existen 3 unidades y el mínimo también es 3.
        ItemStock inventario = mock(ItemStock.class);
        when(inventario.getFormatoLibro()).thenReturn(formatoLibro);
        when(inventario.getCantidad()).thenReturn(3);
        when(inventario.getStockMinimo()).thenReturn(3);

        // Configura los repositorios simulados.
        when(formatoLibroRepository.listarConLibroYAutores())
                .thenReturn(List.of(formatoLibro));

        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of(inventario));

        // Ejecuta el método del servicio.
        List<LibroListadoDTO> resultado =
                libroService.listarParaAdministracion();

        // Comprueba que se generó una fila.
        assertEquals(1, resultado.size());

        // Comprueba las existencias y el estado calculado.
        LibroListadoDTO dto = resultado.get(0);

        assertEquals(3, dto.stock());
        assertEquals("Stock bajo", dto.estado());
    }

    /**
     * Comprueba que una presentación física sin existencias
     * se identifica con el estado "Sin stock".
     *
     */
    @Test
    void listarParaAdministracion_stockCero_debeMostrarSinStock() {

        // Simula los datos bibliográficos del libro.
        Libro libro = mock(Libro.class);
        when(libro.getId()).thenReturn(3L);
        when(libro.getTitulo()).thenReturn("1984");
        when(libro.getAutores()).thenReturn(new HashSet<>());

        // Simula una presentación de pasta blanda.
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        when(formatoLibro.getId()).thenReturn(30L);
        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(Formato.PASTA_BLANDA);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("249.00"));

        // El inventario tiene cero existencias.
        ItemStock inventario = mock(ItemStock.class);
        when(inventario.getFormatoLibro()).thenReturn(formatoLibro);
        when(inventario.getCantidad()).thenReturn(0);

        // Configura las respuestas de los repositorios.
        when(formatoLibroRepository.listarConLibroYAutores())
                .thenReturn(List.of(formatoLibro));

        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of(inventario));

        // Ejecuta el método del servicio.
        List<LibroListadoDTO> resultado =
                libroService.listarParaAdministracion();

        // Comprueba el resultado del listado.
        assertEquals(1, resultado.size());

        LibroListadoDTO dto = resultado.get(0);

        assertEquals(0, dto.stock());
        assertEquals("Sin stock", dto.estado());
    }

    /**
     * Comprueba que una presentación digital no requiere
     * inventario físico y muestra el estado "No aplica".
     *
     * Lafardilla 2026.
     */
    @Test
    void listarParaAdministracion_formatoDigital_debeMostrarNoAplica() {

        // Simula los datos bibliográficos del libro.
        Libro libro = mock(Libro.class);
        when(libro.getId()).thenReturn(4L);
        when(libro.getTitulo()).thenReturn("Frankenstein");
        when(libro.getAutores()).thenReturn(new HashSet<>());

        // Simula una presentación comercial digital.
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        when(formatoLibro.getId()).thenReturn(40L);
        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(Formato.DIGITAL);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("149.00"));

        // Existe la presentación digital, pero no hay inventario físico.
        when(formatoLibroRepository.listarConLibroYAutores())
                .thenReturn(List.of(formatoLibro));

        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of());

        // Ejecuta el método del servicio.
        List<LibroListadoDTO> resultado =
                libroService.listarParaAdministracion();

        // Verifica que se generó una presentación comercial.
        assertEquals(1, resultado.size());

        LibroListadoDTO dto = resultado.get(0);

        // Comprueba el formato, precio y ausencia de stock físico.
        assertEquals("DIGITAL", dto.formato());
        assertEquals(new BigDecimal("149.00"), dto.precio());
        assertNull(dto.stock());
        assertEquals("No aplica", dto.estado());
    }


    /**
     * Comprueba que una presentación física sin registro
     * de inventario muestra el estado "Sin registro".
     *
     * Esto permite distinguir entre un inventario
     * inexistente y uno con cero unidades disponibles.
     *
     */
    @Test
    void listarParaAdministracion_sinRegistroInventario_debeMostrarSinRegistro() {

        // Simula la información bibliográfica del libro.
        Libro libro = mock(Libro.class);
        when(libro.getId()).thenReturn(5L);
        when(libro.getTitulo()).thenReturn("Fundación");
        when(libro.getAutores()).thenReturn(new HashSet<>());

        // Simula una presentación física de pasta dura.
        FormatoLibro formatoLibro = mock(FormatoLibro.class);
        when(formatoLibro.getId()).thenReturn(50L);
        when(formatoLibro.getLibro()).thenReturn(libro);
        when(formatoLibro.getFormato()).thenReturn(Formato.PASTA_DURA);
        when(formatoLibro.getPrecio())
                .thenReturn(new BigDecimal("429.00"));

        // Existe la presentación, pero no su registro de inventario.
        when(formatoLibroRepository.listarConLibroYAutores())
                .thenReturn(List.of(formatoLibro));

        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of());

        // Ejecuta la operación del servicio.
        List<LibroListadoDTO> resultado =
                libroService.listarParaAdministracion();

        // Comprueba que se generó una fila.
        assertEquals(1, resultado.size());

        LibroListadoDTO dto = resultado.get(0);

        // La presentación existe, pero sus existencias son desconocidas.
        assertEquals("PASTA_DURA", dto.formato());
        assertNull(dto.stock());
        assertEquals("Sin registro", dto.estado());
    }


}
