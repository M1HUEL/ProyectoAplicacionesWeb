
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa una copia histórica de la dirección
 * utilizada para entregar una orden de compra.
 *
 * Sus atributos se almacenan en la tabla
 * ordenes_compra mediante @Embedded.
 *
 * No representa una dirección administrable
 * desde la libreta personal del cliente.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DireccionEnvio {

    @Column(name = "envio_nombre_destinatario", length = 150)
    private String nombreDestinatario;

    @Column(name = "envio_calle", length = 150)
    private String calle;

    @Column(name = "envio_numero_exterior", length = 20)
    private String numeroExterior;

    @Column(name = "envio_numero_interior", length = 20)
    private String numeroInterior;

    @Column(name = "envio_colonia", length = 100)
    private String colonia;

    @Column(name = "envio_codigo_postal", length = 5)
    private String codigoPostal;

    @Column(name = "envio_ciudad", length = 100)
    private String ciudad;

    @Column(name = "envio_estado", length = 100)
    private String estado;

    @Column(name = "envio_pais", length = 100)
    private String pais;

    @Column(name = "envio_referencias", length = 500)
    private String referencias;

    /**
     * Construye la copia de los datos de entrega.
     * Las validaciones corresponden a la capa Service.
     */
    public DireccionEnvio(
            String nombreDestinatario,
            String calle,
            String numeroExterior,
            String numeroInterior,
            String colonia,
            String codigoPostal,
            String ciudad,
            String estado,
            String pais,
            String referencias) {

        this.nombreDestinatario = nombreDestinatario;
        this.calle = calle;
        this.numeroExterior = numeroExterior;
        this.numeroInterior = numeroInterior;
        this.colonia = colonia;
        this.codigoPostal = codigoPostal;
        this.ciudad = ciudad;
        this.estado = estado;
        this.pais = pais;
        this.referencias = referencias;
    }
}
