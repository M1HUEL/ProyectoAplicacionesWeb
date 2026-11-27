
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la página principal de Librería Raudskinna.
 *
 * Separamos el inicio público del panel administrativo para mantener
 * organizadas las responsabilidades de la aplicación.
 */
@Controller
public class InicioController {

    /**
     * Servicio encargado de consultar los libros del catálogo.
     */
    private final LibroService libroService;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param libroService servicio de consulta de libros.
     */
    public InicioController(LibroService libroService) {
        this.libroService = libroService;
    }

    /**
     * Presenta la página principal de la librería.
     *
     * Esta ruta será pública para permitir que cualquier visitante
     * pueda conocer Raudskinna sin iniciar sesión.
     *
     * Recupera los libros activos mediante LibroService y los
     * proporciona a la plantilla Thymeleaf a través del modelo.
     *
     * @param model modelo que transporta los datos hacia la vista.
     * @return nombre de la plantilla de inicio.
     */
    @GetMapping("/")
    public String mostrarInicio(Model model) {

        // Recuperar los libros activos con su resumen comercial.
        // LibroService calcula el menor precio disponible
        // y determina si cada libro puede ofrecerse.
        model.addAttribute(
                "libros",
                libroService.listarCatalogoPublico()
        );

        // Presentar la página principal de la librería.
        return "index";
    }
}
