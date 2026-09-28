package com.autorecibo.api.infrastructure.adapter.in.web;

import com.autorecibo.api.domain.model.Recibo;
import com.autorecibo.api.domain.port.in.EmitirReciboCommand;
import com.autorecibo.api.domain.port.in.EmitirReciboUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST que expõe os casos de uso de Recibo para o mundo externo.
 */
@RestController
@RequestMapping("/api/v1/recibos")
public class ReciboController {

    // Dependemos apenas da Interface (Porta de Entrada), mantendo o desacoplamento.
    private final EmitirReciboUseCase emitirReciboUseCase;

    // O Spring injeta automaticamente o EmitirReciboService aqui.
    public ReciboController(EmitirReciboUseCase emitirReciboUseCase) {
        this.emitirReciboUseCase = emitirReciboUseCase;
    }

    /**
     * Endpoint para emissão de um novo recibo.
     * Método POST é usado pois estamos criando um novo recurso no sistema.
     * 
     * @param request O corpo da requisição JSON.
     * @return O Recibo final salvo e o status HTTP 201 (Created).
     */
    @PostMapping
    public ResponseEntity<Recibo> emitirRecibo(@RequestBody EmitirReciboRequest request) {
        
        // 1. Tradução do DTO da Web para o Comando do Domínio
        List<EmitirReciboCommand.ItemComando> itensComando = request.itens().stream()
                .map(item -> new EmitirReciboCommand.ItemComando(item.idProduto(), item.percentualDesconto()))
                .collect(Collectors.toList());

        EmitirReciboCommand comando = new EmitirReciboCommand(
                request.idUsuario(),
                request.documentoCliente(),
                itensComando
        );

        // 2. Chama a regra de negócio central
        Recibo reciboEmitido = emitirReciboUseCase.emitir(comando);

        // 3. Devolve a resposta com sucesso (Status 201)
        return ResponseEntity.status(HttpStatus.CREATED).body(reciboEmitido);
    }
}