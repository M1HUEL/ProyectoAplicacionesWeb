
package com.itson.ProyectoAplicacionesWeb.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa una orden del historial de compras
 * de un cliente en las respuestas de la API REST.
 */
public record OrdenCompraResponseDTO(
        Long ordenId,
        LocalDateTime fecha,
        String estado,
        BigDecimal total,
        List<DetalleOrdenResponseDTO> detalles
) {
}
