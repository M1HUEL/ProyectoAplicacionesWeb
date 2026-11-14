
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.CheckoutResponseDTO;
import com.itson.ProyectoAplicacionesWeb.service.CheckoutService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para confirmar las compras
 * realizadas por clientes autenticados.
 */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutRestController {

    private final CheckoutService checkoutService;

    public CheckoutRestController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * Confirma el carrito del cliente autenticado.
     *
     * POST /api/checkout
     */
    @PostMapping
    public ResponseEntity<CheckoutResponseDTO> confirmar(
            Authentication authentication) {

        String correo = authentication.getName();

        CheckoutResponseDTO respuesta =
                checkoutService.confirmarCompra(correo);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }
}
