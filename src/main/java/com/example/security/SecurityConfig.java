package com.example.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion de Spring Security con autenticacion stateless por JWT.
 *
 * Reglas de autorizacion:
 * <ul>
 * <li>{@code /api/auth/**} es publico (registro y login).</li>
 * <li>Las lecturas (GET) de tutoriales y tags requieren autenticacion
 * (USER o ADMIN).</li>
 * <li>Las escrituras (POST, PUT, DELETE) sobre tutoriales y tags requieren
 * exclusivamente el rol ADMIN.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login",
            "/error"
    };

    private static final String[] TUTORIALS_AND_TAGS = {
            "/api/tutorials/**",
            "/api/tags/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Lecturas: cualquier usuario autenticado
                        .requestMatchers(HttpMethod.GET, TUTORIALS_AND_TAGS).hasAnyRole("USER", "ADMIN")
                        // Escrituras: solo ADMIN
                        .requestMatchers(HttpMethod.POST, TUTORIALS_AND_TAGS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, TUTORIALS_AND_TAGS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, TUTORIALS_AND_TAGS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, TUTORIALS_AND_TAGS).hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager inyectado en el servicio de autenticacion para
     * validar email + contrasena en el endpoint de login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
