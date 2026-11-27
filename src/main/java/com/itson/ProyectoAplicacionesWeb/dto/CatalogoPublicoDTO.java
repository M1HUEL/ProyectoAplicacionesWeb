
package com.itson.ProyectoAplicacionesWeb.dto;

import java.util.List;

/**
 * DTO que agrupa los resultados del catálogo público
 * y las opciones disponibles para su filtrado.
 *
 * Las categorías y autores se obtienen del catálogo
 * público completo, independientemente de los
 * criterios seleccionados por el visitante.
 *
 * Permite transportar la información necesaria
 * hacia la capa de presentación sin realizar
 * recuperaciones adicionales del catálogo.
 *
 * @param libros libros filtrados y ordenados.
 * @param categorias categorías disponibles para filtrado.
 * @param autores autores disponibles para filtrado.
 */
public record CatalogoPublicoDTO(

        List<LibroCatalogoDTO> libros,

        List<String> categorias,

        List<String> autores

) {
}
