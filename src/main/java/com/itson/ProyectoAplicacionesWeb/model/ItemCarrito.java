
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa una presentación comercial agregada
 * al carrito de compras de un cliente.
 */
@Entity
@Table(
        name = "items_carrito",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_carrito_formato",
                columnNames = {"carrito_id", "formato_libro_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    // Carrito al que pertenece el artículo.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrito_id", nullable = false)
    private CarritoCompra carritoCompra;

    // Presentación comercial seleccionada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "formato_libro_id", nullable = false)
    private FormatoLibro formatoLibro;

    // Unidades solicitadas por el cliente.
    @Column(nullable = false)
    private int cantidad;

    public ItemCarrito(
            CarritoCompra carritoCompra,
            FormatoLibro formatoLibro,
            int cantidad) {

        this.carritoCompra = carritoCompra;
        this.formatoLibro = formatoLibro;
        this.cantidad = cantidad;
    }
}
