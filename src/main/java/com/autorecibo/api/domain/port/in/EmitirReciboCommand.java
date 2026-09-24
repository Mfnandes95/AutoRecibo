package com.autorecibo.api.domain.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Representa a intenção do usuário de emitir um recibo.
 * Carrega apenas os dados brutos necessários para a operação.
 */
public record EmitirReciboCommand(
        UUID idUsuario,
        String documentoCliente,
        List<ItemComando> itens
) {
    /**
     * Sub-record para representar os itens solicitados no comando.
     */
    public record ItemComando(
            UUID idProduto, 
            Integer percentualDesconto
    ) {}
}