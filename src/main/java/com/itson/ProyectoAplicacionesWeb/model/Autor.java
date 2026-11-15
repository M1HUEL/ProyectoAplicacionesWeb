
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa a un autor de los libros comercializados
 * por Librería Raudskinna.
 *
 * La relación muchos a muchos con Libro se administra
 * desde la entidad Libro mediante la tabla libros_autores.
 *
 * Las reglas de negocio corresponden a la capa Service.
 */
@Entity
@Table(name = "autores")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Autor {

    /** Identificador único generado por MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /** Nombre del autor. */
    @Column(nullable = false, length = 150)
    private String nombre;

    /**
     * Constructor para crear un autor.
     *
     * Las validaciones corresponden a AutorService.
     */
    public Autor(String nombre) {
        this.nombre = nombre;
    }
}
