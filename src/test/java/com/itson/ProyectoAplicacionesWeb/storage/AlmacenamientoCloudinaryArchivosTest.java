package com.itson.ProyectoAplicacionesWeb.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del proveedor Cloudinary sin contactar con el servicio.
 * Las URL se generan con el SDK real y las operaciones de red se simulan.
 */
class AlmacenamientoCloudinaryArchivosTest {

    private Cloudinary cliente;
    private Uploader uploader;
    private AlmacenamientoCloudinaryArchivos.DescargadorUrls descargador;
    private AlmacenamientoCloudinaryArchivos almacenamiento;

    @BeforeEach
    void configurar() {
        Cloudinary clienteReal = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "demo",
                "api_key", "clave",
                "api_secret", "secreto",
                "secure", true,
                "analytics", false
        ));

        cliente = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        descargador = mock(AlmacenamientoCloudinaryArchivos.DescargadorUrls.class);

        when(cliente.uploader()).thenReturn(uploader);
        when(cliente.url()).thenAnswer(invocacion -> clienteReal.url());

        almacenamiento = new AlmacenamientoCloudinaryArchivos(
                cliente, "demo", "", descargador
        );
    }

    @Test
    void guardar_imagenUsaPublicIdYTipoImage() throws IOException {
        byte[] contenido = "imagen".getBytes(StandardCharsets.UTF_8);
        when(uploader.upload(any(), anyMap()))
                .thenReturn(Map.of("public_id", "portadas/a"));

        String clave = almacenamiento.guardar("portadas/a.png", contenido);

        assertEquals("portadas/a.png", clave);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> opciones =
                ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(contenido), opciones.capture());

        assertEquals("portadas/a", opciones.getValue().get("public_id"));
        assertEquals("image", opciones.getValue().get("resource_type"));
        assertEquals("a.png", opciones.getValue().get("filename"));
        assertEquals(true, opciones.getValue().get("overwrite"));
        assertEquals(true, opciones.getValue().get("invalidate"));
    }

    @Test
    void guardar_pdfUsaTipoRaw() throws IOException {
        when(uploader.upload(any(), anyMap()))
                .thenReturn(Map.of("public_id", "descargas/libro"));

        almacenamiento.guardar(
                "descargas/libro.pdf", new byte[]{1, 2, 3}
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> opciones =
                ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), opciones.capture());

        assertEquals("raw", opciones.getValue().get("resource_type"));
        assertEquals("libro.pdf", opciones.getValue().get("filename"));
    }

    @Test
    void guardar_errorDeCloudinaryLanzaExcepcion() throws IOException {
        when(uploader.upload(any(), anyMap()))
                .thenReturn(Map.of("error", "No autorizado"));

        assertThrows(IllegalStateException.class,
                () -> almacenamiento.guardar("portadas/a.png", new byte[]{1}));
    }

    @Test
    void leer_descargaDesdeLaUrlPublicaDeLaClave() {
        byte[] contenido = "%PDF-1.4 contenido".getBytes(StandardCharsets.US_ASCII);
        when(descargador.descargar(anyString()))
                .thenReturn(Optional.of(contenido));

        Optional<byte[]> resultado =
                almacenamiento.leer("descargas/raudskinna-muestra-digital.pdf");

        assertTrue(resultado.isPresent());
        assertArrayEquals(contenido, resultado.get());
        verify(descargador).descargar(
                "https://res.cloudinary.com/demo/raw/upload/"
                        + "descargas/raudskinna-muestra-digital.pdf"
        );
    }

    @Test
    void leer_archivoInexistenteDevuelveVacio() {
        when(descargador.descargar(anyString()))
                .thenReturn(Optional.empty());

        assertTrue(almacenamiento.leer("portadas/ausente.png").isEmpty());
    }

    @Test
    void eliminar_aceptaClaveYUrlGeneradaPorCloudinary() throws IOException {
        when(uploader.destroy(anyString(), anyMap()))
                .thenReturn(Map.of("result", "ok"));

        almacenamiento.eliminar("portadas/a.png");
        almacenamiento.eliminar(
                "https://res.cloudinary.com/demo/image/upload/v7/portadas/a.png"
        );

        verify(uploader, times(2)).destroy(eq("portadas/a"), anyMap());
    }

    @Test
    void eliminar_archivoQueYaNoExisteNoFalla() throws IOException {
        when(uploader.destroy(anyString(), anyMap()))
                .thenReturn(Map.of("result", "not found"));

        assertDoesNotThrow(() -> almacenamiento.eliminar("portadas/a.png"));
    }

    @Test
    void eliminar_urlExternaSeIgnora() throws IOException {
        almacenamiento.eliminar("https://otro-sitio.com/portadas/a.png");

        verify(uploader, never()).destroy(anyString(), anyMap());
    }

    @Test
    void urlPublica_generaUrlDelProveedorActivo() {
        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/portadas/a.png",
                almacenamiento.urlPublica("portadas/a.png")
        );
        assertEquals(
                "https://res.cloudinary.com/demo/raw/upload/descargas/libro.pdf",
                almacenamiento.urlPublica("descargas/libro.pdf")
        );
        assertEquals("https://res.cloudinary.com/demo/", almacenamiento.baseUrl());
    }

    @Test
    void claveDe_comprendeClavesRutasHistoricasYUrlDeCloudinary() {
        assertEquals("portadas/a.png",
                almacenamiento.claveDe("portadas/a.png"));
        assertEquals("portadas/a.png",
                almacenamiento.claveDe("/uploads/portadas/a.png"));
        assertEquals("portadas/a.png",
                almacenamiento.claveDe(
                        "https://res.cloudinary.com/demo/image/upload/"
                                + "portadas/a.png"
                ));
        assertEquals("descargas/libro.pdf",
                almacenamiento.claveDe(
                        "https://res.cloudinary.com/demo/raw/upload/v12/"
                                + "descargas/libro.pdf?token=abc"
                ));
        assertNull(almacenamiento.claveDe(null));
    }

    @Test
    void resolvedorConvierteReferenciasAntiguasSinMigrarLaBaseDeDatos() {
        ResolvedorUrlsArchivos resolvedor =
                new ResolvedorUrlsArchivos(almacenamiento);

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/portadas/a.png",
                resolvedor.urlPublica("portadas/a.png")
        );
        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/portadas/a.png",
                resolvedor.urlPublica("/uploads/portadas/a.png")
        );
        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/portadas/a.png",
                resolvedor.urlPublica(
                        "https://res.cloudinary.com/demo/image/upload/"
                                + "v3/portadas/a.png"
                )
        );
    }

    @Test
    void dominioDeEntregaPropioSeUsaEnUrlsYClaves() {
        Cloudinary clienteReal = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "demo",
                "api_key", "clave",
                "api_secret", "secreto",
                "secure", true,
                "analytics", false,
                "secure_distribution", "cdn.ejemplo.com",
                "private_cdn", true
        ));
        AlmacenamientoCloudinaryArchivos conDominio =
                new AlmacenamientoCloudinaryArchivos(
                        clienteReal, "demo", "cdn.ejemplo.com", descargador
                );

        assertEquals("https://cdn.ejemplo.com/", conDominio.baseUrl());
        assertEquals(
                "https://cdn.ejemplo.com/image/upload/portadas/a.png",
                conDominio.urlPublica("portadas/a.png")
        );
        assertEquals("portadas/a.png", conDominio.claveDe(
                "https://cdn.ejemplo.com/image/upload/portadas/a.png"
        ));
    }

    @Test
    void constructorValidaLasCredencialesObligatorias() {
        assertThrows(IllegalStateException.class,
                () -> new AlmacenamientoCloudinaryArchivos(
                        "", "clave", "secreto", ""
                ));
        assertThrows(IllegalStateException.class,
                () -> new AlmacenamientoCloudinaryArchivos(
                        "demo", "", "secreto", ""
                ));
        assertThrows(IllegalStateException.class,
                () -> new AlmacenamientoCloudinaryArchivos(
                        "demo", "clave", "", ""
                ));
    }

    @Test
    void constructorDeSpringConstruyeElClienteReal() {
        AlmacenamientoCloudinaryArchivos real =
                new AlmacenamientoCloudinaryArchivos(
                        "demo", "clave", "secreto", ""
                );

        assertEquals("https://res.cloudinary.com/demo/", real.baseUrl());
        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/portadas/a.png",
                real.urlPublica("portadas/a.png")
        );
    }

    @Test
    void constructorDeSpringAceptaDominioDeEntregaPropio() {
        AlmacenamientoCloudinaryArchivos real =
                new AlmacenamientoCloudinaryArchivos(
                        "demo", "clave", "secreto", "cdn.ejemplo.com"
                );

        assertEquals("https://cdn.ejemplo.com/", real.baseUrl());
        assertEquals(
                "https://cdn.ejemplo.com/image/upload/portadas/a.png",
                real.urlPublica("portadas/a.png")
        );
    }

    @Test
    void clavesInvalidasSeRechazan() {
        assertThrows(IllegalArgumentException.class,
                () -> almacenamiento.guardar("../secreto.txt", new byte[]{1}));
        assertThrows(IllegalArgumentException.class,
                () -> almacenamiento.leer("/absoluto.png"));
    }
}
