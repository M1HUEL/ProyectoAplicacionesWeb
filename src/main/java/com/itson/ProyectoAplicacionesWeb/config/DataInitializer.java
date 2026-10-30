package com.itson.ProyectoAplicacionesWeb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.itson.ProyectoAplicacionesWeb.model.Rol;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

/**
 * Configuración para la creación del usuario administrador inicial.
 *
 * Al iniciar la aplicación, verifica si existe una cuenta administrativa
 * predeterminada. Si no existe, la registra en la base de datos.
 *
 * La contraseña se obtiene desde una variable de entorno para evitar
 * almacenarla directamente en el código fuente.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${RAUDSKINNA_ADMIN_PASSWORD:Admin123!}") String adminPassword) {

        return args -> {

            // Evitamos crear nuevamente la cuenta en cada inicio.
            if (!usuarioRepository.existsByCorreo("admin@raudskinna.com")) {

                // No permitimos crear un administrador sin contraseña.
                if (adminPassword == null || adminPassword.isBlank()) {
                    throw new IllegalStateException(
                            "No se configuró la contraseña del administrador"
                    );
                }

                // Creamos el administrador con los datos iniciales.
                Usuario admin = new Usuario();
                admin.setNombre("Administrador");
                admin.setCorreo("admin@raudskinna.com");

                // BCrypt genera el hash antes de almacenar la contraseña.
                // La contraseña original no se guarda en MySQL.
                admin.setContrasena(passwordEncoder.encode(adminPassword));

                // Asignamos explícitamente los privilegios administrativos.
                admin.setRol(Rol.ADMIN);

                // Guardamos el registro mediante Spring Data JPA.
                usuarioRepository.save(admin);

                // Confirmamos la creación sin mostrar credenciales.
                System.out.println(
                        "Usuario administrador inicial creado correctamente."
                );
            }
        };
    }
}