package com.itson.ProyectoAplicacionesWeb.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de CORS para la API REST.
 *
 * El frontend se ejecuta en otro origen (por ejemplo Live Server en
 * http://localhost:5500), por lo que el navegador envía peticiones
 * OPTIONS antes de las solicitudes con JSON o con el token JWT.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500}")
    private String allowedOrigins;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();

        configuracion.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origen -> !origen.isEmpty())
                        .toList()
        );
        configuracion.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        configuracion.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept")
        );
        configuracion.setExposedHeaders(List.of("Location"));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente =
                new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);

        return fuente;
    }
}
