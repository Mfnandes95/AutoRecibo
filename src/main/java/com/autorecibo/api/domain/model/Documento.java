package com.autorecibo.api.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Recibo ou ordem de serviço emitido. Depois de emitido NÃO é editado: guarda uma cópia
 * (snapshot) de nomes e preços, então mudanças no catálogo não alteram documentos antigos.
 * Não é documento fiscal (NF-e/NFS-e).
 */
@Entity
@Table(name = "tb_documento",
        uniqueConstraints = @UniqueConstraint(name = "uk_documento_usuario_numero", columnNames = {"id_usuario", "numero"}))
@Getter
@Setter
@NoArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_documento")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /** Numeração sequencial por usuário. Nulo apenas no preview (não persistido). */
    @Column(name = "numero")
    private Long numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoDocumento tipo;

    @Column(name = "cliente_nome", nullable = false, length = 120)
    private String clienteNome;

    @Column(name = "cliente_documento", length = 14)
    private String clienteDocumento;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ItemDocumento> itens = new ArrayList<>();

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_descontos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDescontos;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "emitido_em")
    private LocalDateTime emitidoEm;
}