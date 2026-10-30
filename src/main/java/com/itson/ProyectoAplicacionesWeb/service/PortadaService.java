package com.itson.ProyectoAplicacionesWeb.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

@Service
public class PortadaService {
    /** Prefijo público con el que se guardan las URL en Libro.portadaUrl. */
    public static final String URL_BASE = "/uploads/portadas/";

    /** Tamaño máximo permitido por imagen: 2 MB. */
    static final long TAMANO_MAXIMO_BYTES = 2L * 1024 * 1024;

    private final Path directorio;

    public PortadaService(
            @Value("${app.uploads.dir:uploads}") String directorioBase) {
        this.directorio = Path.of(directorioBase)
                .toAbsolutePath()
                .normalize()
                .resolve("portadas");
    }

    /**
     * Valida y guarda una imagen de portada.
     *
     * Se comprueba el tipo declarado y los primeros bytes del archivo
     * para evitar que se publique contenido que no sea una imagen.
     *
     * @param archivo archivo recibido del formulario (puede ser null).
     * @return URL pública de la imagen, o null si no se envió archivo.
     * @throws IllegalArgumentException si el archivo no es válido.
     */
    public String guardar(MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            return null;
        }

        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException(
                    "La imagen de portada no puede superar los 2 MB."
            );
        }

        String extension = extensionPorTipo(archivo.getContentType());

        if (extension == null) {
            throw new IllegalArgumentException(
                    "La portada debe ser una imagen JPG, PNG o WEBP."
            );
        }

        String nombre = UUID.randomUUID() + extension;

        try (InputStream entrada = archivo.getInputStream()) {

            byte[] cabecera = entrada.readNBytes(12);

            if (!coincideConFirma(cabecera, extension)) {
                throw new IllegalArgumentException(
                        "El archivo no es una imagen válida."
                );
            }

            Files.createDirectories(directorio);

            // Se vuelve a abrir el archivo para copiarlo completo.
            try (InputStream completa = archivo.getInputStream()) {
                Files.copy(completa, directorio.resolve(nombre));
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "No fue posible guardar la imagen. Inténtalo de nuevo.", e
            );
        }

        return URL_BASE + nombre;
    }

    /**
     * Elimina una imagen guardada por este servicio.
     *
     * Ignora valores nulos, URL externas y cualquier ruta que no
     * pertenezca al directorio de portadas.
     */
    public void eliminar(String url) {

        if (url == null || !url.startsWith(URL_BASE)) {
            return;
        }

        String nombre = url.substring(URL_BASE.length());

        if (nombre.isBlank()
                || nombre.contains("/")
                || nombre.contains("\\")
                || nombre.contains("..")) {
            return;
        }

        try {
            Files.deleteIfExists(directorio.resolve(nombre));
        } catch (IOException e) {
            // No se interrumpe la operación del administrador
            // por un archivo huérfano.
        }
    }

    private String extensionPorTipo(String tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> null;
        };
    }

    private boolean coincideConFirma(byte[] cabecera, String extension) {
        return switch (extension) {
            case ".jpg" -> cabecera.length >= 3
                    && (cabecera[0] & 0xFF) == 0xFF
                    && (cabecera[1] & 0xFF) == 0xD8
                    && (cabecera[2] & 0xFF) == 0xFF;
            case ".png" -> cabecera.length >= 8
                    && Arrays.equals(
                    Arrays.copyOf(cabecera, 8),
                    new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            case ".webp" -> cabecera.length >= 12
                    && cabecera[0] == 'R' && cabecera[1] == 'I'
                    && cabecera[2] == 'F' && cabecera[3] == 'F'
                    && cabecera[8] == 'W' && cabecera[9] == 'E'
                    && cabecera[10] == 'B' && cabecera[11] == 'P';
            default -> false;
        };
    }
}

