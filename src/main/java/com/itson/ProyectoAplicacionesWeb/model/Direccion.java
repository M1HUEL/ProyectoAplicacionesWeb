
package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa una dirección de envío registrada
 * por un cliente de Librería Raudskinna.
 *
 * Un usuario puede registrar varias direcciones.
 * Cada dirección pertenece a un único usuario.
 */
@Entity
@Table(name = "direcciones")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    // Cliente propietario de la dirección.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Persona que recibirá el pedido.
    @Column(name = "nombre_destinatario", nullable = false, length = 150)
    private String nombreDestinatario;

    @Column(nullable = false, length = 150)
    private String calle;

    @Column(name = "numero_exterior", nullable = false, length = 20)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 20)
    private String numeroInterior;

    @Column(nullable = false, length = 100)
    private String colonia;

    @Column(name = "codigo_postal", nullable = false, length = 5)
    private String codigoPostal;

    @Column(nullable = false, length = 100)
    private String ciudad;

    @Column(nullable = false, length = 100)
    private String estado;

    @Column(nullable = false, length = 100)
    private String pais;

    // Indicaciones adicionales para la entrega.
    @Column(length = 500)
    private String referencias;

    /**
     * Crea una dirección asociada a su propietario.
     *
     * Los demás datos serán establecidos por el servicio.
     */
    public Direccion(Usuario usuario) {
        this.usuario = usuario;
    }
}
