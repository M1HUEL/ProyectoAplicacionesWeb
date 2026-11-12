
package com.itson.ProyectoAplicacionesWeb.dto;

import java.time.LocalDateTime;

/**
 * Estructura de respuesta para los errores
 * producidos por la API REST.
 */
public record ErrorRespuestaDTO(
        LocalDateTime fechaHora,
        int estado,
        String error,
        String mensaje,
        String ruta
) {
}
