
package com.itson.ProyectoAplicacionesWeb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos necesarios para registrar un cliente desde la API REST.
 *
 * El identificador y el rol son administrados por el servidor.
 */
public record RegistroRequestDTO(

        @NotBlank(message = "El nombre es obligatorio.")
        String nombre,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
        String contrasena

) {
}
