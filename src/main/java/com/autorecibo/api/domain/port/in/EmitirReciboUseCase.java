package com.autorecibo.api.domain.port.in;

import com.autorecibo.api.domain.model.Recibo;

/**
 * Porta de entrada (Inbound Port).
 * Define o contrato do caso de uso de emissão de recibos.
 */
public interface EmitirReciboUseCase {
    
    /**
     * Executa a regra de negócio orquestrada para emitir um recibo.
     * 
     * @param comando Os dados brutos da requisição.
     * @return O recibo final, validado, calculado e salvo.
     */
    Recibo emitir(EmitirReciboCommand comando);
}