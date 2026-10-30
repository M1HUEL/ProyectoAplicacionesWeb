
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.model.EstadoOrden;
import com.itson.ProyectoAplicacionesWeb.service.OrdenCompraService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador del módulo administrativo de órdenes
 * de Librería Raudskinna.
 */
@Controller
@RequestMapping("/admin/ordenes")
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    // Inyección del servicio mediante constructor.
    public OrdenCompraController(OrdenCompraService ordenCompraService) {
        this.ordenCompraService = ordenCompraService;
    }

    /**
     * Muestra el listado administrativo de órdenes con filtros.
     */
    @GetMapping
    public String listarOrdenes(@RequestParam(required = false) EstadoOrden estado,
                               @RequestParam(required = false) String fechaDesde,
                               @RequestParam(required = false) String fechaHasta,
                               @RequestParam(required = false) String cliente,
                               Model model) {

        model.addAttribute(
                "ordenes",
                ordenCompraService.filtrar(estado, fechaDesde, fechaHasta, cliente)
        );

        model.addAttribute("estados", EstadoOrden.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        model.addAttribute("clienteFiltro", cliente);

        return "administrador-ordenes";
    }

    /**
     * Actualiza el estado de una orden desde el panel administrativo.
     *
     * Utiliza POST/Redirect/GET para evitar el reenvío
     * del formulario al actualizar la página.
     */
    @PostMapping("/{ordenId}/estado")
    public String actualizarEstado(
            @PathVariable Long ordenId,
            @RequestParam EstadoOrden nuevoEstado,
            RedirectAttributes redirectAttributes) {

        try {
            ordenCompraService.actualizarEstado(ordenId, nuevoEstado);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Estado de la orden actualizado correctamente."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/ordenes";
    }

}
