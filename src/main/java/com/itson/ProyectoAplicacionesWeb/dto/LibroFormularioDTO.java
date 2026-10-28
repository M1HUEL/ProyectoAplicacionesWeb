
package com.itson.ProyectoAplicacionesWeb.dto;

import com.itson.ProyectoAplicacionesWeb.model.Formato;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO utilizado para recibir los datos del formulario
 * administrativo de registro y edición de libros.
 *
 * Contiene información bibliográfica, una presentación
 * comercial y sus existencias iniciales.
 *
 * Las reglas de negocio y validaciones se aplicarán
 * en la capa Service.
 *
 */
@Getter
@Setter
public class LibroFormularioDTO {

    /** Título del libro. */
    private String titulo;

    /** ISBN que identifica la edición. */
    private String isbn;

    /** Identificadores de los autores seleccionados. */
    private Set<Long> autoresIds = new HashSet<>();

    /** Identificador de la editorial seleccionada. */
    private Long editorialId;

    /** Identificador de la categoría seleccionada. */
    private Long categoriaId;

    /** Sinopsis o descripción del libro. */
    private String sinopsis;

    /** Dirección de la imagen de portada. */
    private String portadaUrl;

    /** Presentación comercial inicial del libro. */
    private Formato formato;

    /** Precio de la presentación comercial. */
    private BigDecimal precio;

    /** Existencias iniciales para formatos físicos. */
    private Integer cantidad;

    /** Umbral mínimo de inventario físico. */
    private Integer stockMinimo;
}
