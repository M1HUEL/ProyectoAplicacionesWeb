
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.*;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;
import com.itson.ProyectoAplicacionesWeb.storage.ResolvedorUrlsArchivos;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio encargado de administrar la información
 * bibliográfica de los libros de Librería Raudskinna.
 *
 * Contiene las operaciones y reglas de negocio
 * relacionadas con los libros.
 *
 * La persistencia de datos se delega a LibroRepository.
 *
 */
@Service
public class LibroService {


    /** Repositorio para consultar los datos bibliográficos. */
    private final LibroRepository libroRepository;

    /** Repositorio para consultar formatos y precios. */
    private final FormatoLibroRepository formatoLibroRepository;

    /** Repositorio para consultar existencias físicas. */
    private final ItemStockRepository itemStockRepository;

    /**
     * Repositorios para consultar las relaciones obligatorias
     * de un libro antes de registrarlo.
     *
     */
    private final AutorRepository autorRepository;
    private final EditorialRepository editorialRepository;
    private final CategoriaRepository categoriaRepository;

    /** Resuelve las URL de portada según el proveedor de archivos activo. */
    private final ResolvedorUrlsArchivos resolvedorUrlsArchivos;

    /**
     * Inyecta los repositorios necesarios para la gestión
     * administrativa de libros.
     *
     */
    public LibroService(
            LibroRepository libroRepository,
            FormatoLibroRepository formatoLibroRepository,
            ItemStockRepository itemStockRepository,
            AutorRepository autorRepository,
            EditorialRepository editorialRepository,
            CategoriaRepository categoriaRepository,
            ResolvedorUrlsArchivos resolvedorUrlsArchivos) {

        this.libroRepository = libroRepository;
        this.formatoLibroRepository = formatoLibroRepository;
        this.itemStockRepository = itemStockRepository;
        this.autorRepository = autorRepository;
        this.editorialRepository = editorialRepository;
        this.categoriaRepository = categoriaRepository;
        this.resolvedorUrlsArchivos = resolvedorUrlsArchivos;
    }

    /**
     * Recupera todos los libros registrados en la base
     * de datos.
     *
     * La transacción es de solo lectura porque este
     * método no realiza modificaciones.
     *
     * @return lista de libros registrados.
     */
    @Transactional(readOnly = true)
    public List<Libro> listarTodos() {
        return libroRepository.findAll();
    }

    /**
     * Recupera los libros visibles para el catálogo público.
     *
     * Transforma las entidades JPA en DTO para evitar
     * exponer directamente el modelo de persistencia.
     *
     * @return listado de libros activos.
     */
    @Transactional(readOnly = true)
    public List<LibroPublicoDTO> listarLibrosPublicos() {

        return libroRepository.findByActivoTrue()
                .stream()
                .map(libro -> new LibroPublicoDTO(
                        libro.getId(),
                        libro.getTitulo(),
                        libro.getSinopsis(),
                        resolvedorUrlsArchivos.urlPublica(
                                libro.getPortadaUrl()
                        ),
                        libro.getCategoria().getNombre(),
                        libro.getEditorial().getNombre(),
                        libro.getAutores().stream()
                                .map(autor -> autor.getNombre())
                                .sorted()
                                .toList()
                ))
                .toList();
    }

    /**
     * Recupera los libros activos con un resumen comercial
     * para las tarjetas del catálogo público.
     *
     * El precio inicial corresponde al menor precio entre
     * las presentaciones disponibles de cada libro.
     *
     * Los formatos digitales no requieren inventario físico.
     * Los formatos físicos necesitan existencias mayores que cero.
     *
     * Si un libro no tiene presentaciones disponibles,
     * su precio inicial será null y disponible será false.
     *
     * Utiliza consultas agrupadas para evitar consultar
     * los formatos y el inventario individualmente por libro.
     *
     * @return libros públicos con su resumen comercial.
     */
    @Transactional(readOnly = true)
    public List<LibroCatalogoDTO> listarCatalogoPublico() {

        // Recuperar la información bibliográfica pública.
        List<LibroPublicoDTO> libros = listarLibrosPublicos();

        // Recuperar las presentaciones de libros activos.
        List<FormatoLibro> formatos =
                formatoLibroRepository.listarFormatosDeLibrosActivos();

        // Recuperar el inventario físico mediante una consulta agrupada.
        List<ItemStock> inventarios =
                itemStockRepository.listarConFormatoLibro();

        // Asociar cada presentación física con sus existencias.
        Map<Long, Integer> existenciasPorFormato =
                inventarios.stream()
                        .collect(Collectors.toMap(
                                inventario ->
                                        inventario.getFormatoLibro().getId(),
                                ItemStock::getCantidad,
                                (cantidadAnterior, cantidadNueva) ->
                                        cantidadNueva
                        ));

        // Agrupar las presentaciones por identificador de libro.
        Map<Long, List<FormatoLibro>> formatosPorLibro =
                formatos.stream()
                        .collect(Collectors.groupingBy(
                                formato -> formato.getLibro().getId()
                        ));

        // Construir el resumen comercial de cada libro público.
        return libros.stream()
                .map(libro -> {

                    // Recuperar sus presentaciones comerciales.
                    List<FormatoLibro> presentaciones =
                            formatosPorLibro.getOrDefault(
                                    libro.id(),
                                    List.of()
                            );

                    // Encontrar el menor precio entre las
                    // presentaciones que pueden ofrecerse.
                    BigDecimal precioDesde = presentaciones.stream()
                            .filter(formato -> {

                                // Los formatos digitales están disponibles.
                                if (formato.getFormato() == Formato.DIGITAL) {
                                    return true;
                                }

                                // Los formatos físicos necesitan existencias.
                                return existenciasPorFormato.getOrDefault(
                                        formato.getId(),
                                        0
                                ) > 0;
                            })
                            .map(FormatoLibro::getPrecio)
                            .min(BigDecimal::compareTo)
                            .orElse(null);

                    // Existe disponibilidad cuando se encontró
                    // al menos una presentación comercial válida.
                    boolean disponible = precioDesde != null;

                    return new LibroCatalogoDTO(
                            libro,
                            precioDesde,
                            disponible
                    );
                })
                .toList();
    }

    /**
     * Busca libros dentro del catálogo público de
     * Librería Raudskinna mediante su título o autor.
     *
     * Reutiliza la consulta pública de libros activos
     * con su correspondiente información comercial.
     *
     * La búsqueda admite coincidencias parciales y
     * no distingue entre mayúsculas y minúsculas.
     *
     * Cuando el término es nulo o está vacío,
     * devuelve el catálogo público completo.
     *
     * @param termino texto introducido por el visitante.
     * @return libros públicos que coinciden con la búsqueda.
     */
    @Transactional(readOnly = true)
    public List<LibroCatalogoDTO> buscarCatalogoPublico(
            String termino) {

        // Normalizar el término recibido desde el buscador.
        String busqueda = normalizarTextoOpcional(termino);

        // Recuperar el catálogo con su información comercial.
        List<LibroCatalogoDTO> catalogo = listarCatalogoPublico();

        // Sin término de búsqueda, presentar el catálogo completo.
        if (busqueda == null) {
            return catalogo;
        }

        /*
         * Convertir el término a minúsculas para realizar
         * comparaciones independientes de la configuración regional.
         */
        String terminoNormalizado =
                busqueda.toLowerCase(Locale.ROOT);

        // Reutilizar la lógica de coincidencias bibliográficas.
        return catalogo.stream()
                .filter(tarjeta -> coincideConBusqueda(
                        tarjeta.libro(),
                        terminoNormalizado
                ))
                .toList();
    }

    /**
     * Determina si un libro coincide con el término
     * de búsqueda normalizado.
     *
     * Las coincidencias pueden ser parciales y se evalúan
     * sobre el título y cualquiera de los autores asociados.
     *
     * La comparación no distingue entre mayúsculas
     * y minúsculas.
     *
     * @param libro información bibliográfica del libro.
     * @param terminoNormalizado término previamente
     *                           convertido a minúsculas.
     * @return true si coincide el título o algún autor.
     */
    private boolean coincideConBusqueda(
            LibroPublicoDTO libro,
            String terminoNormalizado) {

        // Comprobar coincidencias parciales en el título.
        boolean coincideTitulo = libro.titulo()
                .toLowerCase(Locale.ROOT)
                .contains(terminoNormalizado);

        // Comprobar coincidencias entre los autores asociados.
        boolean coincideAutor = libro.autores()
                .stream()
                .anyMatch(autor -> autor
                        .toLowerCase(Locale.ROOT)
                        .contains(terminoNormalizado));

        // Basta con que coincida el título o algún autor.
        return coincideTitulo || coincideAutor;
    }

    /**
     * Filtra el catálogo público mediante criterios
     * de búsqueda bibliográfica y categoría.
     *
     * Los criterios son opcionales y acumulativos.
     * Cuando se proporcionan ambos, el libro debe
     * satisfacer las dos condiciones.
     *
     * Únicamente se consideran libros activos,
     * conservando su información comercial y
     * disponibilidad.
     *
     * @param termino término de búsqueda por título o autor.
     * @param categoria nombre de la categoría seleccionada.
     * @return libros que satisfacen los criterios proporcionados.
     */
    @Transactional(readOnly = true)

    public List<LibroCatalogoDTO> filtrarCatalogoPublico(
            String termino,
            String categoria) {

        // Delegar el filtrado sin seleccionar un autor específico.
        return filtrarCatalogoPublico(
                termino,
                categoria,
                null
        );
    }

    /**
     * Filtra el catálogo público mediante criterios
     * de búsqueda bibliográfica, categoría y autor.
     *
     * Los criterios son opcionales y acumulativos.
     * Cada libro debe satisfacer todos los criterios
     * proporcionados para formar parte del resultado.
     *
     * Se conserva la información bibliográfica,
     * comercial y de disponibilidad de los libros.
     *
     * @param termino término de búsqueda por título o autor.
     * @param categoria nombre de la categoría seleccionada.
     * @param autor nombre completo del autor seleccionado.
     * @return libros que satisfacen los criterios proporcionados.
     */
    @Transactional(readOnly = true)
    public List<LibroCatalogoDTO> filtrarCatalogoPublico(
            String termino,
            String categoria,
            String autor) {

        // Delegar el filtrado conservando el orden predeterminado.
        return filtrarCatalogoPublico(
                termino,
                categoria,
                autor,
                null
        );
    }

    /**
     * Filtra y ordena el catálogo público mediante los
     * criterios seleccionados por el visitante.
     *
     * Los criterios bibliográficos son opcionales
     * y acumulativos. Cada libro debe satisfacer
     * todos los criterios proporcionados.
     *
     * El ordenamiento se aplica después del filtrado,
     * conservando la información bibliográfica,
     * comercial y de disponibilidad.
     *
     * @param termino término de búsqueda por título o autor.
     * @param categoria nombre de la categoría seleccionada.
     * @param autor nombre completo del autor seleccionado.
     * @param orden criterio de ordenamiento seleccionado.
     * @return libros filtrados y ordenados.
     */
    @Transactional(readOnly = true)
    public List<LibroCatalogoDTO> filtrarCatalogoPublico(
            String termino,
            String categoria,
            String autor,
            String orden) {

        // Recuperar el catálogo público una sola vez.
        List<LibroCatalogoDTO> catalogo =
                listarCatalogoPublico();

        // Delegar el procesamiento del catálogo recuperado.
        return aplicarCriteriosCatalogoPublico(
                catalogo,
                termino,
                categoria,
                autor,
                orden
        );
    }

    /**
     * Aplica los criterios de búsqueda, filtrado y
     * ordenamiento sobre un catálogo previamente recuperado.
     *
     * Este método no realiza consultas a la base de datos.
     * Su responsabilidad consiste exclusivamente en
     * procesar la colección proporcionada.
     *
     * Los criterios bibliográficos son opcionales
     * y acumulativos.
     *
     * @param catalogo catálogo público previamente recuperado.
     * @param termino término de búsqueda por título o autor.
     * @param categoria nombre de la categoría seleccionada.
     * @param autor nombre completo del autor seleccionado.
     * @param orden criterio de ordenamiento seleccionado.
     * @return libros filtrados y ordenados.
     */
    private List<LibroCatalogoDTO> aplicarCriteriosCatalogoPublico(
            List<LibroCatalogoDTO> catalogo,
            String termino,
            String categoria,
            String autor,
            String orden) {

        // Normalizar los criterios proporcionados.
        String busqueda = normalizarTextoOpcional(termino);

        String categoriaSeleccionada =
                normalizarTextoOpcional(categoria);

        String autorSeleccionado =
                normalizarTextoOpcional(autor);

        // Normalizar el término para las comparaciones.
        String terminoNormalizado = busqueda == null
                ? null
                : busqueda.toLowerCase(Locale.ROOT);

        // Aplicar los criterios de manera acumulativa.
        List<LibroCatalogoDTO> resultados = catalogo.stream()

                // Filtrar por coincidencias en título o autor.
                .filter(tarjeta ->
                        terminoNormalizado == null
                                || coincideConBusqueda(
                                tarjeta.libro(),
                                terminoNormalizado
                        )
                )

                // Filtrar por la categoría seleccionada.
                .filter(tarjeta ->
                        categoriaSeleccionada == null
                                || categoriaSeleccionada.equalsIgnoreCase(
                                tarjeta.libro().categoria()
                        )
                )

                // Filtrar por cualquiera de los autores asociados.
                .filter(tarjeta ->
                        autorSeleccionado == null
                                || tarjeta.libro().autores()
                                .stream()
                                .anyMatch(nombreAutor ->
                                        autorSeleccionado
                                                .equalsIgnoreCase(nombreAutor)
                                )
                )

                // Obtener los libros que cumplen todos los criterios.
                .toList();

        // Ordenar únicamente los resultados del filtrado.
        return ordenarCatalogoPublico(resultados, orden);
    }

    /**
     * Ordena los resultados del catálogo público mediante
     * el criterio seleccionado por el visitante.
     *
     * El ordenamiento comercial utiliza el precio mínimo
     * de las presentaciones disponibles de cada libro.
     *
     * Los libros sin precio disponible permanecen al final
     * tanto en orden ascendente como descendente.
     *
     * Los criterios nulos, vacíos o desconocidos conservan
     * el orden original de los resultados.
     *
     * @param libros resultados del filtrado bibliográfico.
     * @param orden criterio de ordenamiento seleccionado.
     * @return resultados ordenados según el criterio.
     */
    private List<LibroCatalogoDTO> ordenarCatalogoPublico(
            List<LibroCatalogoDTO> libros,
            String orden) {

        // Normalizar el criterio de ordenamiento.
        String criterio = normalizarTextoOpcional(orden);

        // Sin criterio, conservar el orden original.
        if (criterio == null) {
            return libros;
        }

        // Seleccionar el comparador correspondiente.
        Comparator<LibroCatalogoDTO> comparador =
                switch (criterio.toLowerCase(Locale.ROOT)) {

                    // Precio de menor a mayor.
                    case "precio-asc" -> Comparator.comparing(
                            LibroCatalogoDTO::precioDesde,
                            Comparator.nullsLast(
                                    Comparator.naturalOrder()
                            )
                    );

                    // Precio de mayor a menor.
                    case "precio-desc" -> Comparator.comparing(
                            LibroCatalogoDTO::precioDesde,
                            Comparator.nullsLast(
                                    Comparator.reverseOrder()
                            )
                    );

                    // Título en orden alfabético.
                    case "titulo-asc" -> Comparator.comparing(
                            tarjeta -> tarjeta.libro().titulo(),
                            String.CASE_INSENSITIVE_ORDER
                    );

                    // Relevancia o criterio desconocido.
                    default -> null;
                };

        // Conservar el orden original si no existe comparador.
        if (comparador == null) {
            return libros;
        }

        // Ordenar sin modificar la colección original.
        return libros.stream()
                .sorted(comparador)
                .toList();
    }

    /**
     * Consulta el catálogo público y genera las opciones
     * disponibles para su filtrado.
     *
     * Las categorías y autores se obtienen del catálogo
     * completo antes de aplicar los criterios seleccionados
     * por el visitante.
     *
     * Las opciones se presentan sin duplicados y en
     * orden alfabético.
     *
     * El catálogo se recupera una sola vez, evitando
     * consultas adicionales para construir las opciones.
     *
     * @param termino término de búsqueda por título o autor.
     * @param categoria nombre de la categoría seleccionada.
     * @param autor nombre completo del autor seleccionado.
     * @param orden criterio de ordenamiento seleccionado.
     * @return resultados y opciones dinámicas del catálogo.
     */
    @Transactional(readOnly = true)
    public CatalogoPublicoDTO consultarCatalogoPublico(
            String termino,
            String categoria,
            String autor,
            String orden) {

        // Recuperar el catálogo público completo una sola vez.
        List<LibroCatalogoDTO> catalogo =
                listarCatalogoPublico();

        // Obtener las categorías presentes en el catálogo.
        List<String> categorias = catalogo.stream()

                // Recuperar la categoría de cada libro.
                .map(tarjeta -> tarjeta.libro().categoria())

                // Excluir valores nulos o vacíos.
                .filter(Objects::nonNull)
                .filter(nombre -> !nombre.isBlank())

                // Eliminar duplicados.
                .distinct()

                // Ordenar alfabéticamente.
                .sorted(String.CASE_INSENSITIVE_ORDER)

                .toList();

        // Obtener todos los autores asociados a los libros.
        List<String> autores = catalogo.stream()

                // Recuperar las colecciones de autores.
                .flatMap(tarjeta ->
                        tarjeta.libro().autores().stream()
                )

                // Excluir valores nulos o vacíos.
                .filter(Objects::nonNull)
                .filter(nombre -> !nombre.isBlank())

                // Eliminar duplicados.
                .distinct()

                // Ordenar alfabéticamente.
                .sorted(String.CASE_INSENSITIVE_ORDER)

                .toList();

        // Aplicar los criterios sobre el catálogo recuperado.
        List<LibroCatalogoDTO> resultados =
                aplicarCriteriosCatalogoPublico(
                        catalogo,
                        termino,
                        categoria,
                        autor,
                        orden
                );

        // Agrupar los resultados y las opciones de filtrado.
        return new CatalogoPublicoDTO(
                resultados,
                categorias,
                autores
        );
    }

    /**
     * Consulta el detalle público de un libro activo.
     *
     * Los libros inexistentes o inactivos no deben
     * mostrarse a los clientes.
     *
     * @param id identificador del libro.
     * @return información bibliográfica pública.
     */
    @Transactional(readOnly = true)
    public LibroPublicoDTO consultarLibroPublico(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del libro no es válido."
            );
        }

        Libro libro = libroRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "El libro solicitado no está disponible."
                ));

        return new LibroPublicoDTO(
                libro.getId(),
                libro.getTitulo(),
                libro.getSinopsis(),
                resolvedorUrlsArchivos.urlPublica(
                        libro.getPortadaUrl()
                ),
                libro.getCategoria().getNombre(),
                libro.getEditorial().getNombre(),
                libro.getAutores().stream()
                        .map(Autor::getNombre)
                        .sorted()
                        .toList()
        );
    }

    /**
     * Recupera las presentaciones comerciales de un libro
     * visible en el catálogo público de Librería Raudskinna.
     *
     * Devuelve el formato, precio y disponibilidad informativa
     * sin exponer directamente las entidades JPA.
     *
     * Las presentaciones digitales no requieren inventario físico.
     * Las presentaciones físicas solamente se consideran disponibles
     * cuando tienen existencias mayores que cero.
     *
     * La disponibilidad puede cambiar antes del checkout.
     *
     * @param libroId identificador del libro solicitado.
     * @return presentaciones comerciales del libro activo.
     * @throws IllegalArgumentException si el ID no es válido.
     */
    @Transactional(readOnly = true)
    public List<FormatoPublicoDTO> listarFormatosPublicos(Long libroId) {

        // Validar el identificador recibido.
        if (libroId == null || libroId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del libro no es válido."
            );
        }

        // Impedir que se consulten presentaciones de libros inactivos.
        libroRepository.findByIdAndActivoTrue(libroId)
                .orElseThrow(() ->
                        new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "El libro solicitado no está disponible."
                        )
                );

        // Recuperar los formatos comerciales asociados al libro.
        List<FormatoLibro> formatos =
                formatoLibroRepository.findByLibroId(libroId);

        // Transformar cada presentación en información pública.
        return formatos.stream()
                .map(formato -> {

                    boolean disponible;

                    if (formato.getFormato() == Formato.DIGITAL) {

                        // Los libros digitales no manejan inventario físico.
                        disponible = true;

                    } else {

                        // Los formatos físicos necesitan existencias positivas.
                        disponible = itemStockRepository
                                .findByFormatoLibroId(formato.getId())
                                .map(stock -> stock.getCantidad() > 0)
                                .orElse(false);
                    }

                    return new FormatoPublicoDTO(
                            formato.getId(),
                            formato.getFormato(),
                            formato.getPrecio(),
                            disponible
                    );
                })
                .toList();
    }

    /**
     * Obtiene la cantidad total de libros registrados.
     *
     * Utiliza el conteo proporcionado por Spring Data JPA,
     * sin recuperar todos los libros en memoria.
     *
     * @return número de libros registrados.
     */
    @Transactional(readOnly = true)
    public long contarLibros() {
        return libroRepository.count();
    }

    /**
     * Busca un libro mediante su identificador.
     *
     * Se utilizará para recuperar la información
     * necesaria durante la edición administrativa.
     *
     * @param id identificador del libro.
     * @return libro encontrado.
     * @throws IllegalArgumentException si el identificador
     *         es inválido o el libro no existe.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Libro buscarPorId(Long id) {

        // Comprueba que el identificador sea válido.
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del libro no es válido."
            );
        }

        // Recupera el libro o informa que no existe.
        return libroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El libro solicitado no existe."
                ));
    }

    /**
     * Prepara los datos bibliográficos de un libro
     * existente para su edición administrativa.
     *
     * La conversión a DTO se realiza dentro de una
     * transacción de lectura para acceder correctamente
     * a la colección de autores.
     *
     * @param id identificador del libro.
     * @return DTO con los datos actuales del libro.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public LibroFormularioDTO prepararFormularioEdicion(Long id) {

        // Recupera el libro utilizando la validación existente.
        Libro libro = buscarPorId(id);

        // Prepara el objeto que recibirá Thymeleaf.
        LibroFormularioDTO formulario = new LibroFormularioDTO();

        formulario.setTitulo(libro.getTitulo());
        formulario.setIsbn(libro.getIsbn());
        formulario.setCategoriaId(libro.getCategoria().getId());
        formulario.setEditorialId(libro.getEditorial().getId());
        formulario.setSinopsis(libro.getSinopsis());
        formulario.setPortadaUrl(
                resolvedorUrlsArchivos.urlPublica(libro.getPortadaUrl())
        );
        formulario.setActivo(libro.isActivo());

        // Recupera los identificadores de los autores asociados.
        Set<Long> autoresIds = libro.getAutores().stream()
                .map(Autor::getId)
                .collect(Collectors.toSet());

        formulario.setAutoresIds(autoresIds);

        return formulario;
    }

    /**
     * Recupera las presentaciones comerciales de un libro.
     *
     * Permite consultar sus formatos y precios para
     * las operaciones de administración del catálogo.
     *
     * @param libroId identificador del libro.
     * @return lista de presentaciones comerciales asociadas.
     * @throws IllegalArgumentException si el ID es inválido.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<FormatoLibro> listarFormatosPorLibro(Long libroId) {

        // Comprueba que el identificador sea válido.
        if (libroId == null || libroId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del libro no es válido."
            );
        }

        // Recupera las presentaciones comerciales del libro.
        return formatoLibroRepository.findByLibroId(libroId);
    }

    /**
     * Recupera una presentación comercial específica
     * y verifica que pertenezca al libro solicitado.
     *
     * @param libroId identificador del libro.
     * @param formatoId identificador de la presentación.
     * @return presentación comercial encontrada.
     * @throws IllegalArgumentException si no existe
     *         o no pertenece al libro indicado.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public FormatoLibro buscarFormatoDeLibro(
            Long libroId,
            Long formatoId) {

        // Valida los identificadores recibidos.
        if (libroId == null || libroId <= 0 ||
                formatoId == null || formatoId <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores del libro y formato no son válidos."
            );
        }

        // Recupera la presentación comercial.
        FormatoLibro formato = formatoLibroRepository.findById(formatoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La presentación comercial solicitada no existe."
                ));

        // Comprueba que la presentación pertenezca al libro indicado.
        if (!libroId.equals(formato.getLibro().getId())) {
            throw new IllegalArgumentException(
                    "La presentación solicitada no pertenece al libro."
            );
        }

        return formato;
    }

    /**
     * Construye el listado de presentaciones comerciales
     * para la pantalla administrativa de libros.
     *
     * Reúne la información bibliográfica, los precios y
     * las existencias físicas en objetos LibroListadoDTO.
     *
     * Cada presentación comercial genera una fila.
     *
     * @return lista de presentaciones para administración.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<LibroListadoDTO> listarParaAdministracion() {

        // Recupera los formatos junto con sus libros y autores.
        List<FormatoLibro> formatos =
                formatoLibroRepository.listarConLibroYAutores();

        // Recupera los registros de inventario físico.
        List<ItemStock> inventarios =
                itemStockRepository.listarConFormatoLibro();

        // Organiza el inventario por el ID de cada presentación.
        // Así evitamos buscar nuevamente en MySQL por cada fila.
        Map<Long, ItemStock> stockPorFormato = inventarios.stream()
                .collect(Collectors.toMap(
                        item -> item.getFormatoLibro().getId(),
                        Function.identity()
                ));

        // Convierte cada presentación comercial en un DTO.
        return formatos.stream().map(formatoLibro -> {

            var libro = formatoLibro.getLibro();

            // Reúne los nombres de los autores del libro.
            String autores = libro.getAutores().stream()
                    .map(autor -> autor.getNombre())
                    .sorted()
                    .collect(Collectors.joining(", "));

            // Consulta el inventario asociado a esta presentación.
            ItemStock inventario =
                    stockPorFormato.get(formatoLibro.getId());

            // Los formatos digitales no manejan existencias físicas.
            Integer stock = null;
            String estado = "No aplica";

            if (formatoLibro.getFormato() != Formato.DIGITAL) {

                if (inventario == null) {
                    estado = "Sin registro";
                } else {
                    stock = inventario.getCantidad();

                    if (stock == 0) {
                        estado = "Sin stock";
                    } else if (stock <= inventario.getStockMinimo()) {
                        estado = "Stock bajo";
                    } else {
                        estado = "Disponible";
                    }
                }
            }

            // Construye la fila que utilizará Thymeleaf.
            return new LibroListadoDTO(
                    libro.getId(),
                    formatoLibro.getId(),
                    libro.getTitulo(),
                    autores,
                    formatoLibro.getFormato().name(),
                    formatoLibro.getPrecio(),
                    stock,
                    estado,
                    libro.isActivo()
            );

        }).toList();
    }

    /**
     * Recupera el inventario físico de una presentación
     * comercial, cuando existe.
     *
     * Los formatos digitales no requieren inventario.
     *
     * @param formatoId identificador de la presentación.
     * @return inventario asociado, si existe.
     * @throws IllegalArgumentException si el ID es inválido.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<ItemStock> buscarStockPorFormato(Long formatoId) {

        // Valida el identificador recibido.
        if (formatoId == null || formatoId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del formato no es válido."
            );
        }

        // Reutiliza el método que ya existe en el repositorio.
        return itemStockRepository.findByFormatoLibroId(formatoId);
    }

    /**
     * Normaliza un texto opcional.
     *
     * Elimina espacios al principio y al final.
     * Si el valor es nulo o queda vacío, devuelve null.
     *
     * @param valor texto que se desea normalizar.
     * @return texto normalizado o null.
     */
    private String normalizarTextoOpcional(String valor) {

        if (valor == null) {
            return null;
        }

        String textoNormalizado = valor.trim();

        return textoNormalizado.isEmpty()
                ? null
                : textoNormalizado;
    }

    /**
     * Valida los datos básicos recibidos desde el formulario
     * administrativo de registro de libros.
     *
     * Las reglas de negocio permanecen en la capa Service,
     * respetando el modelo anémico del proyecto.
     *
     * @param formulario datos capturados por el administrador.
     * @throws IllegalArgumentException si algún dato es inválido.
     *
     */
    private void validarDatosFormulario(LibroFormularioDTO formulario) {

        // Comprueba que se haya recibido un formulario.
        if (formulario == null) {
            throw new IllegalArgumentException(
                    "Los datos del libro son obligatorios."
            );
        }

        // El título es obligatorio.
        if (formulario.getTitulo() == null
                || formulario.getTitulo().isBlank()) {
            throw new IllegalArgumentException(
                    "El título del libro es obligatorio."
            );
        }

        // Respeta la longitud máxima definida en Libro.java.
        if (formulario.getTitulo().trim().length() > 200) {
            throw new IllegalArgumentException(
                    "El título no puede superar los 200 caracteres."
            );
        }

        // El ISBN es obligatorio.
        if (formulario.getIsbn() == null
                || formulario.getIsbn().isBlank()) {
            throw new IllegalArgumentException(
                    "El ISBN del libro es obligatorio."
            );
        }

        // Respeta la longitud máxima definida en Libro.java.
        if (formulario.getIsbn().trim().length() > 20) {
            throw new IllegalArgumentException(
                    "El ISBN no puede superar los 20 caracteres."
            );
        }

        /*
         * Valida la longitud de la URL de portada cuando
         * el administrador proporciona una.
         *
         * El campo es opcional, pero debe respetar el
         * límite de 500 caracteres definido en Libro.java.
         *
         */
        if (formulario.getPortadaUrl() != null
                && formulario.getPortadaUrl().trim().length() > 500) {

            throw new IllegalArgumentException(
                    "La URL de la portada no puede superar los 500 caracteres."
            );
        }

        /*
         * Comprueba que se haya seleccionado al menos un autor.
         *
         * La existencia de los autores en MySQL se verificará
         * posteriormente mediante el repositorio.
         *
         */
        if (formulario.getAutoresIds() == null
                || formulario.getAutoresIds().isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes seleccionar al menos un autor."
            );
        }

        /*
         * Comprueba que se haya seleccionado una editorial.
         */
        if (formulario.getEditorialId() == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar una editorial."
            );
        }

        /*
         * Comprueba que se haya seleccionado una categoría.
         */
        if (formulario.getCategoriaId() == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar una categoría."
            );
        }

        /*
         * Comprueba que el administrador haya seleccionado
         * una presentación comercial para el libro.
         *
         */
        if (formulario.getFormato() == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar un formato para el libro."
            );
        }

        /*
         * El precio debe existir y ser mayor que cero.
         *
         * BigDecimal permite comparar importes monetarios
         * sin utilizar operaciones con float o double.
         */
        if (formulario.getPrecio() == null
                || formulario.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El precio del libro debe ser mayor que cero."
            );
        }

        /*
         * Comprueba que el precio pueda almacenarse en
         * DECIMAL(10,2), conforme a FormatoLibro.java.
         *
         * No se permiten más de dos decimales ni valores
         * superiores a 99,999,999.99.
         *
         */
        BigDecimal precio = formulario.getPrecio();

        if (precio.stripTrailingZeros().scale() > 2
                || precio.compareTo(new BigDecimal("99999999.99")) > 0) {

            throw new IllegalArgumentException(
                    "El precio debe tener máximo dos decimales "
                            + "y no superar 99,999,999.99."
            );
        }

        /*
         * Los formatos físicos requieren existencias iniciales
         * y un umbral mínimo de inventario.
         *
         * Para libros digitales no se exige inventario físico.
         */
        if (formulario.getFormato() != Formato.DIGITAL) {

            if (formulario.getCantidad() == null
                    || formulario.getCantidad() < 0) {
                throw new IllegalArgumentException(
                        "La cantidad disponible debe ser cero o mayor."
                );
            }

            if (formulario.getStockMinimo() == null
                    || formulario.getStockMinimo() < 0) {
                throw new IllegalArgumentException(
                        "El stock mínimo debe ser cero o mayor."
                );
            }
        }
    }

    /**
     * Recupera una categoría y comprueba que esté activa.
     *
     * Se utiliza en las operaciones administrativas
     * de registro y actualización de libros.
     *
     * @param categoriaId identificador de la categoría.
     * @return categoría válida y activa.
     * @throws IllegalArgumentException si no existe
     *         o se encuentra desactivada.
     */
    private Categoria obtenerCategoriaActiva(Long categoriaId) {

        // Recupera la categoría seleccionada.
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La categoría seleccionada no existe."
                ));

        // Impide utilizar categorías desactivadas.
        if (!categoria.isActivo()) {
            throw new IllegalArgumentException(
                    "La categoría seleccionada está desactivada."
            );
        }

        return categoria;
    }

    /**
     * Recupera la editorial seleccionada por el administrador.
     *
     * Permite reutilizar la comprobación de existencia
     * durante el registro y la actualización de libros.
     *
     * @param editorialId identificador de la editorial.
     * @return editorial encontrada.
     * @throws IllegalArgumentException si no existe.
     */
    private Editorial obtenerEditorial(Long editorialId) {

        // Consulta la editorial mediante su identificador.
        return editorialRepository.findById(editorialId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La editorial seleccionada no existe."
                ));
    }

    /**
     * Recupera los autores seleccionados y comprueba
     * que todos existan en la base de datos.
     *
     * Se utilizará durante el registro y la
     * actualización administrativa de libros.
     *
     * @param autoresIds identificadores de los autores.
     * @return conjunto de autores encontrados.
     * @throws IllegalArgumentException si la selección
     *         es inválida o algún autor no existe.
     */
    private Set<Autor> obtenerAutores(Set<Long> autoresIds) {

        // Impide realizar consultas con una selección vacía.
        if (autoresIds == null || autoresIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes seleccionar al menos un autor."
            );
        }

        // Recupera los autores seleccionados.
        List<Autor> autoresEncontrados =
                autorRepository.findAllById(autoresIds);

        // Comprueba que todos los identificadores existan.
        if (autoresEncontrados.size() != autoresIds.size()) {
            throw new IllegalArgumentException(
                    "Uno o más autores seleccionados no existen."
            );
        }

        // Devuelve un conjunto para la relación ManyToMany.
        return new HashSet<>(autoresEncontrados);
    }

    /**
     * Registra un libro y su presentación comercial.
     *
     * La operación será transaccional para garantizar que
     * el libro, su formato y su inventario físico se guarden
     * como una sola unidad de trabajo.
     *
     * @param formulario datos del libro enviados por el administrador.
     * @return libro registrado.
     *
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Libro registrar(LibroFormularioDTO formulario) {

        // Comprueba los datos recibidos antes de persistir.
        validarDatosFormulario(formulario);

        /*
         * Normaliza el ISBN y comprueba que no exista
         * otro libro registrado con el mismo identificador.
         *
         * Esta consulta se realiza antes de comenzar
         * las operaciones de persistencia.
         */
        String isbnLimpio = formulario.getIsbn().trim();

        if (libroRepository.existsByIsbn(isbnLimpio)) {
            throw new IllegalArgumentException(
                    "Ya existe un libro registrado con ese ISBN."
            );
        }

        /*
         * Recupera la categoría seleccionada y verifica
         * que exista y se encuentre activa.
         *
         * Reutiliza las reglas de negocio centralizadas
         * en obtenerCategoriaActiva().
         */
        Categoria categoria = obtenerCategoriaActiva(
                formulario.getCategoriaId()
        );

        /*
         * Recupera la editorial seleccionada y verifica
         * que exista mediante el método reutilizable.
         */
        Editorial editorial = obtenerEditorial(
                formulario.getEditorialId()
        );

        /*
         * Recupera los autores seleccionados y comprueba
         * que todos existan mediante el método reutilizable.
         */
        Set<Autor> autores = obtenerAutores(
                formulario.getAutoresIds()
        );

        /*
         * Construye la entidad Libro con sus datos bibliográficos
         * y las relaciones recuperadas desde MySQL.
         *
         * Toda la coordinación del registro permanece en Service,
         * respetando el modelo anémico.
         *
         */

        // Crea el libro con sus relaciones obligatorias.
        Libro libro = new Libro(
                formulario.getTitulo().trim(),
                isbnLimpio,
                categoria,
                editorial
        );

        // Asocia los autores seleccionados.
        libro.setAutores(autores);

        /*
         * Normaliza los campos bibliográficos opcionales.
         *
         * El método auxiliar elimina espacios exteriores
         * y convierte los textos vacíos en null.
         */
        libro.setSinopsis(
                normalizarTextoOpcional(formulario.getSinopsis())
        );

        libro.setPortadaUrl(
                normalizarTextoOpcional(formulario.getPortadaUrl())
        );
        libro.setActivo(formulario.isActivo());
        // Persiste el libro y obtiene su ID generado.
        Libro libroGuardado = libroRepository.save(libro);

        /*
         * Registra la presentación comercial del libro.
         *
         * Cada presentación tiene un formato y precio propios.
         * La restricción UNIQUE de la tabla formatos_libro
         * evita repetir un formato para el mismo libro.
         */

        /*
         * Normaliza el precio a dos decimales antes de
         * almacenar la presentación comercial.
         *
         * validarFormularioRegistro() ya comprobó que
         * no existen decimales significativos adicionales.
         */
        BigDecimal precioNormalizado =
                formulario.getPrecio().setScale(2);

        FormatoLibro formatoLibro = new FormatoLibro(
                libroGuardado,
                formulario.getFormato(),
                precioNormalizado
        );

        // Guarda la presentación comercial mediante JPA.
        FormatoLibro formatoGuardado =
                formatoLibroRepository.save(formatoLibro);

        /*
         * Registra el inventario inicial únicamente cuando
         * la presentación comercial corresponde a un libro físico.
         *
         * Los formatos digitales no requieren ItemStock.
         *
         * Las cantidades ya fueron validadas anteriormente
         * por validarFormularioRegistro().
         *
         */
        if (formulario.getFormato() != Formato.DIGITAL) {

            // Crea las existencias de la presentación comercial.
            ItemStock itemStock = new ItemStock(
                    formatoGuardado,
                    formulario.getCantidad(),
                    formulario.getStockMinimo()
            );

            // Persiste el inventario inicial mediante JPA.
            itemStockRepository.save(itemStock);
        }

        /*
         * Devuelve el libro registrado una vez preparadas
         * todas las operaciones de persistencia.
         *
         * Al finalizar correctamente el método, Spring
         * podrá confirmar la transacción.
         *
         */
        return libroGuardado;

    }

    /**
     * Actualiza los datos de un libro y una de sus
     * presentaciones comerciales existentes.
     *
     * @param libroId identificador del libro.
     * @param formatoId identificador de la presentación.
     * @param formulario datos recibidos del administrador.
     * @return libro actualizado.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Libro actualizar(
            Long libroId,
            Long formatoId,
            LibroFormularioDTO formulario) {

        /*
         * Primera etapa de la actualización:
         *
         * Recupera la presentación comercial y conserva
         * el formato existente, sin depender del valor
         * enviado por el navegador.
         */

        // Recupera la presentación y verifica su pertenencia.
        FormatoLibro formatoExistente =
                buscarFormatoDeLibro(libroId, formatoId);

        // Comprueba que se hayan recibido datos.
        if (formulario == null) {
            throw new IllegalArgumentException(
                    "Los datos del libro son obligatorios."
            );
        }

        // El formato original prevalece sobre el formulario.
        formulario.setFormato(formatoExistente.getFormato());

        /*
         * Valida los datos recibidos utilizando las mismas
         * reglas de negocio del registro de libros.
         */
        validarDatosFormulario(formulario);

        // Recupera el libro asociado a la presentación.
        Libro libro = formatoExistente.getLibro();

        /*
         * Comprueba que el ISBN solicitado no pertenezca
         * a otro libro.
         *
         * Si el ISBN no cambia, no es necesario consultar
         * nuevamente el repositorio.
         */
        String isbnLimpio = formulario.getIsbn().trim();

        if (!isbnLimpio.equals(libro.getIsbn())
                && libroRepository.existsByIsbn(isbnLimpio)) {

            throw new IllegalArgumentException(
                    "Ya existe un libro registrado con ese ISBN."
            );
        }


        /*
         * Permite conservar la categoría original del libro,
         * incluso cuando haya sido desactivada posteriormente.
         *
         * Si el administrador selecciona otra categoría,
         * esta debe encontrarse activa.
         */
        Categoria categoria;

        Long categoriaOriginalId = libro.getCategoria().getId();

        if (categoriaOriginalId != null
                && categoriaOriginalId.equals(formulario.getCategoriaId())) {

            // La categoría no cambió: conservar la asociación existente.
            categoria = libro.getCategoria();

        } else {

            // La categoría cambió: exigir que la nueva esté activa.
            categoria = obtenerCategoriaActiva(
                    formulario.getCategoriaId()
            );
        }

        /*
         * Recupera la editorial seleccionada y comprueba
         * que exista en la base de datos.
         *
         * Reutiliza el método empleado durante el registro.
         */
        Editorial editorial = obtenerEditorial(
                formulario.getEditorialId()
        );

        /*
         * Recupera los autores seleccionados y comprueba
         * que todos existan en la base de datos.
         *
         * Reutiliza la misma validación utilizada durante
         * el registro de libros.
         */
        Set<Autor> autores = obtenerAutores(
                formulario.getAutoresIds()
        );

        /*
         * Recupera y verifica el inventario antes de modificar
         * los datos bibliográficos y comerciales.
         *
         * Los formatos digitales no requieren inventario físico.
         */
        ItemStock inventario = null;

        if (formatoExistente.getFormato() != Formato.DIGITAL) {

            inventario = itemStockRepository
                    .findByFormatoLibroId(formatoId)
                    .orElseThrow(() -> new IllegalStateException(
                            "La presentación física no tiene inventario registrado."
                    ));
        }

        /*
         * Actualiza las relaciones bibliográficas del libro.
         *
         * Las entidades relacionadas ya fueron recuperadas
         * y validadas previamente.
         *
         * Se conserva el mismo objeto Libro y su identificador.
         */
        libro.setCategoria(categoria);
        libro.setEditorial(editorial);
        libro.setAutores(autores);

        // Conserva o actualiza el ISBN del libro existente.
        libro.setIsbn(isbnLimpio);

        /*
         * Actualiza los datos bibliográficos del libro existente.
         *
         * Se conserva la identidad de la entidad: no se crea
         * un nuevo objeto Libro ni se modifica su ID.
         */
        libro.setTitulo(formulario.getTitulo().trim());

        libro.setSinopsis(
                normalizarTextoOpcional(formulario.getSinopsis())
        );

        libro.setPortadaUrl(
                normalizarTextoOpcional(formulario.getPortadaUrl())
        );
        libro.setActivo(formulario.isActivo());
        /*
         * Actualiza el precio de la presentación comercial.
         *
         * El importe ya fue validado mediante
         * validarDatosFormulario().
         *
         * Se conserva la misma entidad FormatoLibro
         * y su identificador original.
         */
        BigDecimal precioNormalizado =
                formulario.getPrecio().setScale(2);

        formatoExistente.setPrecio(precioNormalizado);

        /*
         * Actualiza las existencias de la presentación física.
         *
         * El inventario fue recuperado y verificado previamente,
         * antes de modificar las entidades.
         *
         * Los formatos digitales no utilizan ItemStock.
         */
        if (inventario != null) {

            // La cantidad no se edita aquí: los cambios de existencias
            // se hacen con InventarioService para registrar su motivo
            inventario.setStockMinimo(formulario.getStockMinimo());
        }

        /*
         * Devuelve el libro actualizado.
         *
         * Las modificaciones de las entidades administradas
         * por JPA se confirmarán al finalizar correctamente
         * la transacción.
         */
        return libro;

    }
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Libro cambiarVisibilidad(Long libroId, boolean activo) {
        Libro libro = buscarPorId(libroId);
        libro.setActivo(activo);
        return libro;
    }
}
