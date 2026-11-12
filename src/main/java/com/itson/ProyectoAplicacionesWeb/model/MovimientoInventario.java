package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

    @Entity
    @Table(name = "movimientos_inventario")
    @Getter
    @Setter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public class MovimientoInventario {


        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Setter(AccessLevel.NONE)
        private Long id;


        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "item_stock_id", nullable = false)
        private ItemStock itemStock;

        /** Fecha y hora en que se registró el ajuste. */
        @Column(nullable = false)
        private LocalDateTime fecha;

        /** Motivo del ajuste. */
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private MotivoAjuste motivo;

        /** Unidades sumadas (positivo) o restadas (negativo). */
        @Column(nullable = false)
        private int diferencia;

        /** Existencias antes del ajuste. */
        @Column(nullable = false)
        private int cantidadAnterior;

        @Column(nullable = false)
        private int cantidadNueva;

        @Column(length = 255)
        private String nota;

        @Column(nullable = false, length = 150)
        private String responsable;

        public MovimientoInventario(ItemStock itemStock,
                                    MotivoAjuste motivo,
                                    int diferencia,
                                    int cantidadAnterior,
                                    int cantidadNueva,
                                    String nota,
                                    String responsable) {
            this.itemStock = itemStock;
            this.fecha = LocalDateTime.now();
            this.motivo = motivo;
            this.diferencia = diferencia;
            this.cantidadAnterior = cantidadAnterior;
            this.cantidadNueva = cantidadNueva;
            this.nota = nota;
            this.responsable = responsable;
        }


}
