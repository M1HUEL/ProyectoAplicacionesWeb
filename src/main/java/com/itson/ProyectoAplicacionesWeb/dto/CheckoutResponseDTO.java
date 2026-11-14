
package com.itson.ProyectoAplicacionesWeb.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta de la API REST después
 * de confirmar una orden de compra.
 */
public record CheckoutResponseDTO(
        Long ordenId,
        LocalDateTime fecha,
        String estado,
        BigDecimal total
) {
}
