package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.CatalogoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del catálogo público con filtros completos.
 *
 * A diferencia de {@code /api/libros}, esta ruta devuelve también
 * las opciones disponibles de categorías y autores, además de
 * aplicar búsqueda, categoría, autor y ordenamiento.
 */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoRestController {

    private final LibroService libroService;

    public CatalogoRestController(LibroService libroService) {
        this.libroService = libroService;
    }

    /**
     * GET /api/catalogo?q=&categoria=&autor=&orden=
     *
     * El parámetro {@code busqueda} se acepta también por
     * compatibilidad con el controlador MVC existente.
     */
    @GetMapping
    public CatalogoPublicoDTO consultar(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "busqueda", required = false)
            String busqueda,
            @RequestParam(name = "categoria", required = false)
            String categoria,
            @RequestParam(name = "autor", required = false)
            String autor,
            @RequestParam(name = "orden", defaultValue = "relevancia")
            String orden) {

        String termino = (q != null && !q.isBlank()) ? q : busqueda;

        return libroService.consultarCatalogoPublico(
                termino,
                categoria,
                autor,
                orden
        );
    }
}
