package com.autorecibo.api.infrastructure.persistence.repository;

import com.autorecibo.api.infrastructure.persistence.entity.ReciboEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReciboRepository extends JpaRepository<ReciboEntity, UUID> {
    
    List<ReciboEntity> findAllByUsuarioId(UUID idUsuario);

    @Query("SELECT r FROM ReciboEntity r JOIN FETCH r.itens WHERE r.id = :id")
    Optional<ReciboEntity> findByIdWithItens(@Param("id") UUID id);
}