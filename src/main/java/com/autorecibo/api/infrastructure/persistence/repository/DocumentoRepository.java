package com.autorecibo.api.infrastructure.persistence.repository;

import com.autorecibo.api.domain.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    /** Sempre filtra pelo dono: um usuário nunca enxerga documento de outro. */
    Optional<Documento> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    List<Documento> findTop50ByUsuarioIdOrderByEmitidoEmDesc(UUID usuarioId);

    @Query("select coalesce(max(d.numero), 0) from Documento d where d.usuario.id = :usuarioId")
    long maiorNumero(@Param("usuarioId") UUID usuarioId);
}