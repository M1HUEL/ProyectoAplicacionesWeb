package com.itson.ProyectoAplicacionesWeb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import com.itson.ProyectoAplicacionesWeb.dto.ErrorRespuestaDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Configuración de seguridad de Librería Raudskinna.
 *
 * Controla la autenticación de usuarios, el acceso exclusivo al panel
 * administrativo y el cierre seguro de las sesiones.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {


    /**
     * Seguridad exclusiva de la API REST.
     *
     * Utiliza JWT Bearer sin autenticación
     * mediante sesiones HTTP.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper) throws Exception {

        http
                .securityMatcher("/api/**")

                .authorizeHttpRequests(auth -> auth

                        // Autenticación pública mediante JSON.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/registro")
                        .permitAll()

                        // Consultas públicas del catálogo.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/libros",
                                "/api/libros/*"
                        ).permitAll()

                        // Los demás endpoints requieren JWT.
                        .anyRequest().authenticated()
                )

                // La API no utiliza autenticación mediante cookies.
                .csrf(csrf -> csrf.disable())

                // No mantener sesiones para autenticar la API.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Validación de tokens Bearer JWT.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})

                        // Sin token o con token inválido: 401 en JSON.
                        .authenticationEntryPoint((request, response, ex) ->
                                escribirErrorJson(
                                        objectMapper,
                                        request,
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "Se requiere un token JWT válido "
                                                + "para acceder a este recurso."
                                )
                        )

                        // Autenticado pero sin permisos: 403 en JSON.
                        .accessDeniedHandler((request, response, ex) ->
                                escribirErrorJson(
                                        objectMapper,
                                        request,
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "No tienes permisos para acceder "
                                                + "a este recurso."
                                )
                        )
                );

        return http.build();
    }

    /**
     * Escribe una respuesta de error con el mismo formato JSON que usa la API
     * ({@link ErrorRespuestaDTO}).
     *
     * Se utiliza desde los manejadores de seguridad, que se ejecutan antes de
     * que la petición llegue a los controladores y, por lo tanto, no pasan por
     * {@code ApiExceptionHandler}.
     */
    private void escribirErrorJson(
            ObjectMapper objectMapper,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus estado,
            String mensaje) throws IOException {

        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getWriter(), error);
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // Recursos públicos y formularios de autenticación.
                        .requestMatchers(
                                "/",
                                "/registro",
                                "/iniciar-sesion",
                                "/admin/login",
                                "/styles/**",
                                "/img/**",
                                "/uploads/**",
                                "/css/**",
                                "/js/**"
                        ).permitAll()

                        // Consulta pública del catálogo mediante API REST.
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/libros",
                                "/api/libros/*"
                        ).permitAll()

                        // La gestión de usuarios es exclusiva de administradores.
                        // Estas rutas no forman parte de /admin, por lo que
                        // necesitan una regla explícita.
                        .requestMatchers(
                                "/usuarios",
                                "/usuarios/**"
                        ).hasRole("ADMIN")

                        // Solo los administradores pueden acceder al panel.
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")

                        // Las demás rutas requieren autenticación por ahora.
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/admin/login"),
                                request ->
                                        request.getServletPath().equals("/admin")
                                                || request.getServletPath().startsWith("/admin/")
                        )
                )

                // Spring Security procesa las credenciales del formulario.
                .formLogin(form -> form
                        .loginPage("/iniciar-sesion")
                        .loginProcessingUrl("/iniciar-sesion")
                        .usernameParameter("correo")
                        .passwordParameter("contrasena")
                        .successHandler((request, response, authentication) -> {

                            // Los administradores ingresan al panel de gestión.
                            boolean esAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(authority ->
                                            authority.getAuthority().equals("ROLE_ADMIN"));

                            if (esAdmin) {
                                response.sendRedirect(request.getContextPath() + "/admin");
                            } else {
                                // Los clientes regresan a la página principal.
                                response.sendRedirect(request.getContextPath() + "/");
                            }
                        })
                        .failureUrl("/iniciar-sesion?error")
                        .permitAll()
                )

                // El cierre de sesión invalida la sesión HTTP.
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/iniciar-sesion?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        // CSRF permanece habilitado por defecto.
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el administrador de autenticación de Spring Security.
     *
     * Permite reutilizar el mecanismo existente de autenticación
     * por correo y contraseña desde nuestra API REST.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

}