package com.autorecibo.api.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tb_item_documento")
@Getter
@Setter
@NoArgsConstructor
public class ItemDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_item_documento")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;

    /** Referência informativa ao item do catálogo de origem (sem chave estrangeira de propósito). */
    @Column(name = "id_item_catalogo")
    private UUID itemCatalogoId;

    @Column(name = "ordem", nullable = false)
    private int ordem;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "preco_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "quantidade", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "desconto_percentual", nullable = false)
    private int descontoPercentual;

    @Column(name = "valor_bruto", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "valor_desconto", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "valor_liquido", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorLiquido;
}