
package com.itson.ProyectoAplicacionesWeb.dto;

/**
 * Datos recibidos al confirmar una compra.
 *
 * El correo del cliente se obtiene de Spring Security,
 * no del cuerpo de la solicitud.
 *
 * direccionId puede ser null para compras
 * exclusivamente digitales.
 */
public record CheckoutRequestDTO(
        Long direccionId
) {
}
