-- Habilita a geração nativa de UUIDs no PostgreSQL (caso não esteja habilitada)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Tabela de Usuários (Emissores)
CREATE TABLE tb_usuario (
    id_usuario UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    documento VARCHAR(14) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Produtos/Serviços importados
CREATE TABLE tb_produto (
    id_produto UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_usuario UUID NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    valor_bruto NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_produto_usuario FOREIGN KEY (id_usuario) 
        REFERENCES tb_usuario(id_usuario) ON DELETE CASCADE
);

-- 3. Tabela de Recibos/Ordens de Serviço emitidas
CREATE TABLE tb_recibo (
    id_recibo UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_usuario UUID NOT NULL,
    documento_cliente VARCHAR(14) NOT NULL,
    valor_total NUMERIC(10,2) NOT NULL,
    data_emissao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recibo_usuario FOREIGN KEY (id_usuario) 
        REFERENCES tb_usuario(id_usuario) ON DELETE CASCADE
);

-- 4. Tabela de Itens do Recibo (Relação de N:N com descontos aplicados)
CREATE TABLE tb_item_recibo (
    id_item_recibo UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_recibo UUID NOT NULL,
    id_produto UUID NOT NULL,
    valor_unitario NUMERIC(10,2) NOT NULL,
    percentual_desconto INTEGER NOT NULL,
    valor_final_item NUMERIC(10,2) NOT NULL,
    
    CONSTRAINT fk_item_recibo FOREIGN KEY (id_recibo) 
        REFERENCES tb_recibo(id_recibo) ON DELETE CASCADE,
        
    CONSTRAINT fk_item_produto FOREIGN KEY (id_produto) 
        REFERENCES tb_produto(id_produto),

    -- Trava de segurança no banco de dados para aceitar apenas os descontos permitidos no escopo (e 0 para sem desconto)
    CONSTRAINT chk_percentual_desconto CHECK (percentual_desconto IN (0, 5, 10, 15, 30))
);

-- 5. Criação de Índices para melhorar a performance de consultas futuras
CREATE INDEX idx_produto_usuario ON tb_produto(id_usuario);
CREATE INDEX idx_recibo_usuario ON tb_recibo(id_usuario);