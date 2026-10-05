package com.example.security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entities.User;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Carga los datos de autenticacion usando el <b>email</b> como nombre de
 * usuario. Es el punto de extension que Spring Security invoca cuando el
 * login es correcto y hay que construir el principal.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario registrado con el email " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(toAuthorities(user))
                .build();
    }

    private List<GrantedAuthority> toAuthorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
}