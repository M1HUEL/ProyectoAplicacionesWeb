
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.DireccionRequestDTO;
import com.itson.ProyectoAplicacionesWeb.dto.DireccionResponseDTO;
import com.itson.ProyectoAplicacionesWeb.model.Direccion;
import com.itson.ProyectoAplicacionesWeb.service.DireccionService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST de las direcciones de envío del cliente autenticado.
 *
 * Reutiliza DireccionService, por lo que todas las operaciones están
 * restringidas a las direcciones propias. La identidad se obtiene
 * exclusivamente del JWT mediante {@code Authentication#getName()}.
 *
 * Las solicitudes requieren autenticación mediante JWT.
 */
@RestController
@RequestMapping("/api/direcciones")
public class DireccionRestController {

    private static final String RUTA_BASE = "/api/direcciones";

    private final DireccionService direccionService;

    public DireccionRestController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    /**
     * Lista las direcciones del cliente autenticado.
     *
     * GET /api/direcciones
     */
    @GetMapping
    public ResponseEntity<List<DireccionResponseDTO>> listar(
            Authentication authentication) {

        String correo = authentication.getName();

        List<DireccionResponseDTO> respuesta =
                direccionService.listarPorUsuario(correo)
                        .stream()
                        .map(DireccionResponseDTO::desde)
                        .toList();

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Consulta una dirección propia.
     *
     * GET /api/direcciones/{direccionId}
     */
    @GetMapping("/{direccionId}")
    public ResponseEntity<DireccionResponseDTO> consultar(
            Authentication authentication,
            @PathVariable Long direccionId) {

        String correo = authentication.getName();

        Direccion direccion =
                direccionService.buscarPropia(correo, direccionId);

        return ResponseEntity.ok(DireccionResponseDTO.desde(direccion));
    }

    /**
     * Registra una dirección para el cliente autenticado.
     *
     * POST /api/direcciones
     */
    @PostMapping
    public ResponseEntity<DireccionResponseDTO> registrar(
            Authentication authentication,
            @RequestBody(required = false) DireccionRequestDTO solicitud) {

        String correo = authentication.getName();

        Direccion direccion =
                direccionService.registrar(correo, solicitud);

        return ResponseEntity
                .created(URI.create(RUTA_BASE + "/" + direccion.getId()))
                .body(DireccionResponseDTO.desde(direccion));
    }

    /**
     * Actualiza una dirección propia.
     *
     * PUT /api/direcciones/{direccionId}
     */
    @PutMapping("/{direccionId}")
    public ResponseEntity<DireccionResponseDTO> actualizar(
            Authentication authentication,
            @PathVariable Long direccionId,
            @RequestBody(required = false) DireccionRequestDTO solicitud) {

        String correo = authentication.getName();

        Direccion direccion = direccionService.actualizar(
                correo,
                direccionId,
                solicitud
        );

        return ResponseEntity.ok(DireccionResponseDTO.desde(direccion));
    }

    /**
     * Elimina una dirección propia.
     *
     * DELETE /api/direcciones/{direccionId}
     */
    @DeleteMapping("/{direccionId}")
    public ResponseEntity<Void> eliminar(
            Authentication authentication,
            @PathVariable Long direccionId) {

        String correo = authentication.getName();

        direccionService.eliminar(correo, direccionId);

        return ResponseEntity.noContent().build();
    }
}
