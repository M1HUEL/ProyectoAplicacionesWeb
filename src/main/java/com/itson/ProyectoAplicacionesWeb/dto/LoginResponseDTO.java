
package com.itson.ProyectoAplicacionesWeb.dto;

/**
 * Respuesta de autenticación de la API REST.
 *
 * Contiene el token JWT y los datos necesarios
 * para que el cliente pueda utilizarlo.
 */
public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
