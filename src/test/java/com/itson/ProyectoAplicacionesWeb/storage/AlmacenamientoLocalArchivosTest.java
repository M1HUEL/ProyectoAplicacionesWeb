package com.itson.ProyectoAplicacionesWeb.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del proveedor local de almacenamiento.
 */
class AlmacenamientoLocalArchivosTest {

    @TempDir
    Path directorioTemporal;

    private AlmacenamientoArchivos almacenamiento;

    @BeforeEach
    void configurar() {
        almacenamiento =
                new AlmacenamientoLocalArchivos(directorioTemporal.toString());
    }

    @Test
    void guardarYLeer_devuelveElMismoContenido() {
        byte[] contenido = "contenido de prueba".getBytes();

        String clave = almacenamiento.guardar("portadas/a.png", contenido);

        assertEquals("portadas/a.png", clave);
        assertTrue(Files.exists(
                directorioTemporal.resolve("portadas").resolve("a.png")
        ));
        assertArrayEquals(
                contenido,
                almacenamiento.leer(clave).orElseThrow()
        );
        assertEquals("/uploads/portadas/a.png",
                almacenamiento.urlPublica(clave));
    }

    @Test
    void leer_archivoInexistente_devuelveVacio() {
        assertEquals(Optional.empty(),
                almacenamiento.leer("portadas/inexistente.png"));
    }

    @Test
    void leer_pdfDeDemostracion_loRecuperaDelClasspath() {
        byte[] pdf = almacenamiento.leer(
                "descargas/raudskinna-muestra-digital.pdf"
        ).orElseThrow();

        assertEquals("%PDF",
                new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)
        );
    }

    @Test
    void eliminar_aceptaClaveYUrlPublica() {
        String clave = almacenamiento.guardar(
                "portadas/a.png", "x".getBytes()
        );

        almacenamiento.eliminar(clave);
        assertFalse(Files.exists(directorioTemporal.resolve(clave)));

        almacenamiento.guardar("portadas/b.png", "x".getBytes());
        almacenamiento.eliminar("/uploads/portadas/b.png");
        assertFalse(Files.exists(directorioTemporal.resolve("portadas/b.png")));
    }

    @Test
    void guardar_claveInvalida_debeRechazar() {
        assertThrows(IllegalArgumentException.class,
                () -> almacenamiento.guardar("../secreto.txt", new byte[0]));

        assertThrows(IllegalArgumentException.class,
                () -> almacenamiento.guardar("/absoluto.txt", new byte[0]));
    }
}
