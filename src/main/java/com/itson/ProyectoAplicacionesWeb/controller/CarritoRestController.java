
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.AgregarItemCarritoRequestDTO;
import com.itson.ProyectoAplicacionesWeb.dto.CarritoResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.ModificarCantidadCarritoRequestDTO;
import com.itson.ProyectoAplicacionesWeb.service.CarritoService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST del carrito de compras.
 *
 * Las solicitudes requieren autenticación mediante JWT.
 */
@RestController
@RequestMapping("/api/carrito")
public class CarritoRestController {

    private final CarritoService carritoService;

    public CarritoRestController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    /**
     * Consulta el carrito del cliente autenticado.
     *
     * GET /api/carrito
     */
    @GetMapping
    public ResponseEntity<CarritoResponseDTO> consultar(
            Authentication authentication) {

        String correo = authentication.getName();

        CarritoResponseDTO respuesta =
                carritoService.consultarCarrito(correo);

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Agrega una presentación comercial al carrito
     * del cliente autenticado.
     *
     * POST /api/carrito/items
     */
    @PostMapping("/items")
    public ResponseEntity<CarritoResponseDTO> agregarItem(
            Authentication authentication,
            @Valid @RequestBody AgregarItemCarritoRequestDTO solicitud) {

        String correo = authentication.getName();

        CarritoResponseDTO respuesta = carritoService.agregarItem(
                correo,
                solicitud
        );

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Modifica la cantidad de una presentación comercial
     * existente en el carrito del cliente autenticado.
     *
     * PUT /api/carrito/items/{formatoLibroId}
     */
    @PutMapping("/items/{formatoLibroId}")
    public ResponseEntity<CarritoResponseDTO> modificarCantidad(
            Authentication authentication,
            @PathVariable Long formatoLibroId,
            @Valid @RequestBody ModificarCantidadCarritoRequestDTO solicitud) {

        String correo = authentication.getName();

        CarritoResponseDTO respuesta = carritoService.modificarCantidad(
                correo,
                formatoLibroId,
                solicitud
        );

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Elimina una presentación comercial del carrito
     * perteneciente al cliente autenticado.
     *
     * DELETE /api/carrito/items/{formatoLibroId}
     */
    @DeleteMapping("/items/{formatoLibroId}")
    public ResponseEntity<CarritoResponseDTO> eliminarItem(
            Authentication authentication,
            @PathVariable Long formatoLibroId) {

        String correo = authentication.getName();

        CarritoResponseDTO respuesta = carritoService.eliminarItem(
                correo,
                formatoLibroId
        );

        return ResponseEntity.ok(respuesta);
    }

}
