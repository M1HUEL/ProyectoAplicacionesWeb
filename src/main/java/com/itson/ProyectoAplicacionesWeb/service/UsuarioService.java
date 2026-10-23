package com.itson.ProyectoAplicacionesWeb.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.itson.ProyectoAplicacionesWeb.model.Rol;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    public Usuario guardarUsuario(Usuario usuario) {
        if (usuario.getId() == null) {
            if (usuario.getCorreo() != null && usuarioRepository.existsByCorreo(usuario.getCorreo())) {
                throw new RuntimeException("El correo ya está registrado");
            }
            if (usuario.getContrasena() != null && !usuario.getContrasena().isBlank()) {
                usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
            }
            if (usuario.getRol() == null) {
                usuario.setRol(Rol.USER);
            }
            return usuarioRepository.save(usuario);
        } else {
            Usuario existente = usuarioRepository.findById(usuario.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (usuario.getNombre() != null && !usuario.getNombre().isBlank()) {
                existente.setNombre(usuario.getNombre());
            }

            if (usuario.getCorreo() != null && !usuario.getCorreo().isBlank()
                    && !usuario.getCorreo().equals(existente.getCorreo())) {
                if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
                    throw new RuntimeException("El correo ya está registrado");
                }
                existente.setCorreo(usuario.getCorreo());
            }

            if (usuario.getContrasena() != null && !usuario.getContrasena().isBlank()) {
                existente.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
            }

            if (usuario.getRol() != null) {
                existente.setRol(usuario.getRol());
            }

            return usuarioRepository.save(existente);
        }
    }

    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }
}
