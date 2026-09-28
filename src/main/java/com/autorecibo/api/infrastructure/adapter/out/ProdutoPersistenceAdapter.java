package com.autorecibo.api.infrastructure.adapter.out;

import com.autorecibo.api.domain.model.Produto;
import com.autorecibo.api.domain.port.out.BuscarProdutoPort;
import com.autorecibo.api.infrastructure.persistence.entity.ProdutoEntity;
import com.autorecibo.api.infrastructure.persistence.repository.ProdutoRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de Saída para Produtos.
 * O @Component diz ao Spring Boot para gerenciar essa classe e injetá-la no nosso Service.
 */
@Component
public class ProdutoPersistenceAdapter implements BuscarProdutoPort {

    private final ProdutoRepository produtoRepository;

    // Construtor para injeção de dependência do repositório
    public ProdutoPersistenceAdapter(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Override
    public Optional<Produto> buscarPorId(UUID idProduto) {
        // 1. Busca a Entidade no banco de dados
        Optional<ProdutoEntity> entityOptional = produtoRepository.findById(idProduto);

        // 2. Traduz a Entidade JPA para o Record puro de Domínio
        return entityOptional.map(entity -> new Produto(
                entity.getId(),
                entity.getUsuario().getId(),
                entity.getDescricao(),
                entity.getValorBruto(),
                true // Definimos como true por padrão, caso o produto exista no banco
        ));
    }
}