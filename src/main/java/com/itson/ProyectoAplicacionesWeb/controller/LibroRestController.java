
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.FormatoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroDetallePublicoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.PaginaCatalogoDTO;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST para consultar el catálogo público
 * de Librería Raudskinna.
 */
@RestController
@RequestMapping("/api/libros")
public class LibroRestController {

    private final LibroService libroService;

    public LibroRestController(LibroService libroService) {
        this.libroService = libroService;
    }

    /**
     * Devuelve una página del catálogo público en formato JSON.
     *
     * GET /api/libros?q=&categoria=&page=0&size=12
     *
     * @param q término opcional a buscar en título o autor.
     * @param categoria filtro opcional por nombre de categoría.
     * @param page índice de la página, iniciando en cero.
     * @param size cantidad máxima de libros por página.
     * @return página solicitada con su resumen comercial.
     */
    @GetMapping
    public PaginaCatalogoDTO listarLibros(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return libroService.listarCatalogoPaginado(
                q,
                categoria,
                page,
                size
        );
    }

    /**
     * Consulta el detalle de un libro activo.
     *
     * GET /api/libros/{id}
     *
     * @param id identificador del libro.
     * La respuesta incluye las presentaciones comerciales con
     * su identificador, precio y disponibilidad.
     *
     * @return detalle público del libro con sus formatos.
     */
    @GetMapping("/{id}")
    public LibroDetallePublicoDTO consultarLibro(@PathVariable Long id) {
        return libroService.consultarDetalleLibroPublico(id);
    }

    /**
     * Consulta las presentaciones comerciales de un libro activo.
     *
     * Devuelve el formato, precio y disponibilidad de cada
     * presentación, utilizando las reglas definidas en LibroService.
     *
     * GET /api/libros/{id}/formatos
     *
     * @param id identificador del libro.
     * @return presentaciones comerciales disponibles para consulta pública.
     */
    @GetMapping("/{id}/formatos")
    public List<FormatoPublicoDTO> consultarFormatos(@PathVariable Long id) {
        return libroService.listarFormatosPublicos(id);
    }

}
