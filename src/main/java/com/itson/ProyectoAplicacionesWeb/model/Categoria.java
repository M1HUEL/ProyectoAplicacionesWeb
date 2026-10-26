
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa una categoría utilizada para clasificar
 * los libros del catálogo de Librería Raudskinna.
 *
 * Esta entidad define exclusivamente la estructura
 * de los datos. Las validaciones corresponden a Service.
 */
@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categoria {

    /** Identificador único generado por MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /** Nombre de la categoría. */
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    /**
     * Constructor para crear una categoría.
     *
     * Las validaciones se realizarán en CategoriaService
     * cuando implementemos la administración del catálogo.
     */
    public Categoria(String nombre) {
        this.nombre = nombre;
    }
}
