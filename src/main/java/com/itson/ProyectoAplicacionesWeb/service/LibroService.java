
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.LibroFormularioDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroListadoDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;
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
            CategoriaRepository categoriaRepository) {

        this.libroRepository = libroRepository;
        this.formatoLibroRepository = formatoLibroRepository;
        this.itemStockRepository = itemStockRepository;
        this.autorRepository = autorRepository;
        this.editorialRepository = editorialRepository;
        this.categoriaRepository = categoriaRepository;
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
        formulario.setPortadaUrl(libro.getPortadaUrl());

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
                    estado
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
         * Recupera la categoría seleccionada y comprueba
         * que exista y se encuentre activa.
         *
         * Reutiliza la misma regla del registro de libros.
         */
        Categoria categoria = obtenerCategoriaActiva(
                formulario.getCategoriaId()
        );

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

            inventario.setCantidad(formulario.getCantidad());
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

}
