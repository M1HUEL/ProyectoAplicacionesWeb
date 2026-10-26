
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Representa una presentación comercial de un libro.
 *
 * Cada formato tiene su propio precio y pertenece
 * a un libro del catálogo de Raudskinna.
 *
 * Las existencias físicas se administrarán posteriormente
 * mediante ItemStock.
 */
@Entity
@Table(
        name = "formatos_libro",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_libro_formato",
                columnNames = {"libro_id", "formato"}
        )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FormatoLibro {

    /**
     * Identificador único de la presentación comercial.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /**
     * Libro al que pertenece esta presentación.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    /**
     * Tipo de presentación: pasta dura, blanda o digital.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Formato formato;

    /**
     * Precio comercial de la presentación.
     *
     * BigDecimal evita los problemas de precisión
     * de los tipos float y double con importes monetarios.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /**
     * Constructor para registrar una presentación comercial.
     */
    public FormatoLibro(Libro libro, Formato formato, BigDecimal precio) {
        this.libro = libro;
        this.formato = formato;
        this.precio = precio;
    }
}
