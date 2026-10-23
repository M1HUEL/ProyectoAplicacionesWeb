package com.itson.ProyectoAplicacionesWeb.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.itson.ProyectoAplicacionesWeb.model.Rol;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!usuarioRepository.existsByCorreo("admin@raudskinna.com")) {
                Usuario admin = new Usuario();
                admin.setNombre("Administrador");
                admin.setCorreo("admin@raudskinna.com");
                admin.setContrasena(passwordEncoder.encode("admin123"));
                admin.setRol(Rol.ADMIN);
                usuarioRepository.save(admin);
                System.out.println("Usuario ADMIN creado: admin@raudskinna.com / admin123");
            }
        };
    }
}
