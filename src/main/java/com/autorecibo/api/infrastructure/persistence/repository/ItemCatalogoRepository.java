package com.autorecibo.api.infrastructure.persistence.repository;

import com.autorecibo.api.domain.model.ItemCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ItemCatalogoRepository
        extends JpaRepository<ItemCatalogo, UUID> {

    List<ItemCatalogo> findAllByIdInAndUsuario_Id(
            Set<UUID> ids,
            UUID usuarioId
    );

    Optional<ItemCatalogo> findByNome(String nome);
}