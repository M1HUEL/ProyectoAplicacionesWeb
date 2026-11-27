package com.itson.ProyectoAplicacionesWeb.storage;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba que las referencias guardadas en la base de datos se
 * resuelvan con el proveedor activo, sin migrar registros.
 */
class ResolvedorUrlsArchivosTest {

    /** Proveedor en memoria con el mismo contrato que los reales. */
    private static class AlmacenamientoFalso
            implements AlmacenamientoArchivos {

        private final String baseUrl;
        private final Map<String, byte[]> archivos = new HashMap<>();

        AlmacenamientoFalso(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @Override
        public String guardar(String clave, byte[] contenido) {
            archivos.put(clave, contenido);
            return clave;
        }

        @Override
        public Optional<byte[]> leer(String clave) {
            return Optional.ofNullable(archivos.get(clave));
        }

        @Override
        public void eliminar(String referencia) {
            archivos.remove(claveDe(referencia));
        }

        @Override
        public String urlPublica(String clave) {
            return baseUrl + clave;
        }

        @Override
        public String baseUrl() {
            return baseUrl;
        }
    }

    @Test
    void proveedorLocal_resuelveClavesYRutasHistoricas() {
        ResolvedorUrlsArchivos resolvedor = new ResolvedorUrlsArchivos(
                new AlmacenamientoFalso("/uploads/")
        );

        assertEquals("/uploads/portadas/a.png",
                resolvedor.urlPublica("portadas/a.png"));
        assertEquals("/uploads/portadas/a.png",
                resolvedor.urlPublica("/uploads/portadas/a.png"));
    }

    @Test
    void proveedorRemoto_resuelveClavesYRutasHistoricas() {
        ResolvedorUrlsArchivos resolvedor = new ResolvedorUrlsArchivos(
                new AlmacenamientoFalso("https://cdn.ejemplo.com/")
        );

        assertEquals("https://cdn.ejemplo.com/portadas/a.png",
                resolvedor.urlPublica("portadas/a.png"));
        assertEquals("https://cdn.ejemplo.com/portadas/a.png",
                resolvedor.urlPublica("/uploads/portadas/a.png"));
        assertEquals("https://cdn.ejemplo.com/portadas/a.png",
                resolvedor.urlPublica(
                        "https://cdn.ejemplo.com/portadas/a.png"
                ));
    }

    @Test
    void respetaUrlExternasYRecursosEstaticos() {
        ResolvedorUrlsArchivos resolvedor = new ResolvedorUrlsArchivos(
                new AlmacenamientoFalso("https://cdn.ejemplo.com/")
        );

        assertEquals("https://otro-sitio.com/portada.png",
                resolvedor.urlPublica("https://otro-sitio.com/portada.png"));
        assertEquals("/img/portadas/dune.png",
                resolvedor.urlPublica("/img/portadas/dune.png"));
        assertNull(resolvedor.urlPublica(null));
    }

    @Test
    void claveDe_extraeLaClaveDeCadaFormatoDeReferencia() {
        AlmacenamientoArchivos almacenamiento =
                new AlmacenamientoLocalArchivos("uploads");

        assertEquals("portadas/a.png",
                almacenamiento.claveDe("portadas/a.png"));
        assertEquals("portadas/a.png",
                almacenamiento.claveDe("/uploads/portadas/a.png"));
        assertEquals("portadas/a.png",
                almacenamiento.claveDe(
                        "https://cdn.ejemplo.com/portadas/a.png?token=123"
                ));
        assertNull(almacenamiento.claveDe(null));
    }
}
