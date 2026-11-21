
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador MVC del catálogo público de Librería Raudskinna.
 *
 * Permite a los visitantes consultar la información bibliográfica
 * y comercial de los libros sin necesidad de iniciar sesión.
 *
 * Las reglas de negocio y las validaciones de disponibilidad
 * permanecen en LibroService.
 */
@Controller
public class CatalogoController {

    /**
     * Servicio encargado de las consultas públicas del catálogo.
     */
    private final LibroService libroService;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param libroService servicio de consulta de libros.
     */
    public CatalogoController(LibroService libroService) {
        this.libroService = libroService;
    }


    /**
     * Presenta el catálogo público de Librería Raudskinna.
     *
     * Permite consultar todos los libros activos o realizar
     * búsquedas mediante coincidencias en el título o autor.
     *
     * La lógica de búsqueda, los precios y la disponibilidad
     * comercial permanecen en LibroService.
     *
     * GET /catalogo
     * GET /catalogo?busqueda=termino
     *
     * @param busqueda término opcional introducido por el visitante.
     * @param model modelo que transporta los datos a Thymeleaf.
     * @return nombre de la plantilla del catálogo público.
     */
    @GetMapping("/catalogo")
    public String mostrarCatalogo(
            @RequestParam(name = "busqueda", required = false)
            String busqueda,
            Model model) {

        // Recuperar el catálogo aplicando el término de búsqueda.
        model.addAttribute(
                "libros",
                libroService.buscarCatalogoPublico(busqueda)
        );

        // Conservar el término introducido por el visitante.
        model.addAttribute("busqueda", busqueda);

        // Presentar la página del catálogo público.
        return "catalogo";
    }

    /**
     * Presenta la información detallada de un libro activo
     * junto con sus formatos, precios y disponibilidad.
     *
     * Los libros inactivos o inexistentes serán rechazados
     * mediante las validaciones realizadas en LibroService.
     *
     * GET /detalle-libro/{id}
     *
     * @param id identificador del libro solicitado.
     * @param model modelo que transporta los datos a Thymeleaf.
     * @return nombre de la plantilla de detalle del libro.
     */
    @GetMapping("/detalle-libro/{id}")
    public String mostrarDetalleLibro(
            @PathVariable Long id,
            Model model) {

        // Recuperar la información bibliográfica pública.
        model.addAttribute(
                "libro",
                libroService.consultarLibroPublico(id)
        );

        // Recuperar los formatos comerciales y su disponibilidad.
        model.addAttribute(
                "formatos",
                libroService.listarFormatosPublicos(id)
        );

        // Presentar la página de detalle del libro.
        return "detalle-libro";
    }
}
