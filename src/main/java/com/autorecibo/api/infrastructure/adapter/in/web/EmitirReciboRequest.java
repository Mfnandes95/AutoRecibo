package com.autorecibo.api.infrastructure.adapter.in.web;

import java.util.List;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) para receber os dados JSON da requisição HTTP.
 * Serve para isolar a nossa API Web do nosso Core Domain.
 */
public record EmitirReciboRequest(
        UUID idUsuario,
        String documentoCliente,
        List<ItemRequest> itens
) {
    /**
     * Sub-record que representa os itens dentro do JSON.
     */
    public record ItemRequest(
            UUID idProduto,
            Integer percentualDesconto
    ) {}
}