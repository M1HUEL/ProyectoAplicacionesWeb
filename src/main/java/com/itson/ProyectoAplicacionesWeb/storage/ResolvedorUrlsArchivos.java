package com.itson.ProyectoAplicacionesWeb.storage;

import org.springframework.stereotype.Component;

/**
 * Convierte la referencia almacenada en la base de datos en la URL que
 * debe utilizarse con el proveedor activo.
 *
 * Así las portadas guardadas con el proveedor local siguen funcionando
 * después de cambiar a Cloudinary (y viceversa), siempre que las
 * claves se hayan migrado al otro proveedor.
 */
@Component
public class ResolvedorUrlsArchivos {

    private final AlmacenamientoArchivos almacenamiento;

    public ResolvedorUrlsArchivos(AlmacenamientoArchivos almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    /**
     * Devuelve la URL pública de una referencia guardada en
     * {@code Libro.portadaUrl}.
     *
     * Se respetan las URL externas escritas por el administrador y las
     * imágenes estáticas del catálogo de demostración (/img/...).
     */
    public String urlPublica(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return referencia;
        }

        String valor = referencia.trim();

        if (valor.startsWith("/img/")) {
            return valor;
        }

        if (esReferenciaPropia(valor)) {
            String clave = almacenamiento.claveDe(valor);
            return clave == null ? valor : almacenamiento.urlPublica(clave);
        }

        if (valor.contains("://")) {
            return valor;
        }

        return almacenamiento.urlPublica(valor);
    }

    private boolean esReferenciaPropia(String valor) {
        return valor.startsWith("/uploads/")
                || valor.startsWith(almacenamiento.baseUrl());
    }
}
