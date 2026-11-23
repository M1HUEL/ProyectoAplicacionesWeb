package com.itson.ProyectoAplicacionesWeb.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.util.Optional;

/**
 * Almacenamiento en Cloudflare R2.
 *
 * R2 expone una API compatible con S3, por lo que se utiliza el cliente
 * de AWS SDK apuntando al endpoint de la cuenta. Se activa únicamente
 * cuando {@code app.archivos.proveedor=cloudflare}.
 */
@Component
@ConditionalOnProperty(
        name = "app.archivos.proveedor",
        havingValue = "cloudflare"
)
public class AlmacenamientoCloudflareR2Archivos
        implements AlmacenamientoArchivos {

    private final S3Client cliente;
    private final String bucket;
    private final String urlPublicaBase;

    public AlmacenamientoCloudflareR2Archivos(
            @Value("${app.cloudflare.r2.account-id:}") String accountId,
            @Value("${app.cloudflare.r2.bucket:}") String bucket,
            @Value("${app.cloudflare.r2.access-key-id:}") String accessKeyId,
            @Value("${app.cloudflare.r2.secret-access-key:}") String secretAccessKey,
            @Value("${app.cloudflare.r2.url-publica:}") String urlPublica) {

        validarConfiguracion(
                accountId, bucket, accessKeyId, secretAccessKey, urlPublica
        );

        this.bucket = bucket.trim();
        this.urlPublicaBase = quitarBarraFinal(urlPublica.trim());

        this.cliente = S3Client.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create(
                        "https://" + accountId.trim()
                                + ".r2.cloudflarestorage.com"
                ))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                accessKeyId.trim(), secretAccessKey.trim()
                        )
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    @Override
    public String guardar(String clave, byte[] contenido) {
        AlmacenamientoArchivos.validarClave(clave);

        cliente.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(clave)
                        .build(),
                RequestBody.fromBytes(contenido)
        );

        return clave;
    }

    @Override
    public Optional<byte[]> leer(String clave) {
        AlmacenamientoArchivos.validarClave(clave);

        try {
            ResponseBytes<GetObjectResponse> objeto = cliente.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(clave)
                            .build()
            );

            return Optional.of(objeto.asByteArray());

        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public void eliminar(String referencia) {
        String clave = claveDe(referencia);

        if (clave == null) {
            return;
        }

        AlmacenamientoArchivos.validarClave(clave);

        cliente.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(clave)
                        .build()
        );
    }

    @Override
    public String urlPublica(String clave) {
        AlmacenamientoArchivos.validarClave(clave);
        return urlPublicaBase + "/" + clave;
    }

    @Override
    public String baseUrl() {
        return urlPublicaBase + "/";
    }

    private void validarConfiguracion(
            String accountId,
            String bucket,
            String accessKeyId,
            String secretAccessKey,
            String urlPublica) {

        if (estaVacio(accountId) || estaVacio(bucket)
                || estaVacio(accessKeyId) || estaVacio(secretAccessKey)
                || estaVacio(urlPublica)) {

            throw new IllegalStateException(
                    "Cloudflare R2 está activo pero faltan variables de "
                            + "configuración. Revisa CLOUDFLARE_ACCOUNT_ID, "
                            + "CLOUDFLARE_R2_BUCKET, "
                            + "CLOUDFLARE_R2_ACCESS_KEY_ID, "
                            + "CLOUDFLARE_R2_SECRET_ACCESS_KEY y "
                            + "CLOUDFLARE_R2_PUBLIC_URL."
            );
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String quitarBarraFinal(String valor) {
        return valor.endsWith("/")
                ? valor.substring(0, valor.length() - 1)
                : valor;
    }
}
