package com.autorecibo.api.domain.port.out;

import com.autorecibo.api.domain.model.Produto;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída (Outbound Port) para comunicação com dados de Produto.
 * O Domínio usa este contrato para pedir informações do mundo externo.
 */
public interface BuscarProdutoPort {
    
    /**
     * Busca um produto pelo seu ID.
     * Utiliza Optional pois o produto pode não existir no banco.
     * 
     * @param idProduto O ID UUID do produto.
     * @return Um Optional contendo o Produto de domínio, se encontrado.
     */
    Optional<Produto> buscarPorId(UUID idProduto);
}