
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa una editorial responsable de publicar libros
 * dentro del catálogo de Librería Raudskinna.
 *
 * Esta entidad contiene únicamente atributos y relaciones JPA.
 * Las reglas de negocio y validaciones corresponden a Service.
 */
@Entity
@Table(name = "editoriales")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Editorial {

    /** Identificador único generado por MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /** Nombre de la editorial. */
    @Column(nullable = false, length = 150)
    private String nombre;

    /** Dirección de la editorial, cuando esté disponible. */
    @Column(length = 255)
    private String direccion;

    /**
     * Constructor para crear una editorial.
     *
     * Las validaciones se realizarán en la capa Service.
     */
    public Editorial(String nombre, String direccion) {
        this.nombre = nombre;
        this.direccion = direccion;
    }
}
