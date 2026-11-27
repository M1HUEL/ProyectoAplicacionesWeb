package com.itson.ProyectoAplicacionesWeb.dto;

import java.util.List;

/**
 * Detalle público de un libro, incluidas sus presentaciones
 * comerciales con formato, precio y disponibilidad.
 *
 * Permite que el cliente obtenga en una sola consulta el
 * formatoLibroId necesario para agregar artículos al carrito.
 */
public record LibroDetallePublicoDTO(
        Long id,
        String titulo,
        String sinopsis,
        String portadaUrl,
        String categoria,
        String editorial,
        List<String> autores,
        List<FormatoPublicoDTO> formatos
) {
}
