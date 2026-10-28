
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LibroFormularioDTO;
import com.itson.ProyectoAplicacionesWeb.service.AutorService;
import com.itson.ProyectoAplicacionesWeb.service.CategoriaService;
import com.itson.ProyectoAplicacionesWeb.service.EditorialService;
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


    /** Servicio encargado de gestionar los libros. */
    private final LibroService libroService;

    /** Servicio encargado de consultar los autores. */
    private final AutorService autorService;

    /** Servicio encargado de consultar las editoriales. */
    private final EditorialService editorialService;

    /** Servicio encargado de consultar las categorías. */
    private final CategoriaService categoriaService;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param libroService servicio de libros.
     * @param autorService servicio de autores.
     * @param editorialService servicio de editoriales.
     * @param categoriaService servicio de categorías.
     */
    public LibroController(
            LibroService libroService,
            AutorService autorService,
            EditorialService editorialService,
            CategoriaService categoriaService) {

        this.libroService = libroService;
        this.autorService = autorService;
        this.editorialService = editorialService;
        this.categoriaService = categoriaService;
    }

    /**
     * Muestra el listado administrativo de libros,
     * incluyendo sus presentaciones comerciales,
     * precios y estados de inventario.
     *
     * @param model objeto utilizado para enviar
     *              los datos hacia Thymeleaf.
     * @return nombre de la plantilla administrativa.
     *
     */
    @GetMapping
    public String listarLibros(Model model) {

        // Obtiene las presentaciones comerciales
        // preparadas por la capa de servicios.
        model.addAttribute(
                "libros",
                libroService.listarParaAdministracion()
        );

        // Devuelve la plantilla administrativa.
        return "administrador-libros";
    }


    /**
     * Prepara y muestra el formulario para registrar
     * un nuevo libro en Librería Raudskinna.
     *
     * Envía las opciones necesarias para seleccionar
     * autores, editorial y categoría.
     *
     * @param model datos enviados a la vista Thymeleaf.
     * @return plantilla del formulario administrativo.
     *
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {

        // Prepara un DTO vacío para capturar los datos del libro.
        model.addAttribute("libroFormulario", new LibroFormularioDTO());

        // Recupera los registros disponibles para el formulario.
        model.addAttribute("autores", autorService.listarTodos());
        model.addAttribute("editoriales", editorialService.listarTodas());

        // Solamente permite seleccionar categorías activas.
        model.addAttribute("categorias", categoriaService.listarActivas());

        // Devuelve la plantilla que ya existe en el proyecto.
        return "administrador-libros-formulario";
    }

}
