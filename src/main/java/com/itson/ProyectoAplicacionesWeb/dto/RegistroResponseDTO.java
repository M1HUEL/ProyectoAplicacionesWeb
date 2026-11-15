
package com.itson.ProyectoAplicacionesWeb.dto;

/**
 * Respuesta pública después de registrar un cliente.
 *
 * No expone contraseñas ni hashes BCrypt.
 */
public record RegistroResponseDTO(
        Long id,
        String nombre,
        String correo,
        String rol
) {
}
