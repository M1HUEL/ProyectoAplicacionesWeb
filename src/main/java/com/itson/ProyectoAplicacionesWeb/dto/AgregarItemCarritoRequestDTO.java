
package com.itson.ProyectoAplicacionesWeb.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Datos necesarios para agregar una presentación
 * comercial al carrito de compras.
 */
public record AgregarItemCarritoRequestDTO(

        @NotNull(message = "La presentación comercial es obligatoria.")
        @Positive(message = "El identificador de la presentación debe ser positivo.")
        Long formatoLibroId,

        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser al menos 1.")
        Integer cantidad
) {
}
