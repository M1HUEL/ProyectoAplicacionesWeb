
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.LibroFormularioDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroListadoDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private void validarFormularioRegistro(LibroFormularioDTO formulario) {

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
        validarFormularioRegistro(formulario);

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

        // Recupera la categoría seleccionada.
        Categoria categoria = categoriaRepository.findById(
                formulario.getCategoriaId()
        ).orElseThrow(() -> new IllegalArgumentException(
                "La categoría seleccionada no existe."
        ));

        /*
         * Impide asignar a un libro nuevo una categoría
         * que haya sido desactivada mediante baja lógica.
         *
         */
        if (!categoria.isActivo()) {
            throw new IllegalArgumentException(
                    "La categoría seleccionada está desactivada."
            );
        }

        // Recupera la editorial seleccionada.
        Editorial editorial = editorialRepository.findById(
                formulario.getEditorialId()
        ).orElseThrow(() -> new IllegalArgumentException(
                "La editorial seleccionada no existe."
        ));

        // Recupera los autores seleccionados.
        List<Autor> autoresEncontrados = autorRepository.findAllById(
                formulario.getAutoresIds()
        );

        /*
         * Verifica que todos los autores seleccionados
         * existan realmente en la base de datos.
         *
         * El conjunto de identificadores no contiene duplicados,
         * por lo que podemos comparar ambas cantidades.
         */
        if (autoresEncontrados.size() != formulario.getAutoresIds().size()) {
            throw new IllegalArgumentException(
                    "Uno o más autores seleccionados no existen."
            );
        }

        // Convierte la lista en un conjunto para Libro.autores.
        Set<Autor> autores = new HashSet<>(autoresEncontrados);

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
         * Elimina espacios exteriores y utiliza null cuando
         * el administrador no proporciona información.
         */

        // Normaliza la sinopsis.
        String sinopsis = formulario.getSinopsis();

        if (sinopsis != null) {
            sinopsis = sinopsis.trim();

            if (sinopsis.isEmpty()) {
                sinopsis = null;
            }
        }

        libro.setSinopsis(sinopsis);

        // Normaliza la dirección de la portada.
        String portadaUrl = formulario.getPortadaUrl();

        if (portadaUrl != null) {
            portadaUrl = portadaUrl.trim();

            if (portadaUrl.isEmpty()) {
                portadaUrl = null;
            }
        }

        libro.setPortadaUrl(portadaUrl);

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
}
