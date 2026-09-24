package com.autorecibo.api.infrastructure.persistence.repository;

import com.autorecibo.api.infrastructure.persistence.entity.ProdutoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProdutoRepository extends JpaRepository<ProdutoEntity, UUID> {
    
    List<ProdutoEntity> findAllByUsuarioId(UUID idUsuario);
}