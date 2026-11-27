
package com.itson.ProyectoAplicacionesWeb.dto;

import com.itson.ProyectoAplicacionesWeb.model.Direccion;

/**
 * Representa una dirección de envío en las respuestas de la API REST.
 *
 * No expone la entidad JPA ni la relación con el usuario: el propietario
 * siempre se obtiene del cliente autenticado.
 */
public record DireccionResponseDTO(
        Long id,
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

    /** Convierte una entidad en la representación pública de la API. */
    public static DireccionResponseDTO desde(Direccion direccion) {

        return new DireccionResponseDTO(
                direccion.getId(),
                direccion.getNombreDestinatario(),
                direccion.getCalle(),
                direccion.getNumeroExterior(),
                direccion.getNumeroInterior(),
                direccion.getColonia(),
                direccion.getCodigoPostal(),
                direccion.getCiudad(),
                direccion.getEstado(),
                direccion.getPais(),
                direccion.getReferencias()
        );
    }
}
