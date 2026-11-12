package com.itson.ProyectoAplicacionesWeb.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.itson.ProyectoAplicacionesWeb.model.Rol;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

/**
 * Servicio encargado de la autenticación y el registro de usuarios.
 *
 * Spring Security utiliza este servicio para recuperar las credenciales
 * y autoridades de los usuarios almacenados en MySQL.
 *
 * También aplica las reglas básicas de seguridad durante el registro público.
 */
@Service
public class AutenticacionService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CORREO_ADMIN = "admin@raudskinna.com";

    public AutenticacionService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Recupera el usuario mediante su correo electrónico.
     *
     * Spring Security compara posteriormente la contraseña proporcionada
     * con el hash almacenado y utiliza las autoridades para controlar el acceso.
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado"));

        String rol = usuario.getRol() != null
                ? usuario.getRol().name()
                : "USER";

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getContrasena())
                .roles(rol)
                .build();
    }

    /**
     * Registra una cuenta de cliente.
     *
     * Por seguridad, los registros públicos siempre reciben el rol USER.
     * Los privilegios administrativos no pueden asignarse desde este método.
     */
    public void registrar(Usuario usuario) {

        // Reservamos el correo utilizado para el administrador inicial.
        if (CORREO_ADMIN.equalsIgnoreCase(usuario.getCorreo())) {
            throw new IllegalArgumentException(
                    "Este correo electrónico no está disponible");
        }

        // Evitamos registrar dos cuentas con el mismo correo.
        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
            throw new IllegalArgumentException(
                    "El correo ya está registrado");
        }

        // El identificador debe generarlo la base de datos.
        // No aceptamos identificadores enviados desde el formulario.
        usuario.setId(null);

        // Todo registro público recibe obligatoriamente el rol USER.
        // Nunca aceptamos privilegios administrativos enviados por el cliente.
        usuario.setRol(Rol.USER);

        // Generamos el hash BCrypt antes de almacenar la contraseña.
        usuario.setContrasena(
                passwordEncoder.encode(usuario.getContrasena()));

        // Persistimos el nuevo usuario mediante Spring Data JPA.
        usuarioRepository.save(usuario);
    }
}