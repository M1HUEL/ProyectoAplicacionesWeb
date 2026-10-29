
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LibroFormularioDTO;
import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import com.itson.ProyectoAplicacionesWeb.model.ItemStock;
import com.itson.ProyectoAplicacionesWeb.service.AutorService;
import com.itson.ProyectoAplicacionesWeb.service.CategoriaService;
import com.itson.ProyectoAplicacionesWeb.service.EditorialService;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import org.springframework.web.bind.annotation.*;
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

    /**
     * Atiende la solicitud para editar una presentación
     * comercial de un libro existente.
     *
     * Comprueba que la presentación solicitada
     * realmente pertenezca al libro indicado.
     *
     * @param libroId identificador del libro.
     * @param formatoId identificador de la presentación.
     * @param model datos enviados a Thymeleaf.
     * @return vista del formulario administrativo.
     */
    @GetMapping("/{libroId}/formatos/{formatoId}/editar")
    public String mostrarFormularioEdicion(
            @PathVariable Long libroId,
            @PathVariable Long formatoId,
            Model model) {

        /*
         * Recupera la presentación comercial y verifica
         * que pertenezca al libro solicitado.
         */
        FormatoLibro formatoSeleccionado =
                libroService.buscarFormatoDeLibro(libroId, formatoId);

        /*
         * Conserva los datos capturados cuando la edición
         * regresa al formulario por un error de validación.
         *
         * Si no existen datos temporales, recupera la
         * información original del libro.
         */
        LibroFormularioDTO formulario;

        if (model.containsAttribute("libroFormulario")) {

            formulario = (LibroFormularioDTO)
                    model.getAttribute("libroFormulario");

        } else {

            formulario = libroService.prepararFormularioEdicion(libroId);
        }

        /*
         * Conserva los datos capturados si el formulario
         * regresa después de un error de validación.
         *
         * En la primera apertura, carga los valores
         * comerciales existentes en MySQL.
         */
        boolean formularioRecuperado =
                model.containsAttribute("error");

        if (!formularioRecuperado) {

            // Carga el precio original de la presentación.
            formulario.setPrecio(formatoSeleccionado.getPrecio());

            // Recupera las existencias únicamente para formatos físicos.
            if (formatoSeleccionado.getFormato() != Formato.DIGITAL) {

                ItemStock inventario = libroService
                        .buscarStockPorFormato(formatoId)
                        .orElseThrow(() -> new IllegalStateException(
                                "La presentación física no tiene inventario registrado."
                        ));

                formulario.setCantidad(inventario.getCantidad());
                formulario.setStockMinimo(inventario.getStockMinimo());
            }
        }

        // El formato siempre proviene de la presentación existente,
        // porque el select está deshabilitado durante la edición.
        formulario.setFormato(formatoSeleccionado.getFormato());


        // Envía el formulario a Thymeleaf.
        model.addAttribute("libroFormulario", formulario);

        // Conserva los identificadores para la edición.
        model.addAttribute("libroId", libroId);
        model.addAttribute("formatoId", formatoId);

        // Catálogos necesarios para los campos desplegables.
        model.addAttribute("autores", autorService.listarTodos());
        model.addAttribute("editoriales", editorialService.listarTodas());
        model.addAttribute("categorias", categoriaService.listarActivas());
        model.addAttribute("formatos", Formato.values());

        return "administrador-libros-formulario";
    }

    /**
     * Procesa la actualización administrativa de un libro
     * y una de sus presentaciones comerciales.
     *
     * Utiliza PRG (Post/Redirect/Get) para evitar
     * reenvíos accidentales del formulario.
     *
     * @param libroId identificador del libro.
     * @param formatoId identificador de la presentación.
     * @param formulario datos recibidos del administrador.
     * @param redirectAttributes mensajes y datos temporales.
     * @return redirección al listado o al formulario.
     */
    @PostMapping("/{libroId}/formatos/{formatoId}")
    public String actualizarLibro(
            @PathVariable Long libroId,
            @PathVariable Long formatoId,
            @ModelAttribute("libroFormulario") LibroFormularioDTO formulario,
            RedirectAttributes redirectAttributes) {

        try {
            // Delega las validaciones y modificaciones al servicio.
            libroService.actualizar(libroId, formatoId, formulario);

            // Confirma la operación al administrador.
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Libro actualizado correctamente."
            );

            return "redirect:/admin/libros";

        } catch (IllegalArgumentException e) {

            // Conserva los datos capturados para corregirlos.
            redirectAttributes.addFlashAttribute(
                    "libroFormulario",
                    formulario
            );

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/admin/libros/"
                    + libroId
                    + "/formatos/"
                    + formatoId
                    + "/editar";
        }
    }

}
