
package com.itson.ProyectoAplicacionesWeb.dto;

import java.math.BigDecimal;


/**
 * Datos de una presentación comercial para
 * el listado administrativo de libros.
 *
 * libroId identifica la obra bibliográfica.
 * formatoId identifica su presentación comercial.
 */
public record LibroListadoDTO(
        Long libroId,
        Long formatoId,
        String titulo,
        String autores,
        String formato,
        BigDecimal precio,
        Integer stock,
        String estado
) {
}
