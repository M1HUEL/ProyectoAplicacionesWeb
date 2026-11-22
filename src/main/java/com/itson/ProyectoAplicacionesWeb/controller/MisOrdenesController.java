
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.OrdenCompraService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador del historial de compras del cliente
 * de Librería Raudskinna.
 *
 * Permite consultar las órdenes previamente registradas
 * por el usuario autenticado, incluyendo sus artículos,
 * importes históricos y estados actuales.
 *
 * Las reglas de negocio y la recuperación de información
 * corresponden a la capa Service.
 */
@Controller
@RequestMapping("/mis-ordenes")
public class MisOrdenesController {

    private final OrdenCompraService ordenCompraService;

    /**
     * Constructor para la inyección del servicio
     * encargado de gestionar las órdenes de compra.
     *
     * @param ordenCompraService servicio de órdenes.
     */
    public MisOrdenesController(
            OrdenCompraService ordenCompraService) {

        this.ordenCompraService = ordenCompraService;
    }

    /**
     * Presenta el historial de compras correspondiente
     * exclusivamente al cliente autenticado.
     *
     * GET /mis-ordenes
     *
     * La identidad del cliente se obtiene del contexto
     * de seguridad y no de parámetros proporcionados
     * por el navegador.
     *
     * @param authentication identidad autenticada.
     * @param model modelo de datos para Thymeleaf.
     * @return plantilla del historial de órdenes.
     */
    @GetMapping
    public String mostrarHistorial(
            Authentication authentication,
            Model model) {

        // Recuperar la identidad del cliente autenticado.
        String correo = authentication.getName();

        // Consultar exclusivamente sus órdenes de compra.
        model.addAttribute(
                "ordenes",
                ordenCompraService.consultarHistorial(correo)
        );

        // Presentar el historial mediante Thymeleaf.
        return "mis-ordenes";
    }

    /**
     * Presenta el detalle individual de una orden
     * previamente registrada por el cliente autenticado.
     *
     * GET /mis-ordenes/{ordenId}
     *
     * La capa Service verifica que la orden solicitada
     * pertenezca al usuario autenticado.
     *
     * Esta operación es exclusivamente de consulta.
     * No procesa compras ni modifica información.
     *
     * @param ordenId identificador de la orden.
     * @param authentication identidad autenticada.
     * @param model modelo de datos para Thymeleaf.
     * @return plantilla del detalle de la orden.
     */
    @GetMapping("/{ordenId}")
    public String mostrarDetalle(
            @PathVariable("ordenId") Long ordenId,
            Authentication authentication,
            Model model) {

        String correo = authentication.getName();

        // Recuperar únicamente una orden propia.
        model.addAttribute(
                "orden",
                ordenCompraService.consultarOrdenPropia(
                        correo,
                        ordenId
                )
        );

        return "detalle-orden-cliente";
    }

}
