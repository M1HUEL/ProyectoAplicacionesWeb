package com.itson.ProyectoAplicacionesWeb.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.service.UsuarioService;

/**
 * Endpoints administrativos para consultar usuarios.
 *
 * Devuelve JSON y no depende de plantillas Thymeleaf.
 * El acceso queda restringido al rol ADMIN tanto por la regla
 * de SecurityConfig como por @PreAuthorize.
 */
@RestController
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Lista todas las cuentas registradas.
     *
     * GET /usuarios
     */
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listarTodos()
                .stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    /**
     * Consulta una cuenta por su identificador.
     *
     * GET /usuarios/{id}
     */
    @GetMapping("/{id}")
    public UsuarioResponse ver(@PathVariable Long id) {
        return usuarioService.buscarPorId(id)
                .map(UsuarioResponse::desde)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario solicitado no existe."
                ));
    }

    /**
     * Representación pública de una cuenta.
     * Nunca expone la contraseña almacenada.
     */
    public record UsuarioResponse(
            Long id,
            String nombre,
            String correo,
            String rol) {

        public static UsuarioResponse desde(Usuario usuario) {
            return new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getCorreo(),
                    usuario.getRol() == null ? null : usuario.getRol().name()
            );
        }
    }
}
