package com.itson.ProyectoAplicacionesWeb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PortadaServiceTest {
    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
    };

    private static final byte[] JPG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final byte[] WEBP = {
            'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'
    };

    @TempDir
    Path directorioTemporal;

    private PortadaService servicio;

    @BeforeEach
    void configurar() {
        servicio = new PortadaService(directorioTemporal.toString());
    }

    private Path archivoGuardado(String url) {
        String nombre = url.substring(PortadaService.URL_BASE.length());
        return directorioTemporal.resolve("portadas").resolve(nombre);
    }

    @Test
    void guardar_sinArchivo_devuelveNull() {
        assertNull(servicio.guardar(null));

        assertNull(servicio.guardar(
                new MockMultipartFile("portada", new byte[0])));
    }

    @Test
    void guardar_imagenesValidas_guardaYDevuelveUrlPublica() throws Exception {

        String urlPng = servicio.guardar(
                new MockMultipartFile("portada", "a.png", "image/png", PNG));
        String urlJpg = servicio.guardar(
                new MockMultipartFile("portada", "a.jpg", "image/jpeg", JPG));
        String urlWebp = servicio.guardar(
                new MockMultipartFile("portada", "a.webp", "image/webp", WEBP));

        assertTrue(urlPng.startsWith(PortadaService.URL_BASE));
        assertTrue(urlPng.endsWith(".png"));
        assertTrue(urlJpg.endsWith(".jpg"));
        assertTrue(urlWebp.endsWith(".webp"));

        assertTrue(Files.exists(archivoGuardado(urlPng)));
        assertTrue(Files.exists(archivoGuardado(urlJpg)));
        assertTrue(Files.exists(archivoGuardado(urlWebp)));
        assertEquals(PNG.length, Files.size(archivoGuardado(urlPng)));
    }

    @Test
    void guardar_tipoNoPermitido_debeRechazar() {

        assertThrows(IllegalArgumentException.class, () ->
                servicio.guardar(new MockMultipartFile(
                        "portada", "a.gif", "image/gif", PNG)));

        assertThrows(IllegalArgumentException.class, () ->
                servicio.guardar(new MockMultipartFile(
                        "portada", "a.html", "text/html", "<html>".getBytes())));
    }

    @Test
    void guardar_contenidoQueNoCoincideConElTipo_debeRechazar() {

        // Declara ser PNG pero el contenido es texto.
        assertThrows(IllegalArgumentException.class, () ->
                servicio.guardar(new MockMultipartFile(
                        "portada", "falso.png", "image/png",
                        "esto no es una imagen".getBytes())));
    }

    @Test
    void guardar_archivoMuyGrande_debeRechazar() {

        byte[] grande = new byte[(int) PortadaService.TAMANO_MAXIMO_BYTES + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);

        assertThrows(IllegalArgumentException.class, () ->
                servicio.guardar(new MockMultipartFile(
                        "portada", "grande.png", "image/png", grande)));
    }

    @Test
    void eliminar_borraElArchivoGuardado() {

        String url = servicio.guardar(
                new MockMultipartFile("portada", "a.png", "image/png", PNG));

        assertTrue(Files.exists(archivoGuardado(url)));

        servicio.eliminar(url);

        assertFalse(Files.exists(archivoGuardado(url)));
    }

    @Test
    void eliminar_ignoraUrlExternasNulasYRutasPeligrosas() throws Exception {

        // Archivo ajeno al directorio de portadas.
        Path ajeno = directorioTemporal.resolve("secreto.txt");
        Files.writeString(ajeno, "no borrar");

        assertDoesNotThrow(() -> servicio.eliminar(null));
        assertDoesNotThrow(() -> servicio.eliminar("https://ejemplo.com/a.jpg"));
        assertDoesNotThrow(() ->
                servicio.eliminar(PortadaService.URL_BASE + "../secreto.txt"));

        assertTrue(Files.exists(ajeno));
    }
}
