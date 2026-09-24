package com.autorecibo.api.domain.port.out;

import com.autorecibo.api.domain.model.Recibo;

/**
 * Porta de saída (Outbound Port) para persistência de Recibos.
 */
public interface SalvarReciboPort {

    /**
     * Salva o recibo no sistema externo (banco de dados).
     * 
     * @param recibo O objeto Recibo de domínio puro já validado e calculado.
     * @return O Recibo salvo (geralmente contendo as datas de criação geradas pelo banco).
     */
    Recibo salvar(Recibo recibo);
}