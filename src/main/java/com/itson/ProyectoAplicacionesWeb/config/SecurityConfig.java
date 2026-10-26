package com.itson.ProyectoAplicacionesWeb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Configuración de seguridad de Librería Raudskinna.
 *
 * Controla la autenticación de usuarios, el acceso exclusivo al panel
 * administrativo y el cierre seguro de las sesiones.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
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
                                "/css/**",
                                "/js/**"
                        ).permitAll()

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
}