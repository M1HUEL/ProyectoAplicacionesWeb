package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.OrdenCompraResponseDTO;
import com.itson.ProyectoAplicacionesWeb.storage.AlmacenamientoArchivos;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestiona la descarga de libros digitales.
 *
 * Reutiliza la validación de propiedad de las órdenes.
 * No modifica inventarios ni entidades JPA. El archivo se obtiene
 * del proveedor de almacenamiento configurado (local o Cloudinary).
 */
@Service
public class DescargaDigitalService {

    /** Clave lógica del PDF de demostración. */
    public static final String CLAVE_PDF_DEMO =
            "descargas/raudskinna-muestra-digital.pdf";

    private final OrdenCompraService ordenCompraService;
    private final AlmacenamientoArchivos almacenamiento;

    public DescargaDigitalService(
            OrdenCompraService ordenCompraService,
            AlmacenamientoArchivos almacenamiento) {

        this.ordenCompraService = ordenCompraService;
        this.almacenamiento = almacenamiento;
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

        return almacenamiento.leer(CLAVE_PDF_DEMO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se pudo recuperar el archivo digital."
                ));
    }
}
