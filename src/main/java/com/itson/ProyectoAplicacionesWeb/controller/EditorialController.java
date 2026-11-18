
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.EditorialService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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
@PreAuthorize("hasRole('ADMIN')")
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

    /**
     * Muestra el formulario para editar una editorial existente.
     *
     * @param id identificador de la editorial.
     * @param model modelo utilizado por Thymeleaf.
     * @param redirectAttributes mensajes para la redirección.
     * @return formulario de edición o listado si hay un error.
     */
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Recupera la editorial mediante la capa Service.
            var editorial = editorialService.buscarPorId(id);

            // Proporciona sus datos al formulario.
            model.addAttribute("editorial", editorial);

            return "administrador-editoriales-formulario";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/admin/editoriales";
        }
    }

    /**
     * Procesa la actualización de una editorial existente.
     *
     * @param id identificador de la editorial.
     * @param nombre nuevo nombre.
     * @param direccion nueva dirección, opcional.
     * @param redirectAttributes mensajes para la redirección.
     * @return listado o formulario de edición.
     */
    @PostMapping("/{id}/editar")
    public String actualizar(
            @PathVariable("id") Long id,
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "direccion", required = false) String direccion,
            RedirectAttributes redirectAttributes) {

        try {
            editorialService.actualizar(id, nombre, direccion);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Editorial actualizada correctamente."
            );

            return "redirect:/admin/editoriales";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            // Conserva los datos capturados cuando falla la validación.
            redirectAttributes.addFlashAttribute(
                    "nombreAnterior",
                    nombre
            );

            redirectAttributes.addFlashAttribute(
                    "direccionAnterior",
                    direccion
            );

            return "redirect:/admin/editoriales/" + id + "/editar";
        }
    }

    /**
     * Elimina una editorial cuando no tiene libros asociados.
     *
     * Utiliza POST y conserva la protección CSRF
     * de los formularios Thymeleaf.
     *
     * @param id identificador de la editorial.
     * @param redirectAttributes mensajes para el usuario.
     * @return redirección al listado administrativo.
     */
    @PostMapping("/{id}/eliminar")
    public String eliminar(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            editorialService.eliminar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Editorial eliminada correctamente."
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/admin/editoriales";
    }
}
