
package com.itson.ProyectoAplicacionesWeb.dto;

/**
 * Datos proporcionados por el cliente para registrar
 * o actualizar una dirección de envío.
 *
 * No incluye usuarioId: el propietario se obtiene
 * mediante la identidad del cliente autenticado.
 */
public record DireccionRequestDTO(
        String nombreDestinatario,
        String calle,
        String numeroExterior,
        String numeroInterior,
        String colonia,
        String codigoPostal,
        String ciudad,
        String estado,
        String pais,
        String referencias
) {
}
