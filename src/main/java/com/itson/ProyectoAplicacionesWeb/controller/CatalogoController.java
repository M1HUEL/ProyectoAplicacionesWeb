
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.CatalogoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;

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
     * Permite realizar búsquedas bibliográficas, aplicar
     * filtros por categoría y autor, y ordenar los
     * resultados mediante diferentes criterios.
     *
     * Las opciones dinámicas de filtrado se obtienen
     * del catálogo público completo.
     *
     * Toda la lógica de búsqueda, filtrado y ordenamiento
     * permanece centralizada en LibroService.
     *
     * @param busqueda término opcional de búsqueda.
     * @param categoria categoría seleccionada.
     * @param autor autor seleccionado.
     * @param orden criterio de ordenamiento seleccionado.
     * @param model modelo que transporta los datos a Thymeleaf.
     * @return nombre de la plantilla del catálogo público.
     */
    @GetMapping("/catalogo")
    public String mostrarCatalogo(
            @RequestParam(name = "busqueda", required = false)
            String busqueda,

            @RequestParam(name = "categoria", required = false)
            String categoria,

            @RequestParam(name = "autor", required = false)
            String autor,

            @RequestParam(name = "orden", defaultValue = "relevancia")
            String orden,

            Model model) {

        // Consultar los resultados y las opciones de filtrado.
        CatalogoPublicoDTO catalogo =
                libroService.consultarCatalogoPublico(
                        busqueda,
                        categoria,
                        autor,
                        orden
                );

        // Transportar los libros filtrados y ordenados.
        model.addAttribute("libros", catalogo.libros());

        // Transportar las opciones dinámicas de filtrado.
        model.addAttribute("categorias", catalogo.categorias());
        model.addAttribute("autores", catalogo.autores());

        // Conservar los criterios seleccionados por el visitante.
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("categoria", categoria);
        model.addAttribute("autor", autor);
        model.addAttribute("orden", orden);

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
     * Identifica si el visitante cuenta con una sesión
     * autenticada para presentar las opciones de compra
     * correspondientes.
     *
     * GET /detalle-libro/{id}
     *
     * @param id identificador del libro solicitado.
     * @param model modelo que transporta los datos a Thymeleaf.
     * @param principal identidad autenticada del visitante,
     *                  o null si no ha iniciado sesión.
     * @return nombre de la plantilla de detalle del libro.
     */
    @GetMapping("/detalle-libro/{id}")
    public String mostrarDetalleLibro(
            @PathVariable Long id,
            Model model,
            Principal principal) {

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

        // Informar a la vista si el visitante
        // cuenta con una sesión autenticada.
        model.addAttribute(
                "clienteAutenticado",
                principal != null
        );

        // Presentar la página de detalle del libro.
        return "detalle-libro";
    }

}
