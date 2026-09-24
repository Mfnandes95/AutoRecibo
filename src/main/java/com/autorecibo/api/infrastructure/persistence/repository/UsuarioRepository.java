package com.autorecibo.api.infrastructure.persistence.repository;

import com.autorecibo.api.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {
    Optional<UsuarioEntity> findByDocumento(String documento);
    Optional<UsuarioEntity> findByEmail(String email);
}
