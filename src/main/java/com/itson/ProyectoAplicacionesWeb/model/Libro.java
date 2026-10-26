
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa la información bibliográfica de un libro
 * dentro del catálogo de Librería Raudskinna.
 *
 * Los precios y las existencias se administran mediante
 * FormatoLibro e ItemStock.
 *
 * Las reglas de negocio y validaciones corresponden
 * a la capa Service.
 */
@Entity
@Table(name = "libros")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Libro {

    /** Identificador único generado por MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /** Título del libro. */
    @Column(nullable = false, length = 200)
    private String titulo;

    /** ISBN utilizado para identificar la edición. */
    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    /** Descripción o sinopsis del libro. */
    @Column(columnDefinition = "TEXT")
    private String sinopsis;

    /** Ruta o URL de la imagen de portada. */
    @Column(length = 500)
    private String portadaUrl;

    /** Número de páginas del libro. */
    private Integer numeroPaginas;

    /** Fecha de publicación. */
    private LocalDate fechaPublicacion;

    /**
     * Categoría a la que pertenece el libro.
     * Una categoría puede contener varios libros.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /**
     * Editorial responsable de la publicación.
     * Una editorial puede publicar varios libros.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editorial_id", nullable = false)
    private Editorial editorial;

    /**
     * Autores que participaron en el libro.
     *
     * La tabla intermedia libros_autores almacena
     * las asociaciones entre libros y autores.
     */
    @ManyToMany
    @JoinTable(
            name = "libros_autores",
            joinColumns = @JoinColumn(name = "libro_id"),
            inverseJoinColumns = @JoinColumn(name = "autor_id")
    )
    private Set<Autor> autores = new HashSet<>();

    /**
     * Constructor para crear un libro con sus datos
     * bibliográficos principales.
     *
     * Las validaciones se realizan en LibroService.
     */
    public Libro(String titulo, String isbn,
                 Categoria categoria, Editorial editorial) {
        this.titulo = titulo;
        this.isbn = isbn;
        this.categoria = categoria;
        this.editorial = editorial;
    }
}
