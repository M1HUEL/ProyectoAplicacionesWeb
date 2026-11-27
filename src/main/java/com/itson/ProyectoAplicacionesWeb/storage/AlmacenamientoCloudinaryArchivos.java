package com.itson.ProyectoAplicacionesWeb.storage;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Almacenamiento en Cloudinary.
 *
 * Las claves lógicas no cambian: {@code portadas/uuid.png} se convierte en
 * un asset de tipo image con public_id {@code portadas/uuid}, mientras que
 * {@code descargas/archivo.pdf} se guarda como raw. La base de datos sigue
 * guardando solo la clave, de modo que no hay que migrar registros al
 * cambiar de proveedor.
 *
 * Se activa únicamente cuando {@code app.archivos.proveedor=cloudinary}.
 */
@Component
@ConditionalOnProperty(
        name = "app.archivos.proveedor",
        havingValue = "cloudinary"
)
public class AlmacenamientoCloudinaryArchivos
        implements AlmacenamientoArchivos {

    private static final String DOMINIO_CLOUDINARY = "https://res.cloudinary.com/";
    private static final Pattern VERSION_URL =
            Pattern.compile("^v\\d+/");

    private final Cloudinary cliente;
    private final String baseUrl;
    private final DescargadorUrls descargador;

    /**
     * Constructor usado por Spring. Las credenciales se leen de variables
     * de entorno y el cliente se construye al arrancar la aplicación.
     */
    public AlmacenamientoCloudinaryArchivos(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret,
            @Value("${app.cloudinary.secure-distribution:}") String distribucionSegura) {

        validarConfiguracion(cloudName, apiKey, apiSecret);

        String nube = cloudName.trim();

        String distribucion = distribucionSegura == null
                ? ""
                : distribucionSegura.trim();

        Map<String, Object> configuracion = new HashMap<>();
        configuracion.put("cloud_name", nube);
        configuracion.put("api_key", apiKey.trim());
        configuracion.put("api_secret", apiSecret.trim());
        configuracion.put("secure", true);
        // Se desactiva el parámetro de analítica para mantener URL estables.
        configuracion.put("analytics", false);

        if (!distribucion.isEmpty()) {
            configuracion.put("secure_distribution", distribucion);
            configuracion.put("private_cdn", true);
        }

        this.cliente = new Cloudinary(configuracion);
        this.baseUrl = distribucion.isEmpty()
                ? DOMINIO_CLOUDINARY + nube + "/"
                : "https://" + distribucion + "/";
        this.descargador = new DescargadorHttp();
    }

    /** Constructor de paquete para las pruebas unitarias. */
    AlmacenamientoCloudinaryArchivos(
            Cloudinary cliente,
            String nombreNube,
            String distribucionSegura,
            DescargadorUrls descargador) {

        this.cliente = cliente;
        this.baseUrl = distribucionSegura == null || distribucionSegura.isBlank()
                ? DOMINIO_CLOUDINARY + nombreNube + "/"
                : "https://" + distribucionSegura.trim() + "/";
        this.descargador = descargador;
    }

    @Override
    public String guardar(String clave, byte[] contenido) {
        AlmacenamientoArchivos.validarClave(clave);

        Map<String, Object> opciones = new HashMap<>();
        opciones.put("public_id", publicId(clave));
        opciones.put("resource_type", tipoRecurso(clave));
        opciones.put("filename", nombreArchivo(clave));
        opciones.put("overwrite", true);
        opciones.put("invalidate", true);

        try {
            Map<?, ?> respuesta = cliente.uploader().upload(contenido, opciones);

            if (respuesta == null || respuesta.get("error") != null) {
                throw new IllegalStateException(
                        "Cloudinary no pudo guardar el archivo.",
                        respuesta == null ? null
                                : new IllegalArgumentException(
                                String.valueOf(respuesta.get("error"))
                        )
                );
            }

            return clave;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No fue posible guardar el archivo en Cloudinary.", e
            );
        }
    }

    @Override
    public Optional<byte[]> leer(String clave) {
        AlmacenamientoArchivos.validarClave(clave);
        return descargador.descargar(urlPublica(clave));
    }

    @Override
    public void eliminar(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return;
        }

        String valor = referencia.trim();

        // Nunca se elimina un asset a partir de una URL externa.
        if (valor.contains("://") && !valor.startsWith(baseUrl)) {
            return;
        }

        String clave = claveDe(valor);

        if (clave == null) {
            return;
        }

        try {
            AlmacenamientoArchivos.validarClave(clave);
        } catch (IllegalArgumentException e) {
            // Referencia huérfana: no se interrumpe la operación.
            return;
        }

        Map<String, Object> opciones = new HashMap<>();
        opciones.put("resource_type", tipoRecurso(clave));

        try {
            Map<?, ?> respuesta = cliente.uploader()
                    .destroy(publicId(clave), opciones);

            String resultado = respuesta == null
                    ? ""
                    : String.valueOf(respuesta.get("result"));

            if (!"ok".equals(resultado) && !"not found".equals(resultado)) {
                throw new IllegalStateException(
                        "Cloudinary no pudo eliminar el archivo: " + resultado
                );
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No fue posible eliminar el archivo de Cloudinary.", e
            );
        }
    }

    @Override
    public String urlPublica(String clave) {
        AlmacenamientoArchivos.validarClave(clave);

        return cliente.url()
                .resourceType(tipoRecurso(clave))
                .format(extension(clave))
                .forceVersion(false)
                .generate(publicId(clave));
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * Comprende claves lógicas, rutas históricas /uploads/... y las URL
     * generadas por Cloudinary, con o sin versión en la ruta.
     */
    @Override
    public String claveDe(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }

        String valor = referencia.trim();

        if (!valor.startsWith(baseUrl)) {
            return AlmacenamientoArchivos.super.claveDe(referencia);
        }

        String resto = valor.substring(baseUrl.length());

        int separador = resto.indexOf('?');
        if (separador >= 0) {
            resto = resto.substring(0, separador);
        }

        separador = resto.indexOf('#');
        if (separador >= 0) {
            resto = resto.substring(0, separador);
        }

        for (String tipo : new String[]{"image", "video", "raw"}) {
            String prefijo = tipo + "/upload/";
            if (resto.startsWith(prefijo)) {
                resto = resto.substring(prefijo.length());
                break;
            }
        }

        Matcher version = VERSION_URL.matcher(resto);
        if (version.find()) {
            resto = resto.substring(version.end());
        }

        /*
         * La extensión de la URL es la extensión de la clave lógica; el
         * public_id de Cloudinary es la misma ruta sin esa extensión.
         */
        return resto.isEmpty() ? null : resto;
    }

    /** Convierte la clave en el public_id de Cloudinary. */
    private String publicId(String clave) {
        int separador = clave.lastIndexOf('.');
        return separador > 0 ? clave.substring(0, separador) : clave;
    }

    /** Extensión sin el punto; cadena vacía si la clave no tiene extensión. */
    private String extension(String clave) {
        int separador = clave.lastIndexOf('.');
        return separador > 0 && separador < clave.length() - 1
                ? clave.substring(separador + 1)
                : "";
    }

    /** Cloudinary necesita el resource_type para subir y borrar el asset. */
    private String tipoRecurso(String clave) {
        return switch (extension(clave).toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "tif", "tiff" -> "image";
            case "mp4", "mov", "avi", "webm" -> "video";
            default -> "raw";
        };
    }

    /** El filename conserva la extensión original de los archivos raw. */
    private String nombreArchivo(String clave) {
        int separador = clave.lastIndexOf('/');
        return separador >= 0 ? clave.substring(separador + 1) : clave;
    }

    private void validarConfiguracion(
            String cloudName, String apiKey, String apiSecret) {

        if (estaVacio(cloudName) || estaVacio(apiKey) || estaVacio(apiSecret)) {
            throw new IllegalStateException(
                    "Cloudinary está activo pero faltan variables de "
                            + "configuración. Revisa CLOUDINARY_CLOUD_NAME, "
                            + "CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET."
            );
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Descarga el contenido desde la URL pública del asset. */
    @FunctionalInterface
    interface DescargadorUrls {
        Optional<byte[]> descargar(String url);
    }

    /** Implementación real basada en el cliente HTTP de Java. */
    private static class DescargadorHttp implements DescargadorUrls {

        private final HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        @Override
        public Optional<byte[]> descargar(String url) {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            try {
                HttpResponse<byte[]> respuesta = httpClient.send(
                        peticion, HttpResponse.BodyHandlers.ofByteArray()
                );

                return switch (respuesta.statusCode()) {
                    case 200 -> Optional.of(respuesta.body());
                    case 404 -> Optional.empty();
                    default -> throw new IllegalStateException(
                            "No fue posible descargar el archivo de "
                                    + "Cloudinary (HTTP "
                                    + respuesta.statusCode() + ")."
                    );
                };

            } catch (IOException e) {
                throw new IllegalStateException(
                        "No fue posible descargar el archivo de Cloudinary.", e
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "La descarga desde Cloudinary fue interrumpida.", e
                );
            }
        }
    }
}
