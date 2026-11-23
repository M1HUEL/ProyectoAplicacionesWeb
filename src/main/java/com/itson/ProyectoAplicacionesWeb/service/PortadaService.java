package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.storage.AlmacenamientoArchivos;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Valida y administra las imágenes de portada.
 *
 * Las decisiones de dónde se guarda el archivo se delegan a
 * {@link AlmacenamientoArchivos}; este servicio conserva únicamente
 * las reglas de negocio sobre el contenido de la imagen.
 */
@Service
public class PortadaService {

    /** Prefijo de las claves de portada dentro del almacenamiento. */
    public static final String PREFIJO_CLAVES = "portadas/";

    /** Tamaño máximo permitido por imagen: 2 MB. */
    static final long TAMANO_MAXIMO_BYTES = 2L * 1024 * 1024;

    private final AlmacenamientoArchivos almacenamiento;

    public PortadaService(AlmacenamientoArchivos almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    /**
     * Valida y guarda una imagen de portada.
     *
     * Se comprueba el tipo declarado y los primeros bytes del archivo
     * para evitar que se publique contenido que no sea una imagen.
     *
     * @param archivo archivo recibido del formulario (puede ser null).
     * @return clave de la imagen, o null si no se envió archivo.
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

        String clave = PREFIJO_CLAVES + UUID.randomUUID() + extension;

        try (InputStream entrada = archivo.getInputStream()) {
            byte[] cabecera = entrada.readNBytes(12);

            if (!coincideConFirma(cabecera, extension)) {
                throw new IllegalArgumentException(
                        "El archivo no es una imagen válida."
                );
            }

            almacenamiento.guardar(clave, archivo.getBytes());

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "No fue posible guardar la imagen. Inténtalo de nuevo.", e
            );
        }

        return clave;
    }

    /**
     * Elimina una imagen guardada por este servicio.
     *
     * Ignora valores nulos, URL externas y cualquier referencia que no
     * pertenezca al directorio de portadas.
     */
    public void eliminar(String referencia) {

        if (referencia == null || referencia.isBlank()) {
            return;
        }

        String valor = referencia.trim();

        // Nunca se elimina un objeto a partir de una URL externa.
        if (valor.contains("://")
                && !valor.startsWith(almacenamiento.baseUrl())) {
            return;
        }

        String clave = almacenamiento.claveDe(valor);

        if (clave == null || !clave.startsWith(PREFIJO_CLAVES)) {
            return;
        }

        almacenamiento.eliminar(clave);
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
