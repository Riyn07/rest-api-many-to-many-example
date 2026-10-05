package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.entities.ERole;
import com.example.services.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Crea el usuario ADMIN inicial en el arranque, ya que el alta publica
 * (/api/auth/register) solo concede el rol USER por seguridad.
 *
 * Es idempotente: si el email ya existe no hace nada, de modo que reiniciar
 * la aplicacion no duplica el administrador ni pisa su contrasena.
 *
 * Se desactiva con {@code app.admin.enabled=false}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserService userService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        String email = userService.normalizeEmail(adminEmail);

        if (userService.existsByEmail(email)) {
            logger.info("El usuario ADMIN {} ya existe: no se vuelve a crear", email);
            return;
        }

        userService.create(email, adminPassword, ERole.ADMIN);
        logger.info("Usuario ADMIN inicial creado con el email {}", email);
    }
}