
package com.itson.ProyectoAplicacionesWeb.dto;

import com.itson.ProyectoAplicacionesWeb.model.MetodoPago;

/**
 * Datos recibidos al confirmar una compra.
 *
 * El correo del cliente se obtiene de Spring Security,
 * no del cuerpo de la solicitud.
 *
 * direccionId puede ser null para compras
 * exclusivamente digitales.
 *
 * metodoPago identifica el método seleccionado
 * por el cliente. No implica procesar un cobro real.
 */
public record CheckoutRequestDTO(
        Long direccionId,
        MetodoPago metodoPago
) {
}
