package com.autorecibo.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidade JPA que representa a tabela tb_item_recibo no banco de dados.
 * Faz a ligação entre o Recibo e o Produto.
 */
@Entity
@Table(name = "tb_item_recibo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemReciboEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(name = "id_item_recibo", updatable = false, nullable = false)
    private UUID id;

    // Muitos itens pertencem a um Recibo
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_recibo", nullable = false)
    private ReciboEntity recibo;

    // Muitos itens referenciam um Produto
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_produto", nullable = false)
    private ProdutoEntity produto;

    @Column(name = "valor_unitario", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorUnitario;

    @Column(name = "percentual_desconto", nullable = false)
    private Integer percentualDesconto;

    @Column(name = "valor_final_item", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorFinalItem;
}