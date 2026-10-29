
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.LibroFormularioDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroListadoDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

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

    /**
     * Comprueba que se pueda recuperar un libro
     * existente mediante su identificador.
     *
     * La consulta se simula con Mockito, sin acceder
     * a la base de datos real.
     */
    @Test
    void buscarPorId_conLibroExistente_debeRetornarLibro() {

        // Prepara las entidades necesarias.
        Categoria categoria = new Categoria("Literatura clásica");
        Editorial editorial = new Editorial("Penguin Random House", null);

        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                categoria,
                editorial
        );

        // Simula que el repositorio encuentra el libro.
        when(libroRepository.findById(1L))
                .thenReturn(Optional.of(libro));

        // Ejecuta la operación.
        Libro resultado = libroService.buscarPorId(1L);

        // Comprueba que se recuperó el libro esperado.
        assertSame(libro, resultado);
        assertEquals("Drácula", resultado.getTitulo());

        // Verifica que se consultó el repositorio.
        verify(libroRepository).findById(1L);
    }

    /**
     * Comprueba que el servicio rechace la búsqueda
     * de un libro que no existe en la base de datos.
     */
    @Test
    void buscarPorId_conLibroInexistente_debeLanzarExcepcion() {

        // Simula que el repositorio no encuentra el libro.
        when(libroRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Comprueba que el servicio rechace la búsqueda.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> libroService.buscarPorId(99L)
        );

        // Verifica el mensaje de negocio.
        assertEquals(
                "El libro solicitado no existe.",
                excepcion.getMessage()
        );

        // Comprueba que se realizó la búsqueda.
        verify(libroRepository).findById(99L);
    }

    /**
     * Comprueba que se rechacen identificadores nulos,
     * iguales a cero o negativos.
     *
     * La validación debe ocurrir antes de consultar
     * el repositorio.
     */
    @Test
    void buscarPorId_conIdentificadorInvalido_debeRechazarBusqueda() {

        // Identificadores que no son válidos para buscar libros.
        Long[] identificadoresInvalidos = {null, 0L, -1L};

        for (Long id : identificadoresInvalidos) {

            IllegalArgumentException excepcion = assertThrows(
                    IllegalArgumentException.class,
                    () -> libroService.buscarPorId(id)
            );

            assertEquals(
                    "El identificador del libro no es válido.",
                    excepcion.getMessage()
            );
        }

        // Ningún identificador inválido debe llegar a MySQL.
        verifyNoInteractions(libroRepository);
    }

    /**
     * Comprueba que los datos bibliográficos de un libro
     * existente se transfieran correctamente al DTO
     * utilizado por el formulario de edición.
     */
    @Test
    void prepararFormularioEdicion_conLibroExistente_debeCompletarDTO() {

        // Prepara las entidades relacionadas.
        Categoria categoria = new Categoria("Literatura clásica");
        Editorial editorial = new Editorial("Penguin Random House", null);
        Autor autor = new Autor("Bram Stoker");

        // Crea un libro con los datos que vamos a recuperar.
        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                categoria,
                editorial
        );

        libro.setSinopsis("Novela de terror.");
        libro.setPortadaUrl("https://ejemplo.com/dracula.jpg");
        libro.setAutores(Set.of(autor));

        // Simula la consulta del libro existente.
        when(libroRepository.findById(1L))
                .thenReturn(Optional.of(libro));

        // Ejecuta la preparación del formulario.
        LibroFormularioDTO formulario =
                libroService.prepararFormularioEdicion(1L);

        // Comprueba los datos bibliográficos recuperados.
        assertEquals("Drácula", formulario.getTitulo());
        assertEquals("9781234567897", formulario.getIsbn());
        assertEquals("Novela de terror.", formulario.getSinopsis());
        assertEquals(
                "https://ejemplo.com/dracula.jpg",
                formulario.getPortadaUrl()
        );

        // Verifica que se consultó el libro correcto.
        verify(libroRepository).findById(1L);
    }

    /**
     * Comprueba que se recuperen las presentaciones
     * comerciales asociadas a un libro.
     *
     * La consulta se simula mediante Mockito,
     * sin acceder a MySQL.
     */
    @Test
    void listarFormatosPorLibro_conFormatosExistentes_debeRetornarLista() {

        // Prepara un libro para asociar sus presentaciones.
        Categoria categoria = new Categoria("Literatura clásica");
        Editorial editorial =
                new Editorial("Penguin Random House", null);

        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                categoria,
                editorial
        );

        // Prepara dos presentaciones comerciales.
        FormatoLibro pastaDura = new FormatoLibro(
                libro,
                Formato.PASTA_DURA,
                new BigDecimal("399.00")
        );

        FormatoLibro digital = new FormatoLibro(
                libro,
                Formato.DIGITAL,
                new BigDecimal("149.00")
        );

        // Simula la consulta de formatos asociados al libro.
        when(formatoLibroRepository.findByLibroId(1L))
                .thenReturn(List.of(pastaDura, digital));

        // Ejecuta la operación del servicio.
        List<FormatoLibro> resultado =
                libroService.listarFormatosPorLibro(1L);

        // Comprueba que se recuperaron ambas presentaciones.
        assertEquals(2, resultado.size());
        assertSame(pastaDura, resultado.get(0));
        assertSame(digital, resultado.get(1));

        // Verifica que se consultó el repositorio correctamente.
        verify(formatoLibroRepository).findByLibroId(1L);
    }

    /**
     * Comprueba que la actualización conserve el formato
     * comercial existente, aunque el formulario no lo envíe.
     */
    @Test
    void actualizar_sinFormatoEnFormulario_debeConservarFormatoOriginal() {

        // Prepara las entidades relacionadas.
        Categoria categoria = new Categoria("Literatura clásica");
        Editorial editorial =
                new Editorial("Penguin Random House", null);

        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                categoria,
                editorial
        );

        /*
         * Simula el identificador que MySQL/JPA habría
         * generado al guardar el libro.
         *
         * Es necesario porque esta prueba usa Mockito
         * y no persiste realmente las entidades.
         */
        ReflectionTestUtils.setField(libro, "id", 1L);

        // Presentación comercial existente.
        FormatoLibro formatoExistente = new FormatoLibro(
                libro,
                Formato.DIGITAL,
                new BigDecimal("149.00")
        );

        /*
         * Simula un formulario de edición válido.
         *
         * El formato permanece en null porque el select
         * deshabilitado no se envía desde el navegador.
         */
        LibroFormularioDTO formulario = new LibroFormularioDTO();

        formulario.setTitulo("Drácula - Edición revisada");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setSinopsis("Novela de terror clásico.");
        formulario.setPortadaUrl(null);
        formulario.setFormato(null);
        formulario.setPrecio(new BigDecimal("199.00"));

        // Simula la consulta de la presentación comercial.
        when(formatoLibroRepository.findById(2L))
                .thenReturn(Optional.of(formatoExistente));

        /*
         * Simula las entidades relacionadas que el servicio
         * necesita recuperar durante la actualización.
         */
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(editorialRepository.findById(1L))
                .thenReturn(Optional.of(editorial));

        Autor autor = new Autor("Bram Stoker");

        when(autorRepository.findAllById(Set.of(1L)))
                .thenReturn(List.of(autor));

        /*
         * Comprueba que la actualización conserve el formato
         * original recuperado de la presentación comercial.
         */
        Libro resultado = libroService.actualizar(1L, 2L, formulario);

        assertSame(libro, resultado);
        assertEquals(Formato.DIGITAL, formatoExistente.getFormato());
        assertEquals(Formato.DIGITAL, formulario.getFormato());

        /*
         * Comprueba que los datos bibliográficos del
         * libro existente hayan sido actualizados.
         *
         * No se crea una entidad nueva: se modifica
         * el mismo objeto recuperado del repositorio.
         */
        assertEquals(
                "Drácula - Edición revisada",
                resultado.getTitulo()
        );

        assertEquals(
                "Novela de terror clásico.",
                resultado.getSinopsis()
        );

        assertNull(resultado.getPortadaUrl());

        /*
         * Comprueba que el ISBN original se conserve
         * cuando el administrador no lo modifica.
         *
         * Además, verifica que no se consulte innecesariamente
         * su existencia en la base de datos.
         */
        assertEquals("9781234567897", resultado.getIsbn());

        /*
         * Comprueba que las relaciones bibliográficas
         * del libro correspondan a las entidades
         * recuperadas durante la actualización.
         */
        assertSame(categoria, resultado.getCategoria());
        assertSame(editorial, resultado.getEditorial());

        /*
         * Comprueba que el libro conserve los autores
         * seleccionados durante la actualización.
         *
         * La relación es ManyToMany, por lo que
         * utilizamos un conjunto de entidades.
         */
        assertEquals(1, resultado.getAutores().size());

        assertTrue(resultado.getAutores().contains(autor));

        /*
         * Comprueba que se actualice el precio de la
         * presentación comercial existente.
         *
         * Se verifica también que el importe quede
         * normalizado a dos posiciones decimales.
         */
        assertEquals(
                new BigDecimal("199.00"),
                formatoExistente.getPrecio()
        );

        verify(libroRepository, never())
                .existsByIsbn(anyString());

        /*
         * Comprueba que la actualización de una presentación
         * digital no realice operaciones de inventario físico.
         *
         * Los formatos digitales no utilizan ItemStock.
         */
        verifyNoInteractions(itemStockRepository);

    }

    /**
     * Comprueba que se permita modificar el ISBN
     * de un libro cuando el nuevo valor está disponible.
     */
    @Test
    void actualizar_conIsbnNuevoDisponible_debeActualizarIsbn() {

        // Prepara un libro existente.
        Categoria categoria = new Categoria("Literatura clásica");
        Editorial editorial =
                new Editorial("Penguin Random House", null);

        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                categoria,
                editorial
        );

        // Simula el identificador generado por JPA.
        ReflectionTestUtils.setField(libro, "id", 1L);

        FormatoLibro formatoExistente = new FormatoLibro(
                libro,
                Formato.DIGITAL,
                new BigDecimal("149.00")
        );

        // Prepara el formulario con un ISBN diferente.
        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula");
        formulario.setIsbn("9781234567898");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setPrecio(new BigDecimal("149.00"));

        // El campo deshabilitado no llega desde el navegador.
        formulario.setFormato(null);

        // Simula las consultas.
        when(formatoLibroRepository.findById(2L))
                .thenReturn(Optional.of(formatoExistente));

        /*
         * Simula las relaciones bibliográficas necesarias
         * para completar la actualización del libro.
         */
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(editorialRepository.findById(1L))
                .thenReturn(Optional.of(editorial));

        Autor autor = new Autor("Bram Stoker");

        when(autorRepository.findAllById(Set.of(1L)))
                .thenReturn(List.of(autor));

        when(libroRepository.existsByIsbn("9781234567898"))
                .thenReturn(false);

        // Ejecuta la actualización.
        Libro resultado = libroService.actualizar(1L, 2L, formulario);

        // Comprueba que el ISBN haya cambiado.
        assertEquals("9781234567898", resultado.getIsbn());

        // Verifica que se consultó la disponibilidad del nuevo ISBN.
        verify(libroRepository).existsByIsbn("9781234567898");
    }

    /**
     * Comprueba que se rechace la actualización cuando
     * el nuevo ISBN ya pertenece a otro libro.
     */
    @Test
    void actualizar_conIsbnDuplicado_debeRechazarCambio() {

        // Prepara un libro existente.
        Categoria categoria = new Categoria("Literatura clásica");
        Editorial editorial =
                new Editorial("Penguin Random House", null);

        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                categoria,
                editorial
        );

        // Simula el identificador generado por JPA.
        ReflectionTestUtils.setField(libro, "id", 1L);

        FormatoLibro formatoExistente = new FormatoLibro(
                libro,
                Formato.DIGITAL,
                new BigDecimal("149.00")
        );

        // Prepara el formulario con un ISBN que ya existe.
        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula - Edición revisada");
        formulario.setIsbn("9781234567898");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setPrecio(new BigDecimal("149.00"));
        formulario.setFormato(null);

        // Simula las respuestas de los repositorios.
        when(formatoLibroRepository.findById(2L))
                .thenReturn(Optional.of(formatoExistente));

        when(libroRepository.existsByIsbn("9781234567898"))
                .thenReturn(true);

        // Comprueba que se rechace el ISBN duplicado.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> libroService.actualizar(1L, 2L, formulario)
        );

        assertEquals(
                "Ya existe un libro registrado con ese ISBN.",
                excepcion.getMessage()
        );

        // El libro debe conservar su información original.
        assertEquals("9781234567897", libro.getIsbn());
        assertEquals("Drácula", libro.getTitulo());

        // Se verificó la existencia del ISBN solicitado.
        verify(libroRepository).existsByIsbn("9781234567898");
    }

    /**
     * Comprueba que al editar una presentación física
     * se actualicen sus existencias y stock mínimo.
     */
    @Test
    void actualizar_formatoFisico_debeActualizarInventario() {

        // Prepara las entidades relacionadas.
        Categoria categoria = new Categoria("Literatura clásica");
        Editorial editorial =
                new Editorial("Penguin Random House", null);
        Autor autor = new Autor("Bram Stoker");

        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                categoria,
                editorial
        );

        ReflectionTestUtils.setField(libro, "id", 1L);

        // Presentación física existente.
        FormatoLibro formatoExistente = new FormatoLibro(
                libro,
                Formato.PASTA_DURA,
                new BigDecimal("399.00")
        );

        // Inventario original: 12 unidades, mínimo 3.
        ItemStock inventario = new ItemStock(
                formatoExistente,
                12,
                3
        );

        // Formulario con nuevas existencias.
        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setFormato(null);
        formulario.setPrecio(new BigDecimal("399.00"));
        formulario.setCantidad(8);
        formulario.setStockMinimo(2);

        // Simula las consultas a los repositorios.
        when(formatoLibroRepository.findById(2L))
                .thenReturn(Optional.of(formatoExistente));

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(editorialRepository.findById(1L))
                .thenReturn(Optional.of(editorial));

        when(autorRepository.findAllById(Set.of(1L)))
                .thenReturn(List.of(autor));

        when(itemStockRepository.findByFormatoLibroId(2L))
                .thenReturn(Optional.of(inventario));

        // Ejecuta la actualización.
        Libro resultado = libroService.actualizar(1L, 2L, formulario);

        // Verifica que se conserva el libro existente.
        assertSame(libro, resultado);

        // Comprueba las nuevas existencias.
        assertEquals(8, inventario.getCantidad());
        assertEquals(2, inventario.getStockMinimo());

        // Comprueba que no se registra otro inventario.
        verify(itemStockRepository, never()).save(any(ItemStock.class));
    }

    /**
     * Comprueba que se rechace la actualización de una
     * presentación física sin inventario registrado.
     */
    @Test
    void actualizar_formatoFisicoSinInventario_debeLanzarExcepcion() {

        Categoria categoria = new Categoria("Literatura clásica");
        Editorial editorial =
                new Editorial("Penguin Random House", null);
        Autor autor = new Autor("Bram Stoker");

        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                categoria,
                editorial
        );

        ReflectionTestUtils.setField(libro, "id", 1L);

        FormatoLibro formatoExistente = new FormatoLibro(
                libro,
                Formato.PASTA_DURA,
                new BigDecimal("399.00")
        );

        LibroFormularioDTO formulario = new LibroFormularioDTO();
        formulario.setTitulo("Drácula");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(1L);
        formulario.setFormato(null);
        formulario.setPrecio(new BigDecimal("399.00"));
        formulario.setCantidad(8);
        formulario.setStockMinimo(2);

        when(formatoLibroRepository.findById(2L))
                .thenReturn(Optional.of(formatoExistente));

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(editorialRepository.findById(1L))
                .thenReturn(Optional.of(editorial));

        when(autorRepository.findAllById(Set.of(1L)))
                .thenReturn(List.of(autor));

        // Simula una inconsistencia: no existe ItemStock.
        when(itemStockRepository.findByFormatoLibroId(2L))
                .thenReturn(Optional.empty());

        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class,
                () -> libroService.actualizar(1L, 2L, formulario)
        );

        assertEquals(
                "La presentación física no tiene inventario registrado.",
                excepcion.getMessage()
        );

        // No debe crear automáticamente otro inventario.
        verify(itemStockRepository, never()).save(any(ItemStock.class));
    }

}
