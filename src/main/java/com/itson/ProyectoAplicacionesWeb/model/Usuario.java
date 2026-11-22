package com.itson.ProyectoAplicacionesWeb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(unique = true, nullable = false)
    private String correo;

    /**
     * Teléfono de contacto del usuario.
     *
     * Se permite null para conservar la compatibilidad
     * con registros anteriores a esta funcionalidad.
     */
    @Column(length = 10)
    private String telefono;

    @Column(nullable = false)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    private Rol rol = Rol.USER;

    /**
     * Constructor de compatibilidad con los avances
     * anteriores de Librería Raudskinna.
     *
     * Conserva la firma utilizada por los componentes
     * y pruebas automatizadas existentes.
     *
     * No implementa reglas ni validaciones de negocio.
     */
    public Usuario(
            Long id,
            String nombre,
            String correo,
            String contrasena,
            Rol rol) {

        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rol = rol;
    }

}
