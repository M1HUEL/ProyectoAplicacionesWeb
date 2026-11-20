
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro del pago asociado a una orden de compra.
 *
 * No representa un cobro bancario real.
 * Las reglas de negocio pertenecen a la capa Service.
 */
@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pago {

    public Pago(
            OrdenCompra ordenCompra,
            BigDecimal monto,
            MetodoPago metodo,
            EstadoPago estado,
            LocalDateTime fechaRegistro) {

        this.ordenCompra = ordenCompra;
        this.monto = monto;
        this.metodo = metodo;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "orden_compra_id",
            nullable = false,
            unique = true
    )
    private OrdenCompra ordenCompra;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;
}
