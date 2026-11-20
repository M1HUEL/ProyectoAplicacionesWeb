
package com.itson.ProyectoAplicacionesWeb.dto;

import java.math.BigDecimal;

/**
 * Representa la información necesaria para mostrar
 * una tarjeta de libro en el catálogo público.
 *
 * Combina la información bibliográfica del libro
 * con un resumen de sus presentaciones comerciales.
 *
 * El precio inicial corresponde a la presentación
 * disponible de menor precio.
 *
 * Si ninguna presentación está disponible,
 * precioDesde será null y disponible será false.
 *
 * Las reglas para calcular estos valores pertenecen
 * a la capa Service, no a este DTO.
 *
 * @param libro información bibliográfica pública.
 * @param precioDesde menor precio entre las presentaciones disponibles.
 * @param disponible indica si existe al menos una presentación disponible.
 */
public record LibroCatalogoDTO(
        LibroPublicoDTO libro,
        BigDecimal precioDesde,
        boolean disponible
) {
}
