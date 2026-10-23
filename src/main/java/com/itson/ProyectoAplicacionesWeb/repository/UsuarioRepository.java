package com.itson.ProyectoAplicacionesWeb.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itson.ProyectoAplicacionesWeb.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
