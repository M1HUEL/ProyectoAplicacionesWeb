
package com.itson.ProyectoAplicacionesWeb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Servicio encargado de generar tokens JWT
 * para los usuarios autenticados de la API REST.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {

        this.jwtEncoder = jwtEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    public String generarToken(Authentication authentication) {

        Instant ahora = Instant.now();

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(autoridad -> autoridad.getAuthority())
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(authentication.getName())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(
                        expirationSeconds,
                        ChronoUnit.SECONDS
                ))
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public long obtenerDuracionSegundos() {
        return expirationSeconds;
    }
}
