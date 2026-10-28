
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.EditorialService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador para la administración de editoriales.
 *
 * Gestiona las solicitudes HTTP del módulo y delega
 * las operaciones de negocio a EditorialService.
 *
 */
@Controller
@RequestMapping("/admin/editoriales")
public class EditorialController {

    private final EditorialService editorialService;

    /**
     * Inyecta el servicio de editoriales.
     *
     * @param editorialService servicio de gestión de editoriales.
     */
    public EditorialController(EditorialService editorialService) {
        this.editorialService = editorialService;
    }

    /**
     * Muestra el listado administrativo de editoriales.
     *
     * @param model modelo utilizado por Thymeleaf.
     * @return nombre de la plantilla administrativa.
     */
    @GetMapping
    public String listar(Model model) {

        // Obtiene las editoriales mediante la capa Service.
        model.addAttribute(
                "editoriales",
                editorialService.listarTodas()
        );

        return "administrador-editoriales";
    }

    /**
     * Muestra el formulario administrativo para registrar
     * una nueva editorial en Librería Raudskinna.
     *
     * @return nombre de la plantilla Thymeleaf
     *         correspondiente al formulario.
     *
     */
    @GetMapping("/nueva")
    public String mostrarFormularioRegistro() {

        // Presenta el formulario para capturar una editorial.
        // El registro se realizará posteriormente mediante POST.
        return "administrador-editoriales-formulario";
    }

    /**
     * Procesa el registro administrativo de una editorial.
     *
     * Delega las validaciones y la persistencia a EditorialService.
     * Utiliza el patrón POST/Redirect/GET.
     *
     * @param nombre nombre de la editorial.
     * @param direccion dirección opcional de la editorial.
     * @param redirectAttributes atributos para mostrar mensajes
     *                           después de la redirección.
     * @return ruta de redirección según el resultado.
     *
     */
    @PostMapping
    public String registrar(
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "direccion", required = false) String direccion,
            RedirectAttributes redirectAttributes) {

        try {
            // Delega las reglas de negocio y el registro al Service.
            editorialService.registrar(nombre, direccion);

            // Prepara el mensaje de confirmación.
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Editorial registrada correctamente."
            );

            // Evita el reenvío del formulario al actualizar la página.
            return "redirect:/admin/editoriales";

        } catch (IllegalArgumentException excepcion) {

            // Conserva los datos introducidos para corregirlos.
            redirectAttributes.addFlashAttribute("nombreAnterior", nombre);
            redirectAttributes.addFlashAttribute("direccionAnterior", direccion);

            // Comunica el motivo del rechazo.
            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/admin/editoriales/nueva";
        }
    }

}
