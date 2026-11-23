package com.itson.ProyectoAplicacionesWeb.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Almacenamiento en el sistema de archivos local.
 *
 * Es el proveedor predeterminado y mantiene el comportamiento original:
 * los archivos quedan bajo {@code app.uploads.dir} y se publican en
 * {@code /uploads/**} mediante WebMvcConfig.
 */
@Component
@ConditionalOnProperty(
        name = "app.archivos.proveedor",
        havingValue = "local",
        matchIfMissing = true
)
public class AlmacenamientoLocalArchivos implements AlmacenamientoArchivos {

    /** Prefijo público de los archivos servidos por Spring MVC. */
    public static final String URL_BASE = "/uploads/";

    private final Path directorio;

    public AlmacenamientoLocalArchivos(
            @Value("${app.uploads.dir:uploads}") String directorioBase) {

        this.directorio = Path.of(directorioBase)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public String guardar(String clave, byte[] contenido) {
        AlmacenamientoArchivos.validarClave(clave);

        Path destino = rutaSegura(clave);

        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No fue posible guardar el archivo local.", e
            );
        }

        return clave;
    }

    @Override
    public Optional<byte[]> leer(String clave) {
        AlmacenamientoArchivos.validarClave(clave);

        Path archivo = rutaSegura(clave);

        try {
            if (Files.exists(archivo)) {
                return Optional.of(Files.readAllBytes(archivo));
            }

            /*
             * Compatibilidad con el PDF de demostración: el proyecto lo
             * distribuye dentro del JAR, no en la carpeta de uploads.
             */
            ClassPathResource recurso = new ClassPathResource(clave);
            if (recurso.exists()) {
                try (InputStream entrada = recurso.getInputStream()) {
                    return Optional.of(entrada.readAllBytes());
                }
            }

            return Optional.empty();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No fue posible leer el archivo local.", e
            );
        }
    }

    @Override
    public void eliminar(String referencia) {
        String clave = claveDe(referencia);

        if (clave == null) {
            return;
        }

        try {
            AlmacenamientoArchivos.validarClave(clave);
            Files.deleteIfExists(rutaSegura(clave));
        } catch (IllegalArgumentException | IOException e) {
            // No se interrumpe la operación por un archivo huérfano.
        }
    }

    @Override
    public String urlPublica(String clave) {
        AlmacenamientoArchivos.validarClave(clave);
        return URL_BASE + clave;
    }

    @Override
    public String baseUrl() {
        return URL_BASE;
    }

    /** Resuelve la clave dentro del directorio configurado. */
    private Path rutaSegura(String clave) {
        Path ruta = directorio.resolve(clave).normalize();

        if (!ruta.startsWith(directorio)) {
            throw new IllegalArgumentException(
                    "La clave de archivo no es válida."
            );
        }

        return ruta;
    }
}
