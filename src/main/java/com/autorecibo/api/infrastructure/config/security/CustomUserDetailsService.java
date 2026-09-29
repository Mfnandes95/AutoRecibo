package com.autorecibo.api.infrastructure.config.security;

import com.autorecibo.api.domain.model.Usuario;
// AJUSTE o pacote abaixo para onde está o seu UsuarioRepository
import com.autorecibo.api.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega o usuário para o Spring Security. Aceita email, CPF ou CNPJ no login.
 * O username devolvido é SEMPRE o email: é ele que o JwtService grava no token
 * e que o filtro usa para recarregar o usuário nas requisições seguintes.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = (login.contains("@")
                ? usuarioRepository.findByEmail(login.trim().toLowerCase())
                : usuarioRepository.findByDocumento(login.replaceAll("\\D", "")))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .authorities("ROLE_USER")
                .build();
    }
}