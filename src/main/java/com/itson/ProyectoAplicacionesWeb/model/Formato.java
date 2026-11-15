
package com.itson.ProyectoAplicacionesWeb.model;

/**
 * Representa las presentaciones disponibles de un libro
 * dentro de Librería Raudskinna.
 *
 * Se conserva la clasificación del dominio original.
 *
 * El formato se utilizará posteriormente en FormatoLibro
 * para identificar la presentación comercial de cada libro.
 */
public enum Formato {

    /**
     * Edición física de pasta dura.
     */
    PASTA_DURA,

    /**
     * Edición física de pasta blanda.
     */
    PASTA_BLANDA,

    /**
     * Edición digital del libro.
     */
    DIGITAL
}
