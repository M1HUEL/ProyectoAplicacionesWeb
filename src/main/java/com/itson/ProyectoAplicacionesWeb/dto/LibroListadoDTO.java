
package com.itson.ProyectoAplicacionesWeb.dto;

import java.math.BigDecimal;

/**
 * DTO utilizado para mostrar la información de los libros
 * en el listado administrativo de Librería Raudskinna.
 *
 * Cada objeto representa una presentación comercial
 * de un libro, incluyendo precio e inventario.
 *
 * No es una entidad JPA y no contiene reglas de negocio.
 *
 */
public record LibroListadoDTO(

        /** Identificador del libro. */
        Long libroId,

        /** Título del libro. */
        String titulo,

        /** Nombres de los autores del libro. */
        String autores,

        /** Presentación comercial del libro. */
        String formato,

        /** Precio correspondiente a la presentación. */
        BigDecimal precio,

        /** Existencias físicas; null para formato digital. */
        Integer stock,

        /** Estado calculado por el servicio. */
        String estado

) {
}
