package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.AjusteInventarioDTO;
import com.itson.ProyectoAplicacionesWeb.model.MotivoAjuste;
import com.itson.ProyectoAplicacionesWeb.service.InventarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/inventario")
public class InventarioController {
    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    /**
     * Muestra el formulario de ajuste y el historial de movimientos.
     */
    @GetMapping("/formatos/{formatoId}/ajuste")
    public String mostrarAjuste(
            @PathVariable Long formatoId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            model.addAttribute(
                    "inventario",
                    inventarioService.buscarInventarioConLibro(formatoId)
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/libros";
        }

        if (!model.containsAttribute("ajusteFormulario")) {
            model.addAttribute("ajusteFormulario", new AjusteInventarioDTO());
        }

        model.addAttribute("formatoId", formatoId);
        model.addAttribute("motivos", MotivoAjuste.values());
        model.addAttribute(
                "movimientos",
                inventarioService.listarMovimientos(formatoId)
        );

        return "administrador-inventario-ajuste";
    }

    /**
     * Registra el ajuste y vuelve a la pantalla con el resultado.
     */
    @PostMapping("/formatos/{formatoId}/ajuste")
    public String registrarAjuste(
            @PathVariable Long formatoId,
            @ModelAttribute("ajusteFormulario") AjusteInventarioDTO formulario,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            inventarioService.ajustar(
                    formatoId,
                    formulario,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Ajuste de inventario registrado correctamente."
            );

        } catch (IllegalArgumentException e) {
            // Conserva lo capturado para que el administrador lo corrija.
            redirectAttributes.addFlashAttribute("ajusteFormulario", formulario);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/inventario/formatos/" + formatoId + "/ajuste";
    }
}
