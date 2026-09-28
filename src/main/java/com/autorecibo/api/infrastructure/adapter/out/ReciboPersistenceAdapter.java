package com.autorecibo.api.infrastructure.adapter.out;

import com.autorecibo.api.domain.model.Recibo;
import com.autorecibo.api.domain.port.out.SalvarReciboPort;
import com.autorecibo.api.infrastructure.persistence.entity.ItemReciboEntity;
import com.autorecibo.api.infrastructure.persistence.entity.ProdutoEntity;
import com.autorecibo.api.infrastructure.persistence.entity.ReciboEntity;
import com.autorecibo.api.infrastructure.persistence.entity.UsuarioEntity;
import com.autorecibo.api.infrastructure.persistence.repository.ReciboRepository;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Saída para salvar Recibos no PostgreSQL.
 */
@Component
public class ReciboPersistenceAdapter implements SalvarReciboPort {

    private final ReciboRepository reciboRepository;

    public ReciboPersistenceAdapter(ReciboRepository reciboRepository) {
        this.reciboRepository = reciboRepository;
    }

    @Override
    public Recibo salvar(Recibo recibo) {
        
        // 1. TRADUÇÃO: De Domínio para Entidade (JPA)
        // Usamos o padrão Builder que você configurou com o Lombok nas entidades
        ReciboEntity reciboEntity = ReciboEntity.builder()
                .documentoCliente(recibo.documentoCliente())
                .valorTotal(recibo.valorTotal())
                // O Hibernate só precisa do ID do usuário para fazer o relacionamento (Foreign Key)
                .usuario(UsuarioEntity.builder().id(recibo.idUsuario()).build())
                .build();

        // Adiciona os itens ao recibo entity
        recibo.itens().forEach(itemDominio -> {
            ItemReciboEntity itemEntity = ItemReciboEntity.builder()
                    .produto(ProdutoEntity.builder().id(itemDominio.produto().id()).build())
                    .valorUnitario(itemDominio.produto().valorBruto())
                    .percentualDesconto(itemDominio.percentualDesconto())
                    .valorFinalItem(itemDominio.valorFinalItem())
                    .build();
            
            // Usa o método utilitário que criamos lá na Fase 1 para garantir a sincronia
            reciboEntity.adicionarItem(itemEntity);
        });

        // 2. PERSISTÊNCIA: O Spring Data JPA faz o comando INSERT no PostgreSQL aqui
        ReciboEntity entidadeSalva = reciboRepository.save(reciboEntity);

        // 3. TRADUÇÃO REVERSA: De Entidade (agora com ID e Data gerados pelo banco) para Domínio
        return new Recibo(
                entidadeSalva.getId(),
                entidadeSalva.getUsuario().getId(),
                entidadeSalva.getDocumentoCliente(),
                entidadeSalva.getDataEmissao(), // Data gerada pelo @CreationTimestamp
                recibo.itens(), // Mantemos os itens originais do domínio que já têm o desconto
                entidadeSalva.getValorTotal()
        );
    }
}