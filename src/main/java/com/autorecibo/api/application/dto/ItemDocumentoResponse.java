package com.autorecibo.api.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDocumentoResponse(
        UUID itemCatalogoId,
        String nome,
        BigDecimal precoUnitario,
        BigDecimal quantidade,
        int descontoPercentual,
        BigDecimal valorBruto,
        BigDecimal valorDesconto,
        BigDecimal valorLiquido
) {}