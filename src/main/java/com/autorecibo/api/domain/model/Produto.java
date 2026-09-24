package com.autorecibo.api.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Modelo de domínio puro para Produto.
 * Representa as regras matemáticas e de negócio, sem depender do banco de dados.
 */
public record Produto(
        UUID id,
        UUID idUsuario,
        String descricao,
        BigDecimal valorBruto,
        boolean ativo
) {
    /**
     * Construtor compacto do Java Record.
     * Ideal para aplicar validações de negócio autoencapsuladas.
     * Um Produto nunca poderá ser instanciado na memória se estiver em um estado inválido.
     */
    public Produto {
        // Valida se o texto da descrição está nulo ou em branco
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição do produto não pode estar vazia.");
        }
        
        // Valida se o valor financeiro do produto é válido (maior que zero)
        if (valorBruto == null || valorBruto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor bruto do produto deve ser maior que zero.");
        }
    }
}