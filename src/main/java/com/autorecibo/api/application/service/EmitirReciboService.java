package com.autorecibo.api.application.service;

import com.autorecibo.api.domain.model.ItemRecibo;
import com.autorecibo.api.domain.model.Produto;
import com.autorecibo.api.domain.model.Recibo;
import com.autorecibo.api.domain.port.in.EmitirReciboCommand;
import com.autorecibo.api.domain.port.in.EmitirReciboUseCase;
import com.autorecibo.api.domain.port.out.BuscarProdutoPort;
import com.autorecibo.api.domain.port.out.SalvarReciboPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do caso de uso de emissão de recibos.
 * O maestro da Arquitetura Hexagonal: orquestra as portas e os modelos puros.
 */
public class EmitirReciboService implements EmitirReciboUseCase {

    private final BuscarProdutoPort buscarProdutoPort;
    private final SalvarReciboPort salvarReciboPort;

    // Construtor: O Spring injetará as portas de saída automaticamente aqui
    public EmitirReciboService(BuscarProdutoPort buscarProdutoPort, SalvarReciboPort salvarReciboPort) {
        this.buscarProdutoPort = buscarProdutoPort;
        this.salvarReciboPort = salvarReciboPort;
    }

    @Override
    public Recibo emitir(EmitirReciboCommand comando) {
        
        List<ItemRecibo> itensCalculados = new ArrayList<>();

        // 1. Para cada item solicitado, buscamos o produto e montamos o item do recibo
        for (EmitirReciboCommand.ItemComando itemCmd : comando.itens()) {
            
            // Busca o produto no banco de dados através da Porta
            Produto produto = buscarProdutoPort.buscarPorId(itemCmd.idProduto())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com ID: " + itemCmd.idProduto()));

            // Cria o modelo puro ItemRecibo (a matemática de desconto ocorre automaticamente aqui)
            ItemRecibo itemRecibo = new ItemRecibo(
                    null, // O ID é gerado pelo banco de dados depois
                    produto,
                    itemCmd.percentualDesconto(),
                    null // O valor final é calculado pelo próprio ItemRecibo
            );
            
            itensCalculados.add(itemRecibo);
        }

        // 2. Cria o Recibo (a matemática da soma total ocorre automaticamente aqui)
        Recibo reciboParaSalvar = new Recibo(
                null, 
                comando.idUsuario(),
                comando.documentoCliente(),
                LocalDateTime.now(),
                itensCalculados,
                null // O Recibo recalcula a soma total automaticamente
        );

        // 3. Salva no banco de dados usando a Porta
        return salvarReciboPort.salvar(reciboParaSalvar);
    }
}