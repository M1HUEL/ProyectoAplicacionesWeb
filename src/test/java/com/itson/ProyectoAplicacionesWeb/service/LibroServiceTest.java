
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.LibroFormularioDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroListadoDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Mock
    private AutorRepository autorRepository;

    @Mock
    private EditorialRepository editorialRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

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
                itemStockRepository,
                autorRepository,
                editorialRepository,
                categoriaRepository
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

    /**
     * Comprueba el registro de un libro físico.
     *
     * Debe guardar el libro, su formato comercial
     * y las existencias iniciales.
     *
     * Los repositorios se simulan mediante Mockito.
     *
     */
    @Test
    void registrar_conLibroFisicoValido_debeGuardarLibroFormatoYStock() {

        // Prepara los datos del formulario.
        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setFormato(Formato.PASTA_DURA);
        formulario.setPrecio(new BigDecimal("399.00"));
        formulario.setCantidad(12);
        formulario.setStockMinimo(3);

        // Prepara las entidades relacionadas.
        Categoria categoria = new Categoria("Literatura clásica");
        categoria.setActivo(true);

        Editorial editorial =
                new Editorial("Penguin Random House", null);

        Autor autor = new Autor("Bram Stoker");

        // Simula las consultas necesarias para el registro.
        when(libroRepository.existsByIsbn("9781234567897"))
                .thenReturn(false);

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(editorialRepository.findById(1L))
                .thenReturn(Optional.of(editorial));

        when(autorRepository.findAllById(Set.of(1L)))
                .thenReturn(List.of(autor));

        // Devuelve las entidades recibidas al simular el guardado.
        when(libroRepository.save(any(Libro.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        when(formatoLibroRepository.save(any(FormatoLibro.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Ejecuta la operación.
        Libro resultado = libroService.registrar(formulario);

        // Comprueba los datos bibliográficos registrados.
        assertEquals("Drácula", resultado.getTitulo());
        assertEquals("9781234567897", resultado.getIsbn());
        assertTrue(resultado.getAutores().contains(autor));

        // Comprueba las tres operaciones de persistencia.
        verify(libroRepository).save(any(Libro.class));
        verify(formatoLibroRepository).save(any(FormatoLibro.class));
        verify(itemStockRepository).save(any(ItemStock.class));
    }

    /**
     * Comprueba que un libro digital se registre
     * sin crear un inventario físico.
     *
     * Deben guardarse Libro y FormatoLibro,
     * pero no ItemStock.
     */
    @Test
    void registrar_conLibroDigitalValido_noDebeGuardarStock() {

        // Prepara un formulario con formato digital.
        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setFormato(Formato.DIGITAL);
        formulario.setPrecio(new BigDecimal("149.00"));

        // No se proporciona inventario físico.
        formulario.setCantidad(null);
        formulario.setStockMinimo(null);

        // Prepara las entidades relacionadas.
        Categoria categoria = new Categoria("Literatura clásica");
        categoria.setActivo(true);

        Editorial editorial =
                new Editorial("Penguin Random House", null);

        Autor autor = new Autor("Bram Stoker");

        // Simula las consultas a los repositorios.
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(editorialRepository.findById(1L))
                .thenReturn(Optional.of(editorial));

        when(autorRepository.findAllById(Set.of(1L)))
                .thenReturn(List.of(autor));

        // Simula los guardados sin utilizar MySQL.
        when(libroRepository.save(any(Libro.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        when(formatoLibroRepository.save(any(FormatoLibro.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Ejecuta el registro.
        Libro resultado = libroService.registrar(formulario);

        // Comprueba que se construyó el libro.
        assertNotNull(resultado);
        assertEquals("Drácula", resultado.getTitulo());

        // Libro y presentación comercial deben guardarse.
        verify(libroRepository).save(any(Libro.class));
        verify(formatoLibroRepository).save(any(FormatoLibro.class));

        // Un libro digital nunca debe generar inventario físico.
        verify(itemStockRepository, never())
                .save(any(ItemStock.class));
    }

    /**
     * Comprueba que no se registre un libro
     * cuando su ISBN ya existe en el catálogo.
     *
     * Ninguna entidad debe enviarse a persistencia
     * si se detecta el ISBN duplicado.
     */
    @Test
    void registrar_conIsbnDuplicado_debeRechazarRegistro() {

        // Prepara un formulario válido para llegar
        // a la comprobación de ISBN duplicado.
        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setFormato(Formato.PASTA_DURA);
        formulario.setPrecio(new BigDecimal("399.00"));
        formulario.setCantidad(12);
        formulario.setStockMinimo(3);

        // Simula que el ISBN ya existe en MySQL.
        when(libroRepository.existsByIsbn("9781234567897"))
                .thenReturn(true);

        // Comprueba que el servicio rechaza el registro.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> libroService.registrar(formulario)
        );

        // Verifica el mensaje de negocio.
        assertEquals(
                "Ya existe un libro registrado con ese ISBN.",
                excepcion.getMessage()
        );

        // Confirma que no se intentó guardar ninguna entidad.
        verify(libroRepository, never()).save(any(Libro.class));

        verify(formatoLibroRepository, never())
                .save(any(FormatoLibro.class));

        verify(itemStockRepository, never())
                .save(any(ItemStock.class));

        // Tampoco debe consultar las entidades relacionadas.
        verifyNoInteractions(
                categoriaRepository,
                editorialRepository,
                autorRepository
        );
    }

    /**
     * Comprueba que no se permita registrar un libro
     * utilizando una categoría desactivada.
     *
     * El servicio debe rechazar el registro antes
     * de guardar cualquier entidad.
     */
    @Test
    void registrar_conCategoriaDesactivada_debeRechazarRegistro() {

        // Prepara un formulario con datos válidos.
        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setFormato(Formato.PASTA_DURA);
        formulario.setPrecio(new BigDecimal("399.00"));
        formulario.setCantidad(12);
        formulario.setStockMinimo(3);

        // Simula una categoría existente, pero desactivada.
        Categoria categoria = new Categoria("Literatura clásica");
        categoria.setActivo(false);

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        // Intenta registrar el libro.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> libroService.registrar(formulario)
        );

        // Comprueba que se informe el motivo del rechazo.
        assertEquals(
                "La categoría seleccionada está desactivada.",
                excepcion.getMessage()
        );

        // No debe guardarse ningún registro.
        verify(libroRepository, never())
                .save(any(Libro.class));

        verify(formatoLibroRepository, never())
                .save(any(FormatoLibro.class));

        verify(itemStockRepository, never())
                .save(any(ItemStock.class));

        // No debe continuar consultando las demás relaciones.
        verifyNoInteractions(editorialRepository, autorRepository);
    }

    /**
     * Comprueba que no se permita registrar un libro
     * cuando alguno de los autores seleccionados no existe.
     *
     * Ninguna entidad debe enviarse a persistencia
     * si falta algún autor.
     */
    @Test
    void registrar_conAutorInexistente_debeRechazarRegistro() {

        // Prepara un formulario con dos autores seleccionados.
        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L, 99L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setFormato(Formato.PASTA_DURA);
        formulario.setPrecio(new BigDecimal("399.00"));
        formulario.setCantidad(12);
        formulario.setStockMinimo(3);

        // Prepara una categoría activa y una editorial válida.
        Categoria categoria = new Categoria("Literatura clásica");
        categoria.setActivo(true);

        Editorial editorial =
                new Editorial("Penguin Random House", null);

        // Solamente existe uno de los dos autores solicitados.
        Autor autor = new Autor("Bram Stoker");

        // Simula las consultas a los repositorios.
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(editorialRepository.findById(1L))
                .thenReturn(Optional.of(editorial));

        when(autorRepository.findAllById(Set.of(1L, 99L)))
                .thenReturn(List.of(autor));

        // Ejecuta el registro y comprueba que sea rechazado.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> libroService.registrar(formulario)
        );

        // Verifica el mensaje de validación.
        assertEquals(
                "Uno o más autores seleccionados no existen.",
                excepcion.getMessage()
        );

        // Comprueba que no se intentó guardar ninguna entidad.
        verify(libroRepository, never())
                .save(any(Libro.class));

        verify(formatoLibroRepository, never())
                .save(any(FormatoLibro.class));

        verify(itemStockRepository, never())
                .save(any(ItemStock.class));
    }

}
