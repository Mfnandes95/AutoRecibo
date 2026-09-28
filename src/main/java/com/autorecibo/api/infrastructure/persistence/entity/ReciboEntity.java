package com.autorecibo.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
// IMPORT CORRIGIDO: Agora estamos importando todas as anotações do Lombok que a classe utiliza
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA que representa a tabela tb_recibo no PostgreSQL.
 * Na Arquitetura Hexagonal, esta classe vive na camada de Infraestrutura,
 * servindo apenas como um mapeamento do banco de dados, isolada das regras de negócio.
 */
@Entity
@Table(name = "tb_recibo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReciboEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(name = "id_recibo", updatable = false, nullable = false)
    private UUID id;

    // FetchType.LAZY garante que o usuário não seja carregado do banco até ser explicitamente chamado, economizando memória.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "documento_cliente", length = 14, nullable = false)
    private String documentoCliente;

    @Column(name = "valor_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorTotal;

    // CreationTimestamp delega ao Hibernate a tarefa de preencher a data exata em que o registro é salvo.
    @CreationTimestamp
    @Column(name = "data_emissao", updatable = false)
    private LocalDateTime dataEmissao;

    // CascadeType.ALL permite que, ao salvar o recibo, o Hibernate salve os itens automaticamente.
    // orphanRemoval = true garante que se removermos um item da lista, ele seja deletado do banco.
    @Builder.Default
    @OneToMany(mappedBy = "recibo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemReciboEntity> itens = new ArrayList<>();

    /**
     * Método utilitário para manter a consistência do relacionamento bidirecional.
     * Sempre que for adicionar um item ao recibo, utilize este método.
     * Ele adiciona o item à lista e aponta o recibo dentro do item.
     *
     * @param item A entidade do item a ser adicionado ao recibo.
     */
    public void adicionarItem(ItemReciboEntity item) {
        itens.add(item);
        item.setRecibo(this);
    }
}