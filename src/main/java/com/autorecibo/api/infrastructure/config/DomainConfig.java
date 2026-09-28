package com.autorecibo.api.infrastructure.config;

import com.autorecibo.api.application.service.EmitirReciboService;
import com.autorecibo.api.domain.port.out.BuscarProdutoPort;
import com.autorecibo.api.domain.port.out.SalvarReciboPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Classe de configuração para injetar os serviços do domínio.
 */
@Configuration
public class DomainConfig {

    @Bean
    public EmitirReciboService emitirReciboService(BuscarProdutoPort buscarProdutoPort, SalvarReciboPort salvarReciboPort) {
        return new EmitirReciboService(buscarProdutoPort, salvarReciboPort);
    }
}