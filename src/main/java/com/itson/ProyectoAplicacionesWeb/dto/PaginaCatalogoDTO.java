package com.itson.ProyectoAplicacionesWeb.dto;

import java.util.List;

/**
 * Respuesta paginada del catálogo público.
 *
 * Evita exponer directamente los objetos de Spring Data y
 * entrega al cliente la página solicitada junto con su
 * información de navegación.
 *
 * @param contenido libros de la página solicitada.
 * @param pagina índice de la página, iniciando en cero.
 * @param tamano cantidad máxima de libros por página.
 * @param totalElementos cantidad total de resultados.
 * @param totalPaginas cantidad total de páginas.
 * @param primera indica si es la primera página.
 * @param ultima indica si es la última página.
 */
public record PaginaCatalogoDTO(
        List<LibroCatalogoDTO> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima
) {
}
