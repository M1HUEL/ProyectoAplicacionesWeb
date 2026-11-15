
package com.itson.ProyectoAplicacionesWeb.dto;

import java.math.BigDecimal;

/**
 * Representa un artículo del carrito
 * en las respuestas de la API REST.
 */
public record ItemCarritoResponseDTO(
        Long formatoLibroId,
        String titulo,
        String formato,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
