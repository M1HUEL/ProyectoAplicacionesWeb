
package com.itson.ProyectoAplicacionesWeb.dto;

import java.math.BigDecimal;

/**
 * Representa una presentación incluida en una orden
 * dentro del historial de compras de un cliente.
 */
public record DetalleOrdenResponseDTO(
        Long formatoLibroId,
        String titulo,
        String formato,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
