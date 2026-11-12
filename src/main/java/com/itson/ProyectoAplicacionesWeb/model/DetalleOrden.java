
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Representa una presentación comercial incluida
 * en una orden de compra de Librería Raudskinna.
 */
@Entity
@Table(name = "detalles_orden")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetalleOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    // Orden de compra a la que pertenece este detalle.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_compra_id", nullable = false)
    private OrdenCompra ordenCompra;

    // Presentación comercial adquirida.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "formato_libro_id", nullable = false)
    private FormatoLibro formatoLibro;

    // Número de unidades adquiridas.
    @Column(nullable = false)
    private Integer cantidad;

    // Precio unitario registrado al momento de la compra.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;
}
