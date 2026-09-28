package com.autorecibo.api.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Modelo de domínio puro para os itens do recibo.
 */
public record ItemRecibo(
        UUID id,
        Produto produto,
        Integer percentualDesconto,
        BigDecimal valorFinalItem
) {
    /**
     * Construtor compacto: executado automaticamente sempre que um ItemRecibo é criado.
     * Serve para proteger e validar os dados antes de existirem na memória.
     */
    public ItemRecibo {
        // 1. Valida se o desconto é permitido pela regra de negócio
        List<Integer> descontosPermitidos = List.of(0, 5, 10, 15, 30);
        if (!descontosPermitidos.contains(percentualDesconto)) {
            throw new IllegalArgumentException("O percentual de desconto deve ser 0, 5, 10, 15 ou 30.");
        }

        // 2. Protege o cálculo: se não foi passado um valor final explicitamente, nós calculamos
        if (valorFinalItem == null && produto != null) {
            if (percentualDesconto == 0) {
                valorFinalItem = produto.valorBruto();
            } else {
                // Matemática: Valor Bruto * (Desconto / 100)
                BigDecimal taxaDesconto = new BigDecimal(percentualDesconto).divide(new BigDecimal("100"));
                BigDecimal valorDescontado = produto.valorBruto().multiply(taxaDesconto);
                
                // Subtrai o desconto do valor bruto e arredonda corretamente (2 casas decimais)
                valorFinalItem = produto.valorBruto().subtract(valorDescontado).setScale(2, RoundingMode.HALF_UP);
            }
        }
    }
}