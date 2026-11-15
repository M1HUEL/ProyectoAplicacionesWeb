package com.itson.ProyectoAplicacionesWeb.dto;

import java.util.List;

/**
 * Información bibliográfica pública de un libro.
 *
 * Se utiliza como respuesta de la API REST para
 * evitar exponer directamente la entidad JPA Libro.
 */
public record LibroPublicoDTO(
        Long id,
        String titulo,
        String sinopsis,
        String portadaUrl,
        String categoria,
        String editorial,
        List<String> autores
) {
}