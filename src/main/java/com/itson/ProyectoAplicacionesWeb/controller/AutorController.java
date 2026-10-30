
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.AutorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

}
