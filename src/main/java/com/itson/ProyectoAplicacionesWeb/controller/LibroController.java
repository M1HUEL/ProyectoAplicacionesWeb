
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador MVC encargado de atender las solicitudes
 * relacionadas con la administración de libros.
 *
 * Recibe las peticiones HTTP, delega las operaciones
 * al servicio y proporciona los datos a Thymeleaf.
 *
 */
@Controller
@RequestMapping("/admin/libros")
public class LibroController {

    /** Servicio encargado de consultar los libros. */
    private final LibroService libroService;

    /**
     * Constructor que recibe el servicio mediante
     * inyección de dependencias de Spring.
     *
     * @param libroService servicio de libros.
     */
    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    /**
     * Muestra el listado administrativo de libros.
     *
     * @param model objeto utilizado para enviar datos
     *              desde el controlador hacia Thymeleaf.
     * @return nombre de la plantilla administrativa.
     */
    @GetMapping
    public String listarLibros(Model model) {

        // Recupera los libros registrados mediante el servicio.
        model.addAttribute("libros", libroService.listarTodos());

        // Indica a Spring MVC qué plantilla debe renderizar.
        return "administrador-libros";
    }
}
