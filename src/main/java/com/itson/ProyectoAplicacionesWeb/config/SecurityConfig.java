package com.itson.ProyectoAplicacionesWeb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/registro", "/registro/**", "/iniciar-sesion", "/iniciar-sesion/**",
                        "/styles/**", "/img/**", "/css/**", "/js/**", "/h2-console/**")
                .permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/iniciar-sesion")
                .loginProcessingUrl("/iniciar-sesion")
                .usernameParameter("correo")
                .passwordParameter("contrasena")
                .defaultSuccessUrl("/", true)
                .failureUrl("/iniciar-sesion?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/iniciar-sesion?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
