package com.autorecibo.api.application.dto;

import com.autorecibo.api.domain.model.TipoDocumento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentoResumoResponse(
        UUID id,
        Long numero,
        TipoDocumento tipo,
        String clienteNome,
        String clienteDocumento,
        BigDecimal total,
        LocalDateTime emitidoEm
) {}