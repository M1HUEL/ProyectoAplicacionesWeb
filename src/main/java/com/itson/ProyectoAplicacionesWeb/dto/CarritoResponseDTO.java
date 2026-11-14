
package com.itson.ProyectoAplicacionesWeb.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa el carrito completo de un cliente
 * en las respuestas de la API REST.
 */
public record CarritoResponseDTO(
        Long carritoId,
        List<ItemCarritoResponseDTO> items,
        BigDecimal total
) {
}
