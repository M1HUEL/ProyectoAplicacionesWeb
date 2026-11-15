
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa el carrito de compras de un cliente.
 *
 * Cada usuario puede tener un carrito activo.
 * Los productos se administrarán mediante ItemCarrito.
 */
@Entity
@Table(name = "carritos_compra")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarritoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_id",
            nullable = false,
            unique = true
    )
    private Usuario usuario;

    @OneToMany(
            mappedBy = "carritoCompra",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ItemCarrito> items = new ArrayList<>();

    public CarritoCompra(Usuario usuario) {
        this.usuario = usuario;
    }
}
