
package com.itson.ProyectoAplicacionesWeb.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Datos necesarios para modificar la cantidad
 * de un artículo existente en el carrito.
 */
public record ModificarCantidadCarritoRequestDTO(

        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser al menos 1.")
        Integer cantidad

) {
}
