package com.autorecibo.api.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linha enviada pelo cliente. NÃO existe campo de preço: o preço vem sempre do catálogo,
 * no servidor, para que o cliente não consiga manipular valores.
 */
public record ItemDocumentoRequest(
        @NotNull UUID itemCatalogoId,
        @NotNull @DecimalMin("0.001") @DecimalMax("999999") @Digits(integer = 6, fraction = 3) BigDecimal quantidade,
        /** 0, 5, 10, 15 ou 30. Nulo equivale a 0. */
        Integer descontoPercentual
) {}