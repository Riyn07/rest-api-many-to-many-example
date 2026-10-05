package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.entities.ERole;
import com.example.entities.User;

import jakarta.persistence.EntityManager;

@DataJpaTest
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User persist(String email, ERole role) {
        return userRepository.saveAndFlush(new User(email, "contrasenaCifrada", role));
    }

    @Test
    @DisplayName("save() persiste un usuario con su email y rol")
    void save() {
        User saved = persist("ada@example.com", ERole.ADMIN);

        assertThat(saved.getId()).isPositive();

        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEmail()).isEqualTo("ada@example.com");
        assertThat(found.getRole()).isEqualTo(ERole.ADMIN);
        assertThat(found.isEnabled()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmail() localiza al usuario por email")
    void findByEmail() {
        persist("grace@example.com", ERole.USER);

        assertThat(userRepository.findByEmail("grace@example.com"))
                .hasValueSatisfying(user -> assertThat(user.getEmail()).isEqualTo("grace@example.com"));
    }

    @Test
    @DisplayName("findByEmail() devuelve vacio si el email no existe")
    void findByEmailWhenNotFound() {
        assertThat(userRepository.findByEmail("nadie@example.com")).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail() indica si el email ya esta registrado")
    void existsByEmail() {
        persist("alan@example.com", ERole.USER);

        assertThat(userRepository.existsByEmail("alan@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("alan@otro.com")).isFalse();
    }

    @Test
    @DisplayName("el email es unico: dos usuarios con el mismo email violan la restriccion")
    void emailIsUnique() {
        persist("duplicado@example.com", ERole.USER);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            persist("duplicado@example.com", ERole.USER);
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("deleteById() elimina el usuario")
    void deleteById() {
        User saved = persist("borrable@example.com", ERole.USER);
        long id = saved.getId();

        userRepository.deleteById(id);
        userRepository.flush();

        assertThat(userRepository.findById(id)).isEmpty();
    }
}