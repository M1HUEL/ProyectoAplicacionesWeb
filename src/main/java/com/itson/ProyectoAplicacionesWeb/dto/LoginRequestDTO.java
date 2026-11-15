
package com.itson.ProyectoAplicacionesWeb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales recibidas mediante la API REST
 * para autenticar a un usuario.
 */
public record LoginRequestDTO(

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria.")
        String contrasena

) {
}
