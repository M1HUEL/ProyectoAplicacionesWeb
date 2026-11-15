
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.LoginRequestDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LoginResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.RegistroRequestDTO;
import com.itson.ProyectoAplicacionesWeb.dto.RegistroResponseDTO;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.service.AutenticacionService;
import com.itson.ProyectoAplicacionesWeb.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    private final AutenticacionService autenticacionService;

    public AuthRestController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AutenticacionService autenticacionService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.autenticacionService = autenticacionService;
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

    /**
     * Registra una nueva cuenta de cliente.
     *
     * POST /api/auth/registro
     */
    @PostMapping("/registro")
    public ResponseEntity<RegistroResponseDTO> registrar(
            @Valid @RequestBody RegistroRequestDTO solicitud) {

        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre());
        usuario.setCorreo(solicitud.correo());
        usuario.setContrasena(solicitud.contrasena());

        // Reutilizamos la lógica existente de registro.
        autenticacionService.registrar(usuario);

        RegistroResponseDTO respuesta = new RegistroResponseDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol().name()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }
}
