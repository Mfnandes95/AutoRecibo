package com.autorecibo.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tb_produto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProdutoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(name = "id_produto", updatable = false, nullable = false)
    private UUID id;

    // Relacionamento Muitos-Para-Um: Vários produtos pertencem a um usuário
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "valor_bruto", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorBruto;
}