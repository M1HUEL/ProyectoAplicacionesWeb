
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.LibroListadoDTO;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import com.itson.ProyectoAplicacionesWeb.model.ItemStock;
import com.itson.ProyectoAplicacionesWeb.model.Libro;
import com.itson.ProyectoAplicacionesWeb.repository.FormatoLibroRepository;
import com.itson.ProyectoAplicacionesWeb.repository.ItemStockRepository;
import com.itson.ProyectoAplicacionesWeb.repository.LibroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
     * Recibe los repositorios mediante inyección
     * de dependencias de Spring.
     *
     * @param libroRepository repositorio de libros.
     * @param formatoLibroRepository repositorio de presentaciones.
     * @param itemStockRepository repositorio de inventario.
     */
    public LibroService(
            LibroRepository libroRepository,
            FormatoLibroRepository formatoLibroRepository,
            ItemStockRepository itemStockRepository) {

        this.libroRepository = libroRepository;
        this.formatoLibroRepository = formatoLibroRepository;
        this.itemStockRepository = itemStockRepository;
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

}
