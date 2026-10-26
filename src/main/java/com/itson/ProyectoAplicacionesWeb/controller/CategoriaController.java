
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.model.Categoria;
import com.itson.ProyectoAplicacionesWeb.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador MVC para administrar las categorías
 * del catálogo de Librería Raudskinna.
 *
 * Recibe las solicitudes HTTP y delega las operaciones
 * a CategoriaService, sin implementar reglas de negocio.
 *
 * Todas sus rutas quedan protegidas mediante Spring Security
 * y requieren el rol ADMIN.
 */
@Controller
@RequestMapping("/admin/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /**
     * Muestra todas las categorías registradas.
     */
    @GetMapping
    public String listarCategorias(Model model) {
        model.addAttribute(
                "categorias",
                categoriaService.listarTodas()
        );

        return "admin/categorias";
    }

    /**
     * Muestra una categoría para su posterior edición.
     *
     * La búsqueda y el manejo de datos corresponden
     * a la capa Service.
     */
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "categoria",
                categoriaService.buscarPorId(id)
        );

        return "admin/categoria-formulario";
    }

    /**
     * Presenta el formulario para registrar una nueva categoría.
     */
    @GetMapping("/nueva")
    public String mostrarFormularioRegistro() {
        return "admin/categoria-formulario";
    }

    /**
     * Procesa el registro de una categoría.
     *
     * Delega las validaciones y persistencia a CategoriaService.
     * Utiliza POST/Redirect/GET para evitar reenvíos accidentales.
     */
    @PostMapping
    public String registrarCategoria(
            @RequestParam String nombre,
            RedirectAttributes redirectAttributes) {

        try {
            categoriaService.registrar(nombre);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Categoría registrada correctamente."
            );

            return "redirect:/admin/categorias";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            redirectAttributes.addFlashAttribute(
                    "nombreAnterior",
                    nombre
            );

            return "redirect:/admin/categorias/nueva";
        }
    }

    /**
     * Procesa la actualización de una categoría existente.
     *
     * Delega las validaciones y la persistencia a CategoriaService.
     * Utiliza POST/Redirect/GET para evitar reenvíos del formulario.
     */
    @PostMapping("/{id}/editar")
    public String actualizarCategoria(
            @PathVariable Long id,
            @RequestParam String nombre,
            RedirectAttributes redirectAttributes) {

        try {
            categoriaService.actualizar(id, nombre);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Categoría actualizada correctamente."
            );

            return "redirect:/admin/categorias";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            redirectAttributes.addFlashAttribute(
                    "nombreAnterior",
                    nombre
            );

            return "redirect:/admin/categorias/" + id + "/editar";
        }
    }


    /**
     * Actualiza parcialmente una categoría mediante HTTP PATCH.
     *
     * Recibe el nombre en formato JSON y delega las
     * validaciones y la actualización a CategoriaService.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizarParcialmente(
            @PathVariable Long id,
            @RequestBody Map<String, String> cambios) {

        if (!cambios.containsKey("nombre")) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Debe proporcionar el campo nombre.")
            );
        }

        try {
            Categoria categoria = categoriaService.actualizar(
                    id,
                    cambios.get("nombre")
            );

            return ResponseEntity.ok(
                    Map.of(
                            "id", categoria.getId(),
                            "nombre", categoria.getNombre(),
                            "mensaje", "Categoría actualizada correctamente."
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }


}
