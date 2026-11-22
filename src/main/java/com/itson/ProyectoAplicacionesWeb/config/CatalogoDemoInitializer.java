
package com.itson.ProyectoAplicacionesWeb.config;

import com.itson.ProyectoAplicacionesWeb.service.CatalogoDemoService;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Ejecuta la carga del catálogo demostrativo
 * únicamente cuando se activa el perfil demo.
 *
 * No modifica el inicializador del administrador
 * ni se ejecuta en el perfil normal.
 */
@Configuration
@Profile("demo")
public class CatalogoDemoInitializer {

    @Bean
    ApplicationRunner inicializarCatalogoDemo(
            CatalogoDemoService catalogoDemoService) {

        return args -> {
            catalogoDemoService.inicializarCatalogo();

            System.out.println(
                    "Catálogo de demostración verificado."
            );
        };
    }
}
