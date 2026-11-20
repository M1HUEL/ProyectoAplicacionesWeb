
package com.itson.ProyectoAplicacionesWeb.dto;

import com.itson.ProyectoAplicacionesWeb.model.Formato;

import java.math.BigDecimal;

/**
 * Representa la información comercial pública de una
 * presentación de libro en Librería Raudskinna.
 *
 * Permite mostrar el formato, precio y disponibilidad
 * sin exponer directamente las entidades JPA.
 *
 * La disponibilidad es informativa y puede cambiar
 * antes de que el cliente confirme su compra.
 *
 * Las validaciones definitivas de inventario
 * corresponden a la capa Service.
 *
 * @param id identificador de la presentación comercial.
 * @param formato tipo de presentación del libro.
 * @param precio precio comercial registrado.
 * @param disponible indica si la presentación puede
 *                   ofrecerse actualmente al cliente.
 */
public record FormatoPublicoDTO(
        Long id,
        Formato formato,
        BigDecimal precio,
        boolean disponible
) {
}
