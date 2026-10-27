
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.model.Categoria;
import com.itson.ProyectoAplicacionesWeb.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

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

    /**
     * Desactiva una categoría mediante HTTP DELETE.
     *
     * Realiza una eliminación lógica (soft delete).
     * No elimina físicamente el registro de MySQL.
     *
     * @param id Identificador de la categoría.
     * @return Confirmación de la operación.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarCategoria(
            @PathVariable Long id) {

        try {
            categoriaService.desactivar(id);

            return ResponseEntity.ok(
                    Map.of(
                            "mensaje",
                            "Categoría desactivada correctamente."
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Desactiva una categoría desde el formulario administrativo.
     *
     * Delega la eliminación lógica a CategoriaService y utiliza
     * POST/Redirect/GET para regresar al listado.
     */
    @PostMapping("/{id}/desactivar")
    public String desactivarDesdeFormulario(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            categoriaService.desactivar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Categoría desactivada correctamente."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/categorias";
    }


}
