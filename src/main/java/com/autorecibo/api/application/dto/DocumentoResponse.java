package com.autorecibo.api.application.dto;

import com.autorecibo.api.domain.model.TipoDocumento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Resposta do preview (id, numero e emitidoEm nulos) e da emissão (todos preenchidos). */
public record DocumentoResponse(
        UUID id,
        Long numero,
        TipoDocumento tipo,
        String clienteNome,
        String clienteDocumento,
        String observacoes,
        List<ItemDocumentoResponse> itens,
        BigDecimal subtotal,
        BigDecimal totalDescontos,
        BigDecimal total,
        LocalDateTime emitidoEm
) {}