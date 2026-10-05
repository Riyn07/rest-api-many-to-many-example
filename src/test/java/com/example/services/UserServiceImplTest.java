package com.example.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.entities.ERole;
import com.example.entities.User;
import com.example.exception.ResourceConflictException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("create() normaliza el email, cifra la contrasena y guarda el rol")
    void create() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secreto1!")).thenReturn("hash-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User creado = userService.create("  Ada@Example.COM ", "Secreto1!", ERole.USER);

        assertThat(creado.getEmail()).isEqualTo("ada@example.com");
        assertThat(creado.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(creado.getRole()).isEqualTo(ERole.USER);
        assertThat(creado.isEnabled()).isTrue();
        verify(passwordEncoder).encode("Secreto1!");
    }

    @Test
    @DisplayName("create() guarda exactamente el usuario que devuelve el repositorio")
    void createDelegatesToRepository() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User creado = userService.create("alan@example.com", "Secreto1!", ERole.ADMIN);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(creado);
        assertThat(captor.getValue().getRole()).isEqualTo(ERole.ADMIN);
        assertThat(captor.getValue().getEmail()).isEqualTo("alan@example.com");
    }

    @Test
    @DisplayName("create() lanza ResourceConflictException si el email ya esta registrado")
    void createWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create("Ada@example.com", "Secreto1!", ERole.USER))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("ada@example.com");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("getByEmail() normaliza el email antes de buscar")
    void getByEmail() {
        when(userRepository.findByEmail("ada@example.com"))
                .thenReturn(Optional.of(new User("ada@example.com", "hash", ERole.USER)));

        User encontrado = userService.getByEmail("  Ada@Example.com  ");

        assertThat(encontrado.getEmail()).isEqualTo("ada@example.com");
        verify(userRepository).findByEmail("ada@example.com");
    }

    @Test
    @DisplayName("getByEmail() lanza ResourceNotFoundException si el email no existe")
    void getByEmailWhenNotFound() {
        when(userRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmail("nadie@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("existsByEmail() normaliza el email antes de consultar")
    void existsByEmail() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThat(userService.existsByEmail("ADA@example.com")).isTrue();
        verify(userRepository).existsByEmail("ada@example.com");
    }

    @Test
    @DisplayName("normalizeEmail() recorta espacios y pasa a minusculas")
    void normalizeEmail() {
        assertThat(userService.normalizeEmail("  Ada@Example.COM ")).isEqualTo("ada@example.com");
    }

    @Test
    @DisplayName("normalizeEmail() rechaza null o vacio")
    void normalizeEmailRejectsBlank() {
        assertThatThrownBy(() -> userService.normalizeEmail(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.normalizeEmail("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
