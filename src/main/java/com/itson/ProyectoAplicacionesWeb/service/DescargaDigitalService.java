
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.OrdenCompraResponseDTO;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Gestiona la descarga simulada de libros digitales.
 *
 * Reutiliza la validación de propiedad de las órdenes.
 * No modifica inventarios ni entidades JPA.
 */
@Service
public class DescargaDigitalService {

    private static final String ARCHIVO_DEMO =
            "descargas/raudskinna-muestra-digital.pdf";

    private final OrdenCompraService ordenCompraService;

    public DescargaDigitalService(
            OrdenCompraService ordenCompraService) {

        this.ordenCompraService = ordenCompraService;
    }

    /**
     * Autoriza la descarga de un libro digital adquirido.
     *
     * Requisitos:
     * - La orden pertenece al cliente autenticado.
     * - El formato solicitado está incluido en la orden.
     * - El artículo adquirido es digital.
     * - La orden se encuentra ENTREGADA.
     */
    public byte[] descargar(
            String correo,
            Long ordenId,
            Long formatoId) {

        if (formatoId == null || formatoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El formato solicitado no es válido."
            );
        }

        // También valida la identidad y propiedad de la orden.
        OrdenCompraResponseDTO orden =
                ordenCompraService.consultarOrdenPropia(
                        correo, ordenId
                );

        boolean formatoAdquirido = orden.detalles().stream()
                .anyMatch(detalle ->
                        formatoId.equals(detalle.formatoLibroId())
                                && "DIGITAL".equals(detalle.formato())
                );

        if (!formatoAdquirido) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe una compra digital asociada."
            );
        }

        if (!"ENTREGADO".equals(orden.estado())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La descarga aún no está autorizada."
            );
        }

        try {
            return new ClassPathResource(ARCHIVO_DEMO)
                    .getInputStream()
                    .readAllBytes();

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo recuperar el archivo digital.",
                    e
            );
        }
    }
}
