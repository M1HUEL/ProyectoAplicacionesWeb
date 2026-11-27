
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.service.DescargaDigitalService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Expone la descarga de ejemplares digitales
 * de demostración para clientes autenticados.
 */
@Controller
@RequestMapping("/mis-ordenes")
public class DescargaDigitalController {

    private final DescargaDigitalService descargaDigitalService;

    public DescargaDigitalController(
            DescargaDigitalService descargaDigitalService) {
        this.descargaDigitalService = descargaDigitalService;
    }

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
