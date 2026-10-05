package com.example.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.AuthResponse;
import com.example.dto.response.UserResponse;
import com.example.entities.ERole;
import com.example.entities.User;
import com.example.exception.InvalidCredentialsException;
import com.example.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

/**
 * Registro e inicio de sesion. La credencial de acceso es el email.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    /**
     * Registra un usuario nuevo. Todo alta por esta via nace con rol USER:
     * un ADMIN solo puede concederlo el bootstrap de arranque
     * ({@code app.admin.*}), nunca un cuerpo de peticion, para evitar
     * escaladas de privilegios.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        User user = userService.create(request.email(), request.password(), ERole.USER);

        return UserResponse.from(user);
    }

    /**
     * Autentica al usuario con email + contrasena y devuelve su token JWT.
     *
     * @throws InvalidCredentialsException si el email no existe, esta
     *                                     deshabilitado o la contrasena no
     *                                     coincide
     */
    public AuthResponse login(LoginRequest request) {
        String email = userService.normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            // No se distingue entre "email inexistente" y "contrasena erronea".
            throw new InvalidCredentialsException("Email o contrasena incorrectos");
        }

        User user = userService.getByEmail(email);

        return new AuthResponse(
                tokenProvider.generateToken(user),
                "Bearer",
                tokenProvider.getExpirationMillis(),
                user.getEmail(),
                user.getRole());
    }
}
