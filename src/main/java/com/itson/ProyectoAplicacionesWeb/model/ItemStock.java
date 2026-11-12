
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa las existencias físicas de una presentación
 * comercial de un libro en Librería Raudskinna.
 *
 * Esta entidad únicamente define la estructura de los datos.
 * Las reglas de inventario y sus validaciones se implementan
 * en la capa Service.
 */
@Entity
@Table(name = "items_stock")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemStock {

    /** Identificador único generado por MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /** Presentación comercial asociada al inventario. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "formato_libro_id",
            nullable = false,
            unique = true
    )
    private FormatoLibro formatoLibro;

    /** Unidades físicas disponibles. */
    @Column(nullable = false)
    private int cantidad;

    /** Umbral mínimo para alertas de inventario. */
    @Column(nullable = false)
    private int stockMinimo;

    /**
     * Constructor para registrar las existencias iniciales
     * de una presentación física de un libro.
     *
     * Las validaciones de cantidades y formatos permitidos
     * corresponden a la capa Service.
     *
     * @param formatoLibro presentación comercial asociada.
     * @param cantidad unidades físicas disponibles.
     * @param stockMinimo umbral mínimo de inventario.
     *
     */
    public ItemStock(
            FormatoLibro formatoLibro,
            int cantidad,
            int stockMinimo) {

        this.formatoLibro = formatoLibro;
        this.cantidad = cantidad;
        this.stockMinimo = stockMinimo;
    }

}
