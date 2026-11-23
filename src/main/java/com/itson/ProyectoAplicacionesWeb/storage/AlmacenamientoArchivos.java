package com.itson.ProyectoAplicacionesWeb.storage;

import java.util.Optional;

/**
 * Contrato para guardar y recuperar los archivos de la aplicación.
 *
 * Las claves son rutas lógicas relativas, por ejemplo
 * {@code portadas/uuid.png} o {@code descargas/libro.pdf}.
 * La base de datos guarda únicamente esa clave, de modo que cambiar
 * de proveedor (local o Cloudinary) no requiere migrar registros.
 */
public interface AlmacenamientoArchivos {

    /** Guarda el contenido bajo una clave y devuelve la misma clave. */
    String guardar(String clave, byte[] contenido);

    /** Recupera el contenido de una clave, si existe. */
    Optional<byte[]> leer(String clave);

    /**
     * Elimina un archivo a partir de una clave o de una URL pública
     * generada por este proveedor. Las referencias ajenas se ignoran.
     */
    void eliminar(String referencia);

    /** URL pública de una clave (ruta local o URL de Cloudinary). */
    String urlPublica(String clave);

    /** Prefijo que identifica las URL generadas por este proveedor. */
    String baseUrl();

    /**
     * Extrae la clave lógica de una referencia guardada en la base de
     * datos. Comprende claves nuevas, rutas locales históricas
     * (/uploads/...) y URL públicas de cualquier proveedor.
     */
    default String claveDe(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }

        String valor = referencia.trim();

        int finEsquema = valor.indexOf("://");
        if (finEsquema >= 0) {
            int inicioClave = valor.indexOf('/', finEsquema + 3);
            if (inicioClave < 0) {
                return null;
            }
            valor = valor.substring(inicioClave + 1);
        } else if (valor.startsWith("/uploads/")) {
            valor = valor.substring("/uploads/".length());
        } else if (valor.startsWith("/")) {
            valor = valor.substring(1);
        }

        int separador = valor.indexOf('?');
        if (separador >= 0) {
            valor = valor.substring(0, separador);
        }

        separador = valor.indexOf('#');
        if (separador >= 0) {
            valor = valor.substring(0, separador);
        }

        return valor.isEmpty() ? null : valor;
    }

    /** Evita rutas absolutas y rutas relativas que salgan del directorio. */
    static void validarClave(String clave) {
        if (clave == null || clave.isBlank()
                || clave.startsWith("/")
                || clave.contains("\\")
                || clave.contains("..")) {
            throw new IllegalArgumentException(
                    "La clave de archivo no es válida."
            );
        }
    }
}
