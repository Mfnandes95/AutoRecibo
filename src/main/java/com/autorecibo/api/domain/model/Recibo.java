package com.autorecibo.api.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public record Recibo(
        UUID id,
        UUID idUsuario,
        String documentoCliente,
        LocalDateTime dataEmissao,
        List<ItemRecibo> itens,
        BigDecimal valorTotal
) {
    
    public Recibo {
        // 1. Validação básica de obrigatoriedade
        if (documentoCliente == null || documentoCliente.isBlank()) {
            throw new IllegalArgumentException("O documento do cliente não pode estar vazio.");
        }
        
        // Protege contra listas nulas (NullPointerException)
        if (itens == null) {
            itens = List.of(); 
        }

        // 2. Regra de Negócio Crítica: O valor total é sempre a soma exata dos itens.
        // O sistema recalcula sozinho para garantir que nenhum código externo passe um valor errado.
        BigDecimal somaCalculada = BigDecimal.ZERO;
        for (ItemRecibo item : itens) {
            somaCalculada = somaCalculada.add(item.valorFinalItem());
        }
        
        // Sobrescrevemos o valorTotal com a soma matemática validada
        valorTotal = somaCalculada;
    }
}