package com.itson.ProyectoAplicacionesWeb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final String ubicacionUploads;

    public WebMvcConfig(
            @Value("${app.uploads.dir:uploads}") String directorioBase) {

        String ubicacion = Path.of(directorioBase)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        // Spring exige la barra final en las ubicaciones de directorios.
        this.ubicacionUploads =
                ubicacion.endsWith("/") ? ubicacion : ubicacion + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(ubicacionUploads);
    }
}
