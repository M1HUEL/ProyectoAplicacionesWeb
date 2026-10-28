
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LibroFormularioDTO;
import com.itson.ProyectoAplicacionesWeb.service.AutorService;
import com.itson.ProyectoAplicacionesWeb.service.CategoriaService;
import com.itson.ProyectoAplicacionesWeb.service.EditorialService;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        /*
         * Prepara un formulario vacío solamente cuando
         * no existen datos recuperados de una redirección.
         *
         * Si ocurrió un error de validación, conserva el
         * LibroFormularioDTO enviado mediante FlashAttributes.
         */
        if (!model.containsAttribute("libroFormulario")) {
            model.addAttribute(
                    "libroFormulario",
                    new LibroFormularioDTO()
            );
        }

        // Recupera los registros disponibles para el formulario.
        model.addAttribute("autores", autorService.listarTodos());
        model.addAttribute("editoriales", editorialService.listarTodas());

        // Solamente permite seleccionar categorías activas.
        model.addAttribute("categorias", categoriaService.listarActivas());

        /*
         * Proporciona los formatos comerciales disponibles
         * para el registro de libros.
         *
         * Se utilizan los valores definidos en el enum Formato:
         * PASTA_DURA, PASTA_BLANDA y DIGITAL.
         */
        model.addAttribute("formatos", Formato.values());


        // Devuelve la plantilla que ya existe en el proyecto.
        return "administrador-libros-formulario";
    }

    /**
     * Procesa el registro administrativo de un nuevo libro.
     *
     * Delega las validaciones y la persistencia a LibroService.
     * Utiliza el patrón POST/Redirect/GET.
     *
     * @param formulario datos recibidos desde Thymeleaf.
     * @param redirectAttributes mensajes y datos conservados durante la redirección.
     * @return ruta de redirección según el resultado.
     */
    @PostMapping
    public String registrarLibro(
            @ModelAttribute("libroFormulario") LibroFormularioDTO formulario,
            RedirectAttributes redirectAttributes) {

        try {
            // Delega el registro a la capa Service.
            libroService.registrar(formulario);

            // Comunica el registro exitoso al listado.
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Libro registrado correctamente."
            );

            // Aplica el patrón POST/Redirect/GET.
            return "redirect:/admin/libros";

        } catch (IllegalArgumentException excepcion) {

            // Conserva los datos para que el usuario pueda corregirlos.
            redirectAttributes.addFlashAttribute(
                    "libroFormulario",
                    formulario
            );

            // Comunica el motivo del rechazo.
            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/admin/libros/nuevo";
        }
    }

}
