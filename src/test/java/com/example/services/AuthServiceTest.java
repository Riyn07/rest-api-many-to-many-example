package com.example.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.AuthResponse;
import com.example.dto.response.UserResponse;
import com.example.entities.ERole;
import com.example.entities.User;
import com.example.exception.InvalidCredentialsException;
import com.example.exception.ResourceConflictException;
import com.example.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private static final String RAW_PASSWORD = "Secreto1!";

    private static User user(String email, ERole role) {
        return new User(email, "hash-bcrypt", role);
    }

    @Test
    @DisplayName("register() crea el usuario con rol USER y no filtra la contrasena")
    void register() {
        User creado = user("ada@example.com", ERole.USER);
        creado.setId(7L);
        when(userService.create("ada@example.com", RAW_PASSWORD, ERole.USER)).thenReturn(creado);

        UserResponse respuesta = authService.register(
                new RegisterRequest("ada@example.com", RAW_PASSWORD));

        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.email()).isEqualTo("ada@example.com");
        assertThat(respuesta.role()).isEqualTo(ERole.USER);
        assertThat(respuesta.enabled()).isTrue();
        // El registro nunca concede ADMIN desde el cuerpo de la peticion.
        verify(userService).create("ada@example.com", RAW_PASSWORD, ERole.USER);
    }

    @Test
    @DisplayName("register() propaga el conflicto si el email ya existe")
    void registerWhenEmailExists() {
        when(userService.create(any(), any(), any()))
                .thenThrow(new ResourceConflictException("duplicado"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("ada@example.com", RAW_PASSWORD)))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    @DisplayName("login() autentica con el email normalizado y devuelve el token")
    void login() {
        when(userService.normalizeEmail("  Ada@Example.COM ")).thenReturn("ada@example.com");
        when(authenticationManager.authenticate(any())).thenReturn(mockAuthentication());
        when(userService.getByEmail("ada@example.com")).thenReturn(user("ada@example.com", ERole.USER));
        when(tokenProvider.generateToken(any())).thenReturn("jwt-falso");
        when(tokenProvider.getExpirationMillis()).thenReturn(3_600_000L);

        AuthResponse respuesta = authService.login(
                new LoginRequest("  Ada@Example.COM ", RAW_PASSWORD));

        assertThat(respuesta.token()).isEqualTo("jwt-falso");
        assertThat(respuesta.tokenType()).isEqualTo("Bearer");
        assertThat(respuesta.expiresIn()).isEqualTo(3_600_000L);
        assertThat(respuesta.email()).isEqualTo("ada@example.com");
        assertThat(respuesta.role()).isEqualTo(ERole.USER);

        // La credencial que viaja al AuthenticationManager es el email, no un username.
        var captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("ada@example.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo(RAW_PASSWORD);
    }

    @Test
    @DisplayName("login() con contrasena incorrecta devuelve InvalidCredentialsException")
    void loginWithWrongPassword() {
        when(userService.normalizeEmail("ada@example.com")).thenReturn("ada@example.com");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "mala")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Email o contrasena incorrectos");

        verifyNoInteractions(tokenProvider);
    }

    @Test
    @DisplayName("login() con email desconocido devuelve InvalidCredentialsException")
    void loginWithUnknownEmail() {
        when(userService.normalizeEmail("nadie@example.com")).thenReturn("nadie@example.com");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@example.com", RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(tokenProvider);
    }

    private static Authentication mockAuthentication() {
        return new UsernamePasswordAuthenticationToken("ada@example.com", null, List.of());
    }
}
