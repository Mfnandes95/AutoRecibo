package com.autorecibo.api.infrastructure.persistence.repository;

import com.autorecibo.api.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Usado pelo UsuarioDetailsService durante a autenticação (login) e
     * pelo AuthController para checar duplicidade no registro.
     */
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByDocumento(String documento);

    boolean existsByEmail(String email);
    boolean existsByDocumento(String documento);

}