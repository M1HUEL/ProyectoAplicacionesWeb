
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.AutorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador de la administración de autores.
 *
 * Atiende las solicitudes HTTP del módulo y delega
 * las operaciones de negocio a AutorService.
 *
 */
@Controller
@RequestMapping("/admin/autores")
@PreAuthorize("hasRole('ADMIN')")
public class AutorController {

    private final AutorService autorService;

    /**
     * Inyección del servicio de autores.
     *
     * @param autorService servicio encargado de gestionar autores.
     */
    public AutorController(AutorService autorService) {
        this.autorService = autorService;
    }

    /**
     * Muestra el listado administrativo de autores.
     *
     * @param model modelo utilizado por Thymeleaf.
     * @return nombre de la plantilla del listado.
     */
    @GetMapping
    public String listar(Model model) {

        // Obtiene los autores mediante la capa Service.
        model.addAttribute("autores", autorService.listarTodos());

        return "administrador-autores";
    }


    /**
     * Muestra el formulario administrativo para registrar
     * un nuevo autor en Librería Raudskinna.
     *
     * @return nombre de la plantilla Thymeleaf del formulario.
     *
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioRegistro() {

        // El formulario todavía no necesita consultar MySQL.
        return "administrador-autores-formulario";
    }

    /**
     * Procesa el registro administrativo de un autor.
     *
     * Delega la validación y persistencia a AutorService
     * y aplica el patrón POST/Redirect/GET.
     *
     * @param nombre nombre recibido desde el formulario.
     * @param redirectAttributes atributos para mostrar mensajes
     *                           después de la redirección.
     * @return ruta a la que se redirige al usuario.
     *
     */
    @PostMapping
    public String registrar(
            @RequestParam("nombre") String nombre,
            RedirectAttributes redirectAttributes) {

        try {
            // Toda la lógica de negocio permanece en Service.
            autorService.registrar(nombre);

            // Mensaje disponible después de la redirección.
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Autor registrado correctamente."
            );

            // Patrón POST/Redirect/GET.
            return "redirect:/admin/autores";

        } catch (IllegalArgumentException excepcion) {

            // Conserva el nombre ingresado para corregirlo.
            redirectAttributes.addFlashAttribute(
                    "nombreAnterior",
                    nombre
            );

            // Envía el mensaje de validación al formulario.
            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/admin/autores/nuevo";
        }
    }

    /**
     * Muestra el formulario de edición de un autor existente.
     *
     * @param id identificador del autor.
     * @param model modelo utilizado por Thymeleaf.
     * @param redirectAttributes mensajes de redirección.
     * @return formulario de edición o listado si hay un error.
     */
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Recupera el autor mediante la capa Service.
            var autor = autorService.buscarPorId(id);

            // Proporciona los datos existentes al formulario.
            model.addAttribute("autor", autor);

            return "administrador-autores-formulario";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/admin/autores";
        }
    }

    /**
     * Procesa la actualización de un autor existente.
     *
     * @param id identificador del autor.
     * @param nombre nombre recibido desde el formulario.
     * @param redirectAttributes mensajes para la redirección.
     * @return listado de autores o formulario de edición.
     */
    @PostMapping("/{id}/editar")
    public String actualizar(
            @PathVariable("id") Long id,
            @RequestParam("nombre") String nombre,
            RedirectAttributes redirectAttributes) {

        try {
            autorService.actualizar(id, nombre);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Autor actualizado correctamente."
            );

            return "redirect:/admin/autores";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            redirectAttributes.addFlashAttribute(
                    "nombreAnterior",
                    nombre
            );

            return "redirect:/admin/autores/" + id + "/editar";
        }
    }

    /**
     * Elimina un autor si no tiene libros asociados.
     *
     * Utiliza POST para recibir la solicitud desde
     * un formulario Thymeleaf protegido mediante CSRF.
     *
     * @param id identificador del autor.
     * @param redirectAttributes mensajes para el usuario.
     * @return redirección al listado de autores.
     */
    @PostMapping("/{id}/eliminar")
    public String eliminar(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            autorService.eliminar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Autor eliminado correctamente."
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/admin/autores";
    }
}
