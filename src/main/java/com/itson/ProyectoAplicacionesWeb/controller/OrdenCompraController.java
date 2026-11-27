
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.model.EstadoOrden;
import com.itson.ProyectoAplicacionesWeb.service.OrdenCompraService;
import com.itson.ProyectoAplicacionesWeb.service.PagoService;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasRole('ADMIN')")
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;
    private final PagoService pagoService;

    /**
     * Constructor para la inyección de los servicios
     * utilizados por el módulo administrativo de órdenes.
     */
    public OrdenCompraController(
            OrdenCompraService ordenCompraService,
            PagoService pagoService) {

        this.ordenCompraService = ordenCompraService;
        this.pagoService = pagoService;
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
     * Presenta el detalle administrativo de una orden,
     * incluyendo los artículos comprados, los datos
     * históricos de entrega y el registro de pago.
     *
     * GET /admin/ordenes/{ordenId}/detalle
     *
     * El acceso está restringido a administradores.
     *
     * @param ordenId identificador de la orden solicitada.
     * @param model modelo de datos para Thymeleaf.
     * @return plantilla del detalle administrativo.
     */
    @GetMapping("/{ordenId}/detalle")
    public String verDetalle(
            @PathVariable Long ordenId,
            Model model) {

        // Recuperar la orden de compra existente.
        model.addAttribute(
                "orden",
                ordenCompraService.buscarPorId(ordenId)
        );

        // Recuperar el registro de pago, si existe.
        model.addAttribute(
                "pago",
                pagoService.consultarPagoPorOrden(ordenId)
                        .orElse(null)
        );

        return "detalle-pedido";
    }

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
