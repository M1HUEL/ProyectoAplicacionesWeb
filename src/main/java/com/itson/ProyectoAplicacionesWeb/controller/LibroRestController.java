
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LibroPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
     * Devuelve los libros activos en formato JSON.
     *
     * GET /api/libros
     */
    @GetMapping
    public List<LibroPublicoDTO> listarLibros() {
        return libroService.listarLibrosPublicos();
    }
}
