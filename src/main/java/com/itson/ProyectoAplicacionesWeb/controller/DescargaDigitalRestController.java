package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.DescargaDigitalService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST para descargar ejemplares digitales de demostración.
 *
 * La identidad del cliente se obtiene del JWT validado, por lo que
 * no es posible descargar compras de otros usuarios.
 */
@RestController
@RequestMapping("/api/ordenes")
public class DescargaDigitalRestController {

    private final DescargaDigitalService descargaDigitalService;

    public DescargaDigitalRestController(
            DescargaDigitalService descargaDigitalService) {
        this.descargaDigitalService = descargaDigitalService;
    }

    /**
     * GET /api/ordenes/{ordenId}/descargar/{formatoId}
     *
     * Devuelve el PDF de demostración cuando la orden pertenece al
     * cliente autenticado, incluye el formato digital y ya fue entregada.
     */
    @GetMapping("/{ordenId}/descargar/{formatoId}")
    public ResponseEntity<byte[]> descargar(
            @PathVariable Long ordenId,
            @PathVariable Long formatoId,
            Authentication authentication) {

        byte[] archivo = descargaDigitalService.descargar(
                authentication.getName(),
                ordenId,
                formatoId
        );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"raudskinna-muestra-digital.pdf\""
                )
                .cacheControl(CacheControl.noStore())
                .body(archivo);
    }
}
