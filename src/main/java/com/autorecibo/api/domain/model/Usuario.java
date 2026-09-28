package com.autorecibo.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa um usuário (microempreendedor) do sistema.
 *
 * NOTA: esta classe foi reconstruída a partir das colunas observadas no log
 * do Hibernate (tb_usuario). Se você já tem essa entidade implementada no
 * projeto com nomes diferentes, ajuste os campos abaixo em vez de duplicar.
 */
@Entity
@Table(name = "tb_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_usuario")
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "documento", nullable = false, unique = true, length = 14)
    private String documento;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;
}