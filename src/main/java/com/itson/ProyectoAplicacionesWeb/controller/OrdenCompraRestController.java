
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.OrdenCompraResponseDTO;
import com.itson.ProyectoAplicacionesWeb.service.OrdenCompraService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para consultar el historial de compras
 * de los clientes autenticados.
 *
 * Las solicitudes requieren autenticación mediante JWT.
 */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenCompraRestController {

    private final OrdenCompraService ordenCompraService;

    public OrdenCompraRestController(OrdenCompraService ordenCompraService) {
        this.ordenCompraService = ordenCompraService;
    }

    /**
     * Consulta las órdenes del cliente autenticado,
     * desde la más reciente.
     *
     * El correo se obtiene del JWT validado, por lo que
     * un cliente solo puede consultar su propio historial.
     *
     * GET /api/ordenes
     */
    @GetMapping
    public ResponseEntity<List<OrdenCompraResponseDTO>> consultarHistorial(
            Authentication authentication) {

        String correo = authentication.getName();

        List<OrdenCompraResponseDTO> respuesta =
                ordenCompraService.consultarHistorial(correo);

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Consulta el detalle de una orden perteneciente
     * al cliente autenticado.
     *
     * El correo se obtiene del JWT validado, por lo que un
     * cliente no puede consultar órdenes de otras personas
     * aunque conozca su identificador.
     *
     * GET /api/ordenes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraResponseDTO> consultarOrden(
            Authentication authentication,
            @PathVariable Long id) {

        String correo = authentication.getName();

        OrdenCompraResponseDTO respuesta =
                ordenCompraService.consultarOrdenPropia(correo, id);

        return ResponseEntity.ok(respuesta);
    }
}
