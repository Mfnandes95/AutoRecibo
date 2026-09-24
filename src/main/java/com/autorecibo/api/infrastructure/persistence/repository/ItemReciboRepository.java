package com.autorecibo.api.infrastructure.persistence.repository;

import com.autorecibo.api.infrastructure.persistence.entity.ItemReciboEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ItemReciboRepository extends JpaRepository<ItemReciboEntity, UUID> {
    
}