package com.example.services;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entities.ERole;
import com.example.entities.User;
import com.example.exception.ResourceConflictException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User create(String email, String rawPassword, ERole role) {
        String normalized = normalizeEmail(email);

        if (userRepository.existsByEmail(normalized)) {
            throw new ResourceConflictException("Ya existe un usuario registrado con el email " + normalized);
        }

        return userRepository.save(new User(normalized, passwordEncoder.encode(rawPassword), role));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un usuario registrado con el email " + normalizeEmail(email)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    @Override
    public String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email no puede ser nulo ni vacio");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
