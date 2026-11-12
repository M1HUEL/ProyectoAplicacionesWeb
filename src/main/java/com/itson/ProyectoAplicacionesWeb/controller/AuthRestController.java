
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LoginRequestDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LoginResponseDTO;
import com.itson.ProyectoAplicacionesWeb.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthRestController(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Autentica al usuario y devuelve un JWT.
     *
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO solicitud) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        solicitud.correo(),
                        solicitud.contrasena()
                )
        );

        String token = jwtService.generarToken(authentication);

        LoginResponseDTO respuesta = new LoginResponseDTO(
                token,
                "Bearer",
                jwtService.obtenerDuracionSegundos()
        );

        return ResponseEntity.ok(respuesta);
    }
}
