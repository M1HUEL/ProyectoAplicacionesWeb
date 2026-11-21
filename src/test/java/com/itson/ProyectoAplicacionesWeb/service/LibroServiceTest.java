
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.*;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

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
     * Comprueba que el servicio devuelve la cantidad
     * de libros registrada por el repositorio.
     *
     * Utiliza Mockito para simular el conteo sin
     * establecer una conexión con MySQL.
     */
    @Test
    void contarLibros_debeRetornarCantidadRegistrada() {

        // Simula que existen dos libros registrados.
        when(libroRepository.count()).thenReturn(2L);

        // Ejecuta el método que estamos probando.
        long resultado = libroService.contarLibros();

        // Comprueba que el servicio devuelve la cantidad esperada.
        assertEquals(2L, resultado);

        // Verifica que se consultó el repositorio.
        verify(libroRepository).count();

        // Comprueba que no se recuperó el listado completo.
        verify(libroRepository, never()).findAll();
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
    void actualizar_formatoFisico_debeActualizarStockMinimoSinTocarCantidad() {

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
        assertEquals(12, inventario.getCantidad());

        // El stock mínimo sí se actualiza.
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

    /**
     * Comprueba que un libro existente pueda actualizarse
     * conservando su categoría original, aunque esta se
     * encuentre desactivada.
     *
     * Desactivar una categoría no debería impedir modificar
     * otros datos bibliográficos de sus libros asociados.
     */
    @Test
    void actualizar_conCategoriaOriginalInactiva_debePermitirConservarla() {

        // ARRANGE: preparar una categoría desactivada.
        Categoria categoria = new Categoria("Terror");
        categoria.setActivo(false);

        // Simular el identificador persistido de la categoría.
        ReflectionTestUtils.setField(categoria, "id", 1L);

        Editorial editorial = new Editorial(
                "Raudskinna - Edición de prueba",
                null
        );

        // Crear un libro que ya pertenece a la categoría inactiva.
        Libro libro = new Libro(
                "La llamada de Cthulhu",
                "9781234567897",
                categoria,
                editorial
        );

        ReflectionTestUtils.setField(libro, "id", 1L);

        // Utilizar una presentación digital para aislar
        // la prueba de las operaciones de inventario físico.
        FormatoLibro formatoExistente = new FormatoLibro(
                libro,
                Formato.DIGITAL,
                new BigDecimal("149.00")
        );

        // Simular los datos enviados por el formulario de edición.
        LibroFormularioDTO formulario = new LibroFormularioDTO();

        formulario.setTitulo("La llamada de Cthulhu - Edición revisada");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(2L));
        formulario.setEditorialId(1L);

        // El administrador conserva la misma categoría.
        formulario.setCategoriaId(1L);

        formulario.setSinopsis("Relato de horror cósmico.");
        formulario.setPortadaUrl(null);
        formulario.setFormato(null);
        formulario.setPrecio(new BigDecimal("149.00"));

        // Simular los resultados de los repositorios.
        when(formatoLibroRepository.findById(2L))
                .thenReturn(Optional.of(formatoExistente));

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(editorialRepository.findById(1L))
                .thenReturn(Optional.of(editorial));

        Autor autor = new Autor("H. P. Lovecraft");

        when(autorRepository.findAllById(Set.of(2L)))
                .thenReturn(List.of(autor));

        // ACT: actualizar el libro sin cambiar su categoría.
        Libro resultado = libroService.actualizar(
                1L,
                2L,
                formulario
        );

        // ASSERT: verificar que la actualización sea permitida.
        assertSame(libro, resultado);

        assertSame(categoria, resultado.getCategoria());

        assertFalse(resultado.getCategoria().isActivo());

        assertEquals(
                "La llamada de Cthulhu - Edición revisada",
                resultado.getTitulo()
        );

        assertEquals(
                Formato.DIGITAL,
                formatoExistente.getFormato()
        );
    }

    /**
     * Comprueba que no sea posible cambiar la categoría
     * de un libro hacia otra categoría desactivada.
     *
     * Conservar la categoría original inactiva es válido,
     * pero asignar una categoría inactiva diferente no lo es.
     */
    @Test
    void actualizar_conOtraCategoriaInactiva_debeRechazarCambio() {

        // ARRANGE: categoría original del libro.
        Categoria categoriaOriginal = new Categoria("Literatura clásica");
        ReflectionTestUtils.setField(categoriaOriginal, "id", 1L);

        // Nueva categoría que está desactivada.
        Categoria categoriaNueva = new Categoria("Terror");
        categoriaNueva.setActivo(false);
        ReflectionTestUtils.setField(categoriaNueva, "id", 2L);

        Editorial editorial = new Editorial(
                "Raudskinna - Edición de prueba",
                null
        );

        Libro libro = new Libro(
                "Drácula",
                "9781234567897",
                categoriaOriginal,
                editorial
        );

        ReflectionTestUtils.setField(libro, "id", 1L);

        FormatoLibro formatoExistente = new FormatoLibro(
                libro,
                Formato.DIGITAL,
                new BigDecimal("149.00")
        );

        // Preparar una solicitud de edición válida,
        // excepto por la nueva categoría inactiva.
        LibroFormularioDTO formulario = new LibroFormularioDTO();

        formulario.setTitulo("Drácula - Edición revisada");
        formulario.setIsbn("9781234567897");
        formulario.setAutoresIds(Set.of(1L));
        formulario.setEditorialId(1L);
        formulario.setCategoriaId(2L);
        formulario.setSinopsis("Novela de terror clásico.");
        formulario.setPrecio(new BigDecimal("149.00"));

        when(formatoLibroRepository.findById(2L))
                .thenReturn(Optional.of(formatoExistente));

        when(categoriaRepository.findById(2L))
                .thenReturn(Optional.of(categoriaNueva));

        // ACT y ASSERT: rechazar la asignación de otra categoría inactiva.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> libroService.actualizar(1L, 2L, formulario)
        );

        assertEquals(
                "La categoría seleccionada está desactivada.",
                excepcion.getMessage()
        );

        // La relación original del libro debe mantenerse.
        assertSame(categoriaOriginal, libro.getCategoria());
    }

    /**
     * Comprueba que una presentación física se considere
     * disponible cuando tiene existencias mayores que cero.
     */
    @Test
    void listarFormatosPublicos_formatoFisicoConStock_debeEstarDisponible() {

        // ARRANGE: simular un libro activo.
        Libro libro = mock(Libro.class);

        when(libroRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(libro));

        // Preparación comercial con precio registrado.
        FormatoLibro formato = mock(FormatoLibro.class);

        when(formato.getId()).thenReturn(2L);
        when(formato.getFormato()).thenReturn(Formato.PASTA_DURA);
        when(formato.getPrecio())
                .thenReturn(new BigDecimal("350.00"));

        when(formatoLibroRepository.findByLibroId(1L))
                .thenReturn(List.of(formato));

        // El inventario contiene cuatro unidades.
        ItemStock inventario = mock(ItemStock.class);

        when(inventario.getCantidad()).thenReturn(4);

        when(itemStockRepository.findByFormatoLibroId(2L))
                .thenReturn(Optional.of(inventario));

        // ACT: consultar las presentaciones públicas.
        var resultado = libroService.listarFormatosPublicos(1L);

        // ASSERT: comprobar los datos comerciales.
        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).id());
        assertEquals(Formato.PASTA_DURA, resultado.get(0).formato());
        assertEquals(new BigDecimal("350.00"), resultado.get(0).precio());

        assertTrue(resultado.get(0).disponible());

        // Verificar que se consultaron las existencias físicas.
        verify(itemStockRepository).findByFormatoLibroId(2L);
    }

    /**
     * Comprueba que una presentación física agotada
     * no se anuncie como disponible en el catálogo.
     */
    @Test
    void listarFormatosPublicos_formatoFisicoSinStock_debeEstarAgotado() {

        // ARRANGE: simular un libro activo.
        Libro libro = mock(Libro.class);

        when(libroRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(libro));

        FormatoLibro formato = mock(FormatoLibro.class);

        when(formato.getId()).thenReturn(2L);
        when(formato.getFormato()).thenReturn(Formato.PASTA_BLANDA);
        when(formato.getPrecio())
                .thenReturn(new BigDecimal("250.00"));

        when(formatoLibroRepository.findByLibroId(1L))
                .thenReturn(List.of(formato));

        // El inventario existe, pero está agotado.
        ItemStock inventario = mock(ItemStock.class);

        when(inventario.getCantidad()).thenReturn(0);

        when(itemStockRepository.findByFormatoLibroId(2L))
                .thenReturn(Optional.of(inventario));

        // ACT: recuperar las presentaciones.
        var resultado = libroService.listarFormatosPublicos(1L);

        // ASSERT: no debe aparecer como disponible.
        assertEquals(1, resultado.size());
        assertEquals(Formato.PASTA_BLANDA, resultado.get(0).formato());
        assertFalse(resultado.get(0).disponible());

        verify(itemStockRepository).findByFormatoLibroId(2L);
    }

    /**
     * Comprueba que las presentaciones digitales de libros activos
     * se consideren disponibles sin requerir inventario físico.
     *
     * Las consultas públicas no deben solicitar ItemStock
     * cuando la presentación es DIGITAL.
     */
    @Test
    void listarFormatosPublicos_formatoDigital_noDebeConsultarInventario() {

        // ARRANGE: preparar un libro activo simulado.
        Libro libro = mock(Libro.class);

        when(libroRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(libro));

        // Preparar una presentación digital con precio.
        FormatoLibro formatoDigital = mock(FormatoLibro.class);

        when(formatoDigital.getId()).thenReturn(2L);
        when(formatoDigital.getFormato()).thenReturn(Formato.DIGITAL);
        when(formatoDigital.getPrecio())
                .thenReturn(new BigDecimal("149.00"));

        when(formatoLibroRepository.findByLibroId(1L))
                .thenReturn(List.of(formatoDigital));

        // ACT: consultar los formatos comerciales públicos.
        var resultado = libroService.listarFormatosPublicos(1L);

        // ASSERT: el formato digital debe estar disponible.
        assertEquals(1, resultado.size());
        assertEquals(Formato.DIGITAL, resultado.get(0).formato());
        assertEquals(new BigDecimal("149.00"), resultado.get(0).precio());
        assertTrue(resultado.get(0).disponible());

        // No debe consultar inventario físico para un formato digital.
        verifyNoInteractions(itemStockRepository);
    }

    /**
     * Comprueba que una presentación física sin registro
     * de inventario no se anuncie como disponible.
     *
     * Una presentación sin ItemStock no debe considerarse
     * disponible para la compra.
     */
    @Test
    void listarFormatosPublicos_formatoFisicoSinInventario_debeEstarAgotado() {

        // ARRANGE: preparar un libro activo.
        Libro libro = mock(Libro.class);

        when(libroRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(libro));

        // Preparar una presentación comercial física.
        FormatoLibro formato = mock(FormatoLibro.class);

        when(formato.getId()).thenReturn(2L);
        when(formato.getFormato()).thenReturn(Formato.PASTA_DURA);
        when(formato.getPrecio())
                .thenReturn(new BigDecimal("350.00"));

        when(formatoLibroRepository.findByLibroId(1L))
                .thenReturn(List.of(formato));

        // No existe registro ItemStock para esta presentación.
        when(itemStockRepository.findByFormatoLibroId(2L))
                .thenReturn(Optional.empty());

        // ACT: consultar los formatos públicos del libro.
        var resultado = libroService.listarFormatosPublicos(1L);

        // ASSERT: la presentación debe aparecer como no disponible.
        assertEquals(1, resultado.size());
        assertEquals(Formato.PASTA_DURA, resultado.get(0).formato());
        assertFalse(resultado.get(0).disponible());

        // Confirmar que se consultó el inventario físico.
        verify(itemStockRepository).findByFormatoLibroId(2L);
    }

    /**
     * Comprueba que el catálogo público no exponga
     * presentaciones comerciales de libros inactivos
     * o inexistentes.
     *
     * El servicio debe rechazar la consulta antes
     * de recuperar formatos o inventario.
     */
    @Test
    void listarFormatosPublicos_libroInactivo_debeRetornar404() {

        // ARRANGE: el repositorio no encuentra un libro activo.
        when(libroRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.empty());

        // ACT: intentar consultar los formatos públicos.
        var excepcion = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> libroService.listarFormatosPublicos(1L)
        );

        // ASSERT: comprobar que la respuesta corresponde a HTTP 404.
        assertEquals(
                org.springframework.http.HttpStatus.NOT_FOUND,
                excepcion.getStatusCode()
        );

        // No se deben consultar datos comerciales de libros ocultos.
        verifyNoInteractions(
                formatoLibroRepository,
                itemStockRepository
        );
    }

    /**
     * Comprueba que la consulta pública rechace identificadores
     * nulos, iguales a cero o negativos.
     *
     * Los datos inválidos deben rechazarse antes de realizar
     * cualquier consulta a los repositorios.
     */
    @Test
    void listarFormatosPublicos_idInvalido_debeRechazarConsulta() {

        // ARRANGE: identificadores que no representan libros válidos.
        Long[] identificadoresInvalidos = {
                null,
                0L,
                -1L
        };

        // ACT y ASSERT: verificar cada identificador inválido.
        for (Long libroId : identificadoresInvalidos) {

            IllegalArgumentException excepcion = assertThrows(
                    IllegalArgumentException.class,
                    () -> libroService.listarFormatosPublicos(libroId)
            );

            assertEquals(
                    "El identificador del libro no es válido.",
                    excepcion.getMessage()
            );
        }

        // No debe consultar ningún repositorio.
        verifyNoInteractions(
                libroRepository,
                formatoLibroRepository,
                itemStockRepository
        );
    }

    /**
     * Comprueba que el precio inicial del catálogo público
     * corresponda a la presentación disponible más económica.
     *
     * Las presentaciones físicas agotadas no deben participar
     * en el cálculo, aunque tengan un precio menor.
     */
    @Test
    void listarCatalogoPublico_debeCalcularMenorPrecioDisponible() {

        // ARRANGE: preparar un libro público con sus relaciones.
        Categoria categoria = mock(Categoria.class);
        Editorial editorial = mock(Editorial.class);
        Libro libro = mock(Libro.class);

        when(libro.getId()).thenReturn(1L);
        when(libro.getTitulo()).thenReturn("La llamada de Cthulhu");
        when(libro.getCategoria()).thenReturn(categoria);
        when(libro.getEditorial()).thenReturn(editorial);
        when(libro.getAutores()).thenReturn(new HashSet<>());

        when(categoria.getNombre()).thenReturn("Terror");
        when(editorial.getNombre()).thenReturn("Editorial de prueba");

        when(libroRepository.findByActivoTrue())
                .thenReturn(List.of(libro));

        // Preparaciones comerciales del mismo libro.
        FormatoLibro pastaDura = mock(FormatoLibro.class);
        FormatoLibro pastaBlanda = mock(FormatoLibro.class);
        FormatoLibro digital = mock(FormatoLibro.class);

        when(pastaDura.getId()).thenReturn(10L);
        when(pastaDura.getLibro()).thenReturn(libro);
        when(pastaDura.getFormato()).thenReturn(Formato.PASTA_DURA);
        when(pastaDura.getPrecio()).thenReturn(new BigDecimal("350.00"));

        when(pastaBlanda.getId()).thenReturn(20L);
        when(pastaBlanda.getLibro()).thenReturn(libro);
        when(pastaBlanda.getFormato()).thenReturn(Formato.PASTA_BLANDA);
        when(pastaBlanda.getPrecio()).thenReturn(new BigDecimal("100.00"));

        when(digital.getLibro()).thenReturn(libro);
        when(digital.getFormato()).thenReturn(Formato.DIGITAL);
        when(digital.getPrecio()).thenReturn(new BigDecimal("149.00"));

        when(formatoLibroRepository.listarFormatosDeLibrosActivos())
                .thenReturn(List.of(pastaDura, pastaBlanda, digital));

        // Inventario: pasta dura disponible, pasta blanda agotada.
        ItemStock stockDura = mock(ItemStock.class);
        ItemStock stockBlanda = mock(ItemStock.class);

        when(stockDura.getFormatoLibro()).thenReturn(pastaDura);
        when(stockDura.getCantidad()).thenReturn(4);

        when(stockBlanda.getFormatoLibro()).thenReturn(pastaBlanda);
        when(stockBlanda.getCantidad()).thenReturn(0);

        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of(stockDura, stockBlanda));

        // ACT: construir el catálogo público.
        List<LibroCatalogoDTO> resultado =
                libroService.listarCatalogoPublico();

        // ASSERT: se conserva el libro y se calcula el precio correcto.
        assertEquals(1, resultado.size());

        LibroCatalogoDTO tarjeta = resultado.get(0);

        assertEquals("La llamada de Cthulhu", tarjeta.libro().titulo());
        assertEquals(new BigDecimal("149.00"), tarjeta.precioDesde());
        assertTrue(tarjeta.disponible());

        // Confirmar que se utilizaron consultas agrupadas.
        verify(formatoLibroRepository).listarFormatosDeLibrosActivos();
        verify(itemStockRepository).listarConFormatoLibro();

        // No deben hacerse consultas individuales por presentación.
        verify(itemStockRepository, never())
                .findByFormatoLibroId(anyLong());
    }

    /**
     * Comprueba que un libro con todas sus presentaciones
     * físicas agotadas no anuncie un precio de compra.
     *
     * Cuando ninguna presentación está disponible,
     * precioDesde debe ser null y disponible debe ser false.
     */
    @Test
    void listarCatalogoPublico_libroAgotado_noDebeMostrarPrecioDesde() {

        // ARRANGE: preparar un libro activo.
        Categoria categoria = mock(Categoria.class);
        Editorial editorial = mock(Editorial.class);
        Libro libro = mock(Libro.class);

        when(libro.getId()).thenReturn(1L);
        when(libro.getTitulo()).thenReturn("Libro agotado");
        when(libro.getCategoria()).thenReturn(categoria);
        when(libro.getEditorial()).thenReturn(editorial);
        when(libro.getAutores()).thenReturn(new HashSet<>());

        when(categoria.getNombre()).thenReturn("Terror");
        when(editorial.getNombre()).thenReturn("Editorial de prueba");

        when(libroRepository.findByActivoTrue())
                .thenReturn(List.of(libro));

        // El libro tiene una presentación física.
        FormatoLibro formato = mock(FormatoLibro.class);

        when(formato.getId()).thenReturn(10L);
        when(formato.getLibro()).thenReturn(libro);
        when(formato.getFormato()).thenReturn(Formato.PASTA_DURA);
        when(formato.getPrecio()).thenReturn(new BigDecimal("350.00"));

        when(formatoLibroRepository.listarFormatosDeLibrosActivos())
                .thenReturn(List.of(formato));

        // Su inventario existe, pero está agotado.
        ItemStock inventario = mock(ItemStock.class);

        when(inventario.getFormatoLibro()).thenReturn(formato);
        when(inventario.getCantidad()).thenReturn(0);

        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of(inventario));

        // ACT: construir el catálogo comercial.
        List<LibroCatalogoDTO> resultado =
                libroService.listarCatalogoPublico();

        // ASSERT: el libro permanece visible, pero agotado.
        assertEquals(1, resultado.size());

        LibroCatalogoDTO tarjeta = resultado.get(0);

        assertEquals("Libro agotado", tarjeta.libro().titulo());
        assertNull(tarjeta.precioDesde());
        assertFalse(tarjeta.disponible());
    }

    /**
     * Comprueba que un libro activo sin presentaciones
     * comerciales permanezca visible en el catálogo.
     *
     * Como no tiene formatos registrados, no debe
     * anunciar precio ni disponibilidad comercial.
     */
    @Test
    void listarCatalogoPublico_libroSinFormatos_debePermanecerVisible() {

        // ARRANGE: preparar un libro activo.
        Categoria categoria = mock(Categoria.class);
        Editorial editorial = mock(Editorial.class);
        Libro libro = mock(Libro.class);

        when(libro.getId()).thenReturn(1L);
        when(libro.getTitulo()).thenReturn("Libro sin formatos");
        when(libro.getCategoria()).thenReturn(categoria);
        when(libro.getEditorial()).thenReturn(editorial);
        when(libro.getAutores()).thenReturn(new HashSet<>());

        when(categoria.getNombre()).thenReturn("Terror");
        when(editorial.getNombre()).thenReturn("Editorial de prueba");

        when(libroRepository.findByActivoTrue())
                .thenReturn(List.of(libro));

        // No existen presentaciones comerciales registradas.
        when(formatoLibroRepository.listarFormatosDeLibrosActivos())
                .thenReturn(List.of());

        // Tampoco existen registros de inventario físico.
        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of());

        // ACT: construir el catálogo público.
        List<LibroCatalogoDTO> resultado =
                libroService.listarCatalogoPublico();

        // ASSERT: el libro permanece visible.
        assertEquals(1, resultado.size());

        LibroCatalogoDTO tarjeta = resultado.get(0);

        assertEquals("Libro sin formatos", tarjeta.libro().titulo());
        assertNull(tarjeta.precioDesde());
        assertFalse(tarjeta.disponible());

        // Confirmar el uso de consultas agrupadas.
        verify(formatoLibroRepository).listarFormatosDeLibrosActivos();
        verify(itemStockRepository).listarConFormatoLibro();
    }

    /**
     * Comprueba que el catálogo público devuelva una lista vacía
     * cuando no existen libros activos.
     *
     * No deben generarse tarjetas comerciales ficticias,
     * incluso si existen formatos o inventarios registrados.
     */
    @Test
    void listarCatalogoPublico_sinLibrosActivos_debeRetornarListaVacia() {

        // ARRANGE: no existen libros visibles para el público.
        when(libroRepository.findByActivoTrue())
                .thenReturn(List.of());

        // Las consultas agrupadas tampoco encuentran datos públicos.
        when(formatoLibroRepository.listarFormatosDeLibrosActivos())
                .thenReturn(List.of());

        when(itemStockRepository.listarConFormatoLibro())
                .thenReturn(List.of());

        // ACT: consultar el catálogo público.
        List<LibroCatalogoDTO> resultado =
                libroService.listarCatalogoPublico();

        // ASSERT: no deben generarse tarjetas.
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        // Confirmar que se consultaron los datos agrupados.
        verify(libroRepository).findByActivoTrue();
        verify(formatoLibroRepository).listarFormatosDeLibrosActivos();
        verify(itemStockRepository).listarConFormatoLibro();
    }

    /**
     * Prepara un catálogo público de ejemplo para las
     * pruebas unitarias de búsqueda y filtrado.
     *
     * Incluye libros disponibles y agotados para comprobar
     * que las búsquedas conserven su información comercial.
     *
     * @return catálogo público simulado.
     */
    private List<LibroCatalogoDTO> prepararCatalogoDePrueba() {

        LibroPublicoDTO cthulhu = new LibroPublicoDTO(
                1L,
                "La llamada de Cthulhu",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("H. P. Lovecraft")
        );

        LibroPublicoDTO dracula = new LibroPublicoDTO(
                2L,
                "Drácula",
                "Novela de terror.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("Bram Stoker")
        );

        LibroPublicoDTO hobbit = new LibroPublicoDTO(
                3L,
                "El Hobbit",
                "Novela de fantasía.",
                null,
                "Fantasía",
                "Editorial de prueba",
                List.of("J. R. R. Tolkien")
        );

        return List.of(
                new LibroCatalogoDTO(
                        cthulhu,
                        new BigDecimal("350.00"),
                        true
                ),
                new LibroCatalogoDTO(
                        dracula,
                        null,
                        false
                ),
                new LibroCatalogoDTO(
                        hobbit,
                        new BigDecimal("299.00"),
                        true
                )
        );
    }

    /**
     * Comprueba que la búsqueda pública encuentre libros
     * mediante coincidencias parciales en su título.
     *
     * También verifica que la búsqueda ignore mayúsculas,
     * minúsculas y espacios exteriores, conservando la
     * información comercial del libro encontrado.
     */
    @Test
    void buscarCatalogoPublico_porTitulo_debeRetornarCoincidencias() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        /*
         * Crear un Spy para ejecutar la lógica real de búsqueda,
         * simulando únicamente la recuperación del catálogo.
         */
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: buscar mediante una coincidencia parcial.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.buscarCatalogoPublico("  CTHULHU  ");

        // ASSERT: únicamente debe encontrarse el libro solicitado.
        assertEquals(1, resultado.size());

        LibroCatalogoDTO tarjeta = resultado.get(0);

        // Verificar la información bibliográfica.
        assertEquals(1L, tarjeta.libro().id());

        assertEquals(
                "La llamada de Cthulhu",
                tarjeta.libro().titulo()
        );

        // Verificar que se conserve la información comercial.
        assertEquals(
                new BigDecimal("350.00"),
                tarjeta.precioDesde()
        );

        assertTrue(tarjeta.disponible());

        // Confirmar que se reutilizó la consulta del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que la búsqueda pública encuentre libros
     * mediante coincidencias parciales en el nombre del autor.
     *
     * Los libros agotados deben permanecer visibles cuando
     * coincidan con el término de búsqueda, conservando
     * su correspondiente información comercial.
     */
    @Test
    void buscarCatalogoPublico_porAutor_debeRetornarLibroAgotado() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: buscar mediante el apellido del autor.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.buscarCatalogoPublico("stoker");

        // ASSERT: únicamente debe encontrarse Drácula.
        assertEquals(1, resultado.size());

        LibroCatalogoDTO tarjeta = resultado.get(0);

        // Verificar la información bibliográfica.
        assertEquals(2L, tarjeta.libro().id());

        assertEquals(
                "Drácula",
                tarjeta.libro().titulo()
        );

        assertTrue(
                tarjeta.libro().autores().contains("Bram Stoker")
        );

        // El libro debe conservar su estado de agotamiento.
        assertNull(tarjeta.precioDesde());
        assertFalse(tarjeta.disponible());

        // Confirmar la reutilización del catálogo público.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que la búsqueda pública devuelva una
     * colección vacía cuando ningún libro coincida
     * con el término proporcionado.
     *
     * La ausencia de coincidencias representa un
     * resultado válido y no un error del sistema.
     */
    @Test
    void buscarCatalogoPublico_sinCoincidencias_debeRetornarListaVacia() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: buscar un autor que no pertenece al catálogo.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.buscarCatalogoPublico("Stephen King");

        // ASSERT: debe devolverse una colección válida pero vacía.
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        // Confirmar la reutilización del catálogo público.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que la ausencia del término de búsqueda
     * permita consultar el catálogo público completo.
     *
     * Este escenario ocurre cuando el visitante accede
     * al catálogo sin proporcionar parámetros de búsqueda.
     */
    @Test
    void buscarCatalogoPublico_terminoNulo_debeRetornarCatalogoCompleto() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: consultar el catálogo sin término de búsqueda.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.buscarCatalogoPublico(null);

        // ASSERT: debe conservarse el catálogo completo.
        assertNotNull(resultado);
        assertEquals(catalogo, resultado);

        // Confirmar la reutilización del catálogo público.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que una búsqueda vacía o compuesta
     * únicamente por espacios devuelva el catálogo
     * público completo.
     *
     * No deben aplicarse filtros ni modificarse
     * los datos bibliográficos y comerciales.
     */
    @Test
    void buscarCatalogoPublico_terminoVacio_debeRetornarCatalogoCompleto() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: realizar búsquedas sin contenido significativo.
        List<LibroCatalogoDTO> resultadoVacio =
                servicioSpy.buscarCatalogoPublico("");

        List<LibroCatalogoDTO> resultadoEspacios =
                servicioSpy.buscarCatalogoPublico("   ");

        // ASSERT: ambos resultados deben conservar el catálogo.
        assertEquals(catalogo, resultadoVacio);
        assertEquals(catalogo, resultadoEspacios);

        // Confirmar una consulta por cada búsqueda.
        verify(servicioSpy, times(2)).listarCatalogoPublico();
    }

    /**
     * Comprueba que el catálogo público pueda filtrarse
     * mediante una categoría específica.
     *
     * El filtro debe conservar tanto los libros disponibles
     * como los agotados que pertenezcan a dicha categoría.
     *
     * La comparación no distingue entre mayúsculas
     * y minúsculas y admite espacios exteriores.
     */
    @Test
    void filtrarCatalogoPublico_porCategoria_debeRetornarCoincidencias() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: filtrar por categoría sin término de búsqueda.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        "  TERROR  "
                );

        // ASSERT: deben recuperarse dos libros.
        assertNotNull(resultado);
        assertEquals(2, resultado.size());

        // Comprobar que todos pertenecen a la categoría solicitada.
        assertTrue(resultado.stream()
                .allMatch(tarjeta ->
                        "Terror".equals(tarjeta.libro().categoria())
                ));

        // Confirmar la presencia del libro disponible.
        assertEquals(catalogo.get(0), resultado.get(0));
        assertTrue(resultado.get(0).disponible());

        // Confirmar que el libro agotado permanece visible.
        assertEquals(catalogo.get(1), resultado.get(1));
        assertFalse(resultado.get(1).disponible());
        assertNull(resultado.get(1).precioDesde());

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy, times(1)).listarCatalogoPublico();
    }

    /**
     * Comprueba que el filtrado por una categoría
     * inexistente devuelva una colección vacía.
     *
     * La ausencia de coincidencias representa un
     * resultado válido y no un error del sistema.
     */
    @Test
    void filtrarCatalogoPublico_categoriaInexistente_debeRetornarListaVacia() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: filtrar por una categoría inexistente.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        "Ciencia ficción"
                );

        // ASSERT: debe devolverse una colección válida pero vacía.
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que la ausencia de criterios de búsqueda
     * y categoría permita consultar el catálogo completo.
     *
     * No deben excluirse libros por su categoría
     * ni por su disponibilidad comercial.
     */
    @Test
    void filtrarCatalogoPublico_categoriaNula_debeRetornarCatalogoCompleto() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: consultar sin criterios de filtrado.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(null, null);

        // ASSERT: debe conservarse el catálogo completo.
        assertNotNull(resultado);
        assertEquals(catalogo, resultado);

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que una categoría vacía o compuesta
     * únicamente por espacios no restrinja los resultados
     * del catálogo público.
     *
     * Ambos valores deben interpretarse como ausencia
     * del criterio de filtrado por categoría.
     */
    @Test
    void filtrarCatalogoPublico_categoriaVacia_debeRetornarCatalogoCompleto() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: realizar consultas con categorías vacías.
        List<LibroCatalogoDTO> resultadoVacio =
                servicioSpy.filtrarCatalogoPublico(null, "");

        List<LibroCatalogoDTO> resultadoEspacios =
                servicioSpy.filtrarCatalogoPublico(null, "   ");

        // ASSERT: ambos resultados deben conservar el catálogo.
        assertEquals(catalogo, resultadoVacio);
        assertEquals(catalogo, resultadoEspacios);

        // Confirmar una recuperación por cada consulta.
        verify(servicioSpy, times(2)).listarCatalogoPublico();
    }

    /**
     * Comprueba que el catálogo público permita combinar
     * un término de búsqueda con una categoría específica.
     *
     * El libro debe satisfacer ambos criterios para
     * formar parte de los resultados.
     */
    @Test
    void filtrarCatalogoPublico_criteriosCompatibles_debeRetornarCoincidencia() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: combinar una búsqueda por autor y categoría.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        "Lovecraft",
                        "Terror"
                );

        // ASSERT: únicamente debe recuperarse un libro.
        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        // Confirmar que se conserva toda su información.
        assertEquals(catalogo.get(0), resultado.get(0));

        assertEquals(
                "La llamada de Cthulhu",
                resultado.get(0).libro().titulo()
        );

        assertEquals(
                "Terror",
                resultado.get(0).libro().categoria()
        );

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que la combinación de criterios
     * incompatibles devuelva una colección vacía.
     *
     * Aunque existan coincidencias individuales,
     * ningún libro debe incluirse si no satisface
     * todos los criterios proporcionados.
     */
    @Test
    void filtrarCatalogoPublico_criteriosIncompatibles_debeRetornarListaVacia() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: combinar criterios bibliográficos incompatibles.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        "Lovecraft",
                        "Fantasía"
                );

        // ASSERT: ningún libro debe satisfacer ambos criterios.
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que el catálogo público pueda filtrarse
     * mediante un autor específico.
     *
     * La comparación debe realizarse utilizando el nombre
     * completo del autor, sin distinguir entre mayúsculas
     * y minúsculas y admitiendo espacios exteriores.
     *
     * Debe conservarse toda la información bibliográfica
     * y comercial del libro encontrado.
     */
    @Test
    void filtrarCatalogoPublico_porAutor_debeRetornarCoincidencias() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: filtrar únicamente por autor.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        "  H. P. LOVECRAFT  "
                );

        // ASSERT: únicamente debe recuperarse un libro.
        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        // Confirmar que se conserva toda su información.
        assertEquals(catalogo.get(0), resultado.get(0));

        // Verificar la información bibliográfica.
        assertEquals(
                "La llamada de Cthulhu",
                resultado.get(0).libro().titulo()
        );

        assertTrue(resultado.get(0).libro().autores()
                .contains("H. P. Lovecraft"));

        // Verificar la información comercial.
        assertTrue(resultado.get(0).disponible());

        assertEquals(
                new BigDecimal("350.00"),
                resultado.get(0).precioDesde()
        );

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que el filtrado por un autor inexistente
     * devuelva una colección vacía.
     *
     * La ausencia de coincidencias representa un
     * resultado válido y no un error del sistema.
     */
    @Test
    void filtrarCatalogoPublico_autorInexistente_debeRetornarListaVacia() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: filtrar por un autor inexistente.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        "Stephen King"
                );

        // ASSERT: debe devolverse una colección válida pero vacía.
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que el filtro por autor requiera una
     * coincidencia completa con el nombre registrado.
     *
     * A diferencia del buscador bibliográfico, el filtro
     * por autor no debe admitir coincidencias parciales.
     */
    @Test
    void filtrarCatalogoPublico_autorParcial_debeRetornarListaVacia() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: filtrar utilizando únicamente el apellido.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        "Lovecraft"
                );

        // ASSERT: no deben admitirse coincidencias parciales.
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que el filtrado por autor conserve
     * los libros que no tienen disponibilidad comercial.
     *
     * La ausencia de existencias no debe impedir
     * que un libro activo aparezca en el catálogo.
     */
    @Test
    void filtrarCatalogoPublico_porAutor_debeConservarLibroAgotado() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: filtrar por el autor de un libro agotado.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        "Bram Stoker"
                );

        // ASSERT: debe recuperarse el libro agotado.
        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        // Confirmar que se conserva toda su información.
        assertEquals(catalogo.get(1), resultado.get(0));

        assertEquals(
                "Drácula",
                resultado.get(0).libro().titulo()
        );

        // Verificar su información comercial.
        assertFalse(resultado.get(0).disponible());
        assertNull(resultado.get(0).precioDesde());

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que un autor nulo, vacío o compuesto
     * únicamente por espacios no restrinja los
     * resultados del catálogo público.
     *
     * Los tres valores deben interpretarse como
     * ausencia del criterio de filtrado por autor.
     */
    @Test
    void filtrarCatalogoPublico_autorSinContenido_debeRetornarCatalogoCompleto() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: realizar consultas sin un autor específico.
        List<LibroCatalogoDTO> resultadoNulo =
                servicioSpy.filtrarCatalogoPublico(
                        null, null, null
                );

        List<LibroCatalogoDTO> resultadoVacio =
                servicioSpy.filtrarCatalogoPublico(
                        null, null, ""
                );

        List<LibroCatalogoDTO> resultadoEspacios =
                servicioSpy.filtrarCatalogoPublico(
                        null, null, "   "
                );

        // ASSERT: deben conservarse todos los libros.
        assertEquals(catalogo, resultadoNulo);
        assertEquals(catalogo, resultadoVacio);
        assertEquals(catalogo, resultadoEspacios);

        // Confirmar una recuperación por cada consulta.
        verify(servicioSpy, times(3)).listarCatalogoPublico();
    }

    /**
     * Comprueba que el filtrado por autor considere
     * todos los autores asociados a un libro.
     *
     * Un libro escrito por múltiples autores debe
     * aparecer cuando cualquiera de ellos sea
     * seleccionado como criterio de filtrado.
     */
    @Test
    void filtrarCatalogoPublico_multiplesAutores_debeRetornarCoincidencias() {

        // ARRANGE: preparar un libro con múltiples autores.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                4L,
                "El superviviente",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of(
                        "H. P. Lovecraft",
                        "August Derleth"
                )
        );

        // Preparar su información comercial.
        LibroCatalogoDTO tarjeta = new LibroCatalogoDTO(
                libro,
                new BigDecimal("280.00"),
                true
        );

        List<LibroCatalogoDTO> catalogo = List.of(tarjeta);

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: filtrar por cada uno de los autores asociados.
        List<LibroCatalogoDTO> resultadoPrimerAutor =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        "H. P. Lovecraft"
                );

        List<LibroCatalogoDTO> resultadoSegundoAutor =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        "August Derleth"
                );

        // ASSERT: ambos autores deben conducir al mismo libro.
        assertEquals(catalogo, resultadoPrimerAutor);
        assertEquals(catalogo, resultadoSegundoAutor);

        // Confirmar una recuperación por cada consulta.
        verify(servicioSpy, times(2)).listarCatalogoPublico();
    }

    /**
     * Comprueba que el catálogo público permita combinar
     * simultáneamente los tres criterios de filtrado:
     *
     * - Término de búsqueda.
     * - Categoría seleccionada.
     * - Autor seleccionado.
     *
     * El libro debe satisfacer todos los criterios
     * para formar parte de los resultados.
     */
    @Test
    void filtrarCatalogoPublico_tresCriteriosCompatibles_debeRetornarCoincidencia() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: combinar los tres criterios de filtrado.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        "Cthulhu",
                        "Terror",
                        "H. P. Lovecraft"
                );

        // ASSERT: únicamente debe recuperarse un libro.
        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        // Confirmar que se conserva toda su información.
        assertEquals(catalogo.get(0), resultado.get(0));

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que la combinación de tres criterios
     * incompatibles devuelva una colección vacía.
     *
     * Aunque un libro satisfaga dos de los criterios,
     * no debe incluirse si incumple el tercero.
     */
    @Test
    void filtrarCatalogoPublico_tresCriteriosIncompatibles_debeRetornarListaVacia() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: combinar criterios incompatibles.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        "Cthulhu",
                        "Terror",
                        "Bram Stoker"
                );

        // ASSERT: ningún libro satisface los tres criterios.
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que el catálogo público pueda ordenarse
     * por precio de menor a mayor.
     *
     * Los libros sin precio disponible deben permanecer
     * al final de los resultados.
     *
     * La colección original no debe modificarse.
     */
    @Test
    void filtrarCatalogoPublico_precioAscendente_debeOrdenarResultados() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: ordenar por precio de menor a mayor.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        null,
                        "precio-asc"
                );

        // ASSERT: comprobar el orden comercial esperado.
        assertEquals(
                List.of(
                        catalogo.get(2), // El Hobbit: $299.00
                        catalogo.get(0), // Cthulhu: $350.00
                        catalogo.get(1)  // Drácula: agotado
                ),
                resultado
        );

        // Confirmar que el libro agotado permanece al final.
        assertNull(resultado.get(2).precioDesde());
        assertFalse(resultado.get(2).disponible());

        // Confirmar que la colección original no fue modificada.
        assertEquals(
                "La llamada de Cthulhu",
                catalogo.get(0).libro().titulo()
        );

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que el catálogo público pueda ordenarse
     * por precio de mayor a menor.
     *
     * Invertir el orden de los precios no debe provocar
     * que los libros sin precio aparezcan al principio
     * de los resultados.
     */
    @Test
    void filtrarCatalogoPublico_precioDescendente_debeOrdenarResultados() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: ordenar por precio de mayor a menor.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        null,
                        "precio-desc"
                );

        // ASSERT: comprobar el orden comercial esperado.
        assertEquals(
                List.of(
                        catalogo.get(0), // Cthulhu: $350.00
                        catalogo.get(2), // El Hobbit: $299.00
                        catalogo.get(1)  // Drácula: agotado
                ),
                resultado
        );

        // Confirmar que el libro agotado permanece al final.
        assertNull(resultado.get(2).precioDesde());
        assertFalse(resultado.get(2).disponible());

        // Confirmar que la colección original no fue modificada.
        assertEquals(
                "Drácula",
                catalogo.get(1).libro().titulo()
        );

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que el catálogo público pueda ordenarse
     * alfabéticamente mediante el título de los libros.
     *
     * La disponibilidad comercial no debe afectar
     * la posición de los libros en este ordenamiento.
     */
    @Test
    void filtrarCatalogoPublico_tituloAscendente_debeOrdenarResultados() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: ordenar alfabéticamente los títulos.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        null,
                        "titulo-asc"
                );

        // ASSERT: comprobar el orden alfabético esperado.
        assertEquals(
                List.of(
                        catalogo.get(1), // Drácula
                        catalogo.get(2), // El Hobbit
                        catalogo.get(0)  // La llamada de Cthulhu
                ),
                resultado
        );

        // Confirmar que la disponibilidad no afecta el orden.
        assertFalse(resultado.get(0).disponible());
        assertNull(resultado.get(0).precioDesde());

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que el criterio de ordenamiento por
     * relevancia conserve el orden original del catálogo.
     *
     * No deben aplicarse comparadores comerciales
     * ni alfabéticos cuando se selecciona esta opción.
     */
    @Test
    void filtrarCatalogoPublico_relevancia_debeConservarOrdenOriginal() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: consultar utilizando el orden predeterminado.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        null,
                        null,
                        "relevancia"
                );

        // ASSERT: debe conservarse el contenido y orden original.
        assertNotNull(resultado);
        assertEquals(catalogo, resultado);

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que los criterios de ordenamiento nulos,
     * vacíos, compuestos por espacios o desconocidos
     * conserven el orden original del catálogo público.
     *
     * Un criterio no reconocido no debe provocar
     * excepciones ni modificar los resultados.
     */
    @Test
    void filtrarCatalogoPublico_ordenNoReconocido_debeConservarOrdenOriginal() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: consultar utilizando diferentes criterios inválidos.
        List<LibroCatalogoDTO> resultadoNulo =
                servicioSpy.filtrarCatalogoPublico(
                        null, null, null, null
                );

        List<LibroCatalogoDTO> resultadoVacio =
                servicioSpy.filtrarCatalogoPublico(
                        null, null, null, ""
                );

        List<LibroCatalogoDTO> resultadoEspacios =
                servicioSpy.filtrarCatalogoPublico(
                        null, null, null, "   "
                );

        List<LibroCatalogoDTO> resultadoDesconocido =
                servicioSpy.filtrarCatalogoPublico(
                        null, null, null, "orden-inexistente"
                );

        // ASSERT: todas las consultas deben conservar el orden.
        assertEquals(catalogo, resultadoNulo);
        assertEquals(catalogo, resultadoVacio);
        assertEquals(catalogo, resultadoEspacios);
        assertEquals(catalogo, resultadoDesconocido);

        // Confirmar una recuperación por cada consulta.
        verify(servicioSpy, times(4)).listarCatalogoPublico();
    }

    /**
     * Comprueba que el catálogo público permita combinar
     * criterios de filtrado y ordenamiento.
     *
     * El ordenamiento debe aplicarse únicamente sobre
     * los libros que satisfacen los criterios bibliográficos.
     *
     * También verifica la normalización del criterio
     * de ordenamiento seleccionado.
     */
    @Test
    void filtrarCatalogoPublico_categoriaYOrden_debeRetornarResultadosOrdenados() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: combinar categoría y ordenamiento alfabético.
        List<LibroCatalogoDTO> resultado =
                servicioSpy.filtrarCatalogoPublico(
                        null,
                        "Terror",
                        null,
                        "  TITULO-ASC  "
                );

        // ASSERT: comprobar el contenido y orden esperado.
        assertNotNull(resultado);

        assertEquals(
                List.of(
                        catalogo.get(1), // Drácula
                        catalogo.get(0)  // La llamada de Cthulhu
                ),
                resultado
        );

        // Confirmar que El Hobbit fue excluido por el filtro.
        assertFalse(resultado.contains(catalogo.get(2)));

        // Confirmar que todos pertenecen a la categoría solicitada.
        assertTrue(resultado.stream()
                .allMatch(tarjeta ->
                        "Terror".equals(tarjeta.libro().categoria())
                ));

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que la consulta del catálogo público
     * genere dinámicamente las opciones de filtrado.
     *
     * Las categorías y autores deben presentarse
     * sin duplicados exactos y en orden alfabético.
     *
     * También verifica que el catálogo completo
     * se recupere una sola vez.
     */
    @Test
    void consultarCatalogoPublico_sinCriterios_debeGenerarOpciones() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: consultar el catálogo sin criterios de filtrado.
        CatalogoPublicoDTO resultado =
                servicioSpy.consultarCatalogoPublico(
                        null,
                        null,
                        null,
                        null
                );

        // ASSERT: verificar el DTO generado.
        assertNotNull(resultado);

        // Deben conservarse todos los libros.
        assertEquals(catalogo, resultado.libros());

        // Verificar las categorías únicas y ordenadas.
        assertEquals(
                List.of("Fantasía", "Terror"),
                resultado.categorias()
        );

        // Verificar los autores ordenados alfabéticamente.
        assertEquals(
                List.of(
                        "Bram Stoker",
                        "H. P. Lovecraft",
                        "J. R. R. Tolkien"
                ),
                resultado.autores()
        );

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy, times(1)).listarCatalogoPublico();
    }

    /**
     * Comprueba que las opciones de filtrado se obtengan
     * del catálogo público completo y no únicamente
     * de los libros que satisfacen los criterios.
     *
     * Seleccionar una categoría no debe provocar que
     * desaparezcan las demás categorías o autores
     * de las listas desplegables.
     */
    @Test
    void consultarCatalogoPublico_conFiltros_debeConservarOpcionesCompletas() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: filtrar por Terror y ordenar alfabéticamente.
        CatalogoPublicoDTO resultado =
                servicioSpy.consultarCatalogoPublico(
                        null,
                        "Terror",
                        null,
                        "titulo-asc"
                );

        // ASSERT: verificar el DTO generado.
        assertNotNull(resultado);

        // Verificar los libros filtrados y ordenados.
        assertEquals(
                List.of(
                        catalogo.get(1), // Drácula
                        catalogo.get(0)  // La llamada de Cthulhu
                ),
                resultado.libros()
        );

        // Ambas categorías deben permanecer disponibles.
        assertEquals(
                List.of("Fantasía", "Terror"),
                resultado.categorias()
        );

        // Todos los autores deben permanecer disponibles.
        assertEquals(
                List.of(
                        "Bram Stoker",
                        "H. P. Lovecraft",
                        "J. R. R. Tolkien"
                ),
                resultado.autores()
        );

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que una búsqueda sin coincidencias
     * conserve las opciones dinámicas de filtrado.
     *
     * La ausencia de resultados no significa que
     * el catálogo público esté vacío.
     *
     * El visitante debe conservar la posibilidad
     * de seleccionar otras categorías y autores.
     */
    @Test
    void consultarCatalogoPublico_sinCoincidencias_debeConservarOpciones() {

        // ARRANGE: preparar el catálogo público de ejemplo.
        List<LibroCatalogoDTO> catalogo =
                prepararCatalogoDePrueba();

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: realizar una búsqueda sin coincidencias.
        CatalogoPublicoDTO resultado =
                servicioSpy.consultarCatalogoPublico(
                        "Stephen King",
                        null,
                        null,
                        null
                );

        // ASSERT: verificar el DTO generado.
        assertNotNull(resultado);

        // No deben existir libros coincidentes.
        assertNotNull(resultado.libros());
        assertTrue(resultado.libros().isEmpty());

        // Las categorías deben permanecer disponibles.
        assertEquals(
                List.of("Fantasía", "Terror"),
                resultado.categorias()
        );

        // Los autores deben permanecer disponibles.
        assertEquals(
                List.of(
                        "Bram Stoker",
                        "H. P. Lovecraft",
                        "J. R. R. Tolkien"
                ),
                resultado.autores()
        );

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que las opciones dinámicas de filtrado
     * consideren todos los autores asociados a cada libro.
     *
     * Los autores que aparezcan en múltiples libros
     * deben incluirse una sola vez en las opciones.
     *
     * Las categorías tampoco deben duplicarse.
     */
    @Test
    void consultarCatalogoPublico_autoresDuplicados_debeGenerarOpcionesUnicas() {

        // ARRANGE: preparar un catálogo ampliable.
        List<LibroCatalogoDTO> catalogo =
                new ArrayList<>(prepararCatalogoDePrueba());

        // Preparar un libro asociado a múltiples autores.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                4L,
                "El superviviente",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of(
                        "H. P. Lovecraft",
                        "August Derleth"
                )
        );

        // Incorporar el libro con su información comercial.
        catalogo.add(
                new LibroCatalogoDTO(
                        libro,
                        new BigDecimal("280.00"),
                        true
                )
        );

        // Simular únicamente la recuperación del catálogo.
        LibroService servicioSpy = spy(libroService);

        doReturn(catalogo)
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: consultar el catálogo completo.
        CatalogoPublicoDTO resultado =
                servicioSpy.consultarCatalogoPublico(
                        null, null, null, null
                );

        // ASSERT: deben conservarse los cuatro libros.
        assertNotNull(resultado);
        assertEquals(catalogo, resultado.libros());
        assertEquals(4, resultado.libros().size());

        // Verificar las categorías únicas y ordenadas.
        assertEquals(
                List.of("Fantasía", "Terror"),
                resultado.categorias()
        );

        // Verificar los autores únicos y ordenados.
        assertEquals(
                List.of(
                        "August Derleth",
                        "Bram Stoker",
                        "H. P. Lovecraft",
                        "J. R. R. Tolkien"
                ),
                resultado.autores()
        );

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

    /**
     * Comprueba que un catálogo público completamente
     * vacío genere un DTO válido.
     *
     * Las colecciones de libros, categorías y autores
     * deben existir, aunque no contengan elementos.
     *
     * Esta condición no representa un error del sistema.
     */
    @Test
    void consultarCatalogoPublico_catalogoVacio_debeRetornarColeccionesVacias() {

        // ARRANGE: simular un catálogo sin libros registrados.
        LibroService servicioSpy = spy(libroService);

        doReturn(List.of())
                .when(servicioSpy)
                .listarCatalogoPublico();

        // ACT: consultar el catálogo público.
        CatalogoPublicoDTO resultado =
                servicioSpy.consultarCatalogoPublico(
                        null, null, null, null
                );

        // ASSERT: debe generarse un DTO válido.
        assertNotNull(resultado);

        // Verificar la colección de libros.
        assertNotNull(resultado.libros());
        assertTrue(resultado.libros().isEmpty());

        // Verificar la colección de categorías.
        assertNotNull(resultado.categorias());
        assertTrue(resultado.categorias().isEmpty());

        // Verificar la colección de autores.
        assertNotNull(resultado.autores());
        assertTrue(resultado.autores().isEmpty());

        // Confirmar una sola recuperación del catálogo.
        verify(servicioSpy).listarCatalogoPublico();
    }

}
