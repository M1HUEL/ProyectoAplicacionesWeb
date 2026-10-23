package com.itson.ProyectoAplicacionesWeb.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

@Service
public class AutenticacionService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticacionService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        String rol = usuario.getRol() != null ? usuario.getRol().name() : "USER";

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getContrasena())
                .roles(rol)
                .build();
    }

    public void registrar(Usuario usuario) {
        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
            throw new RuntimeException("El correo ya está registrado");
        }

        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        if (usuario.getRol() == null) {
            usuario.setRol(com.itson.ProyectoAplicacionesWeb.model.Rol.USER);
        }

        usuarioRepository.save(usuario);
    }
}
