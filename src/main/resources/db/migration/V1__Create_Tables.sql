-- ============================================================
-- AutoRecibo — Esquema unificado
-- ============================================================

-- 1. Tabela de Usuários (Emissores / MEIs)
CREATE TABLE tb_usuario (
    id_usuario UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    documento VARCHAR(14) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Catálogo de Itens (produtos e serviços do usuário)
CREATE TABLE tb_item_catalogo (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_usuario UUID NOT NULL,
    nome VARCHAR(200) NOT NULL,
    descricao VARCHAR(500),
    preco NUMERIC(15,2) NOT NULL,
    CONSTRAINT fk_catalogo_usuario FOREIGN KEY (id_usuario)
        REFERENCES tb_usuario(id_usuario) ON DELETE CASCADE
);

-- 3. Documentos emitidos (recibos e ordens de serviço)
CREATE TABLE tb_documento (
    id_documento UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_usuario UUID NOT NULL,
    numero BIGINT,
    tipo VARCHAR(20) NOT NULL,
    cliente_nome VARCHAR(120) NOT NULL,
    cliente_documento VARCHAR(14),
    observacoes VARCHAR(500),
    subtotal NUMERIC(15,2) NOT NULL,
    total_descontos NUMERIC(15,2) NOT NULL,
    total NUMERIC(15,2) NOT NULL,
    emitido_em TIMESTAMP,
    CONSTRAINT fk_documento_usuario FOREIGN KEY (id_usuario)
        REFERENCES tb_usuario(id_usuario) ON DELETE CASCADE,
    CONSTRAINT uk_documento_usuario_numero UNIQUE (id_usuario, numero)
);

-- 4. Itens do documento (snapshot do catálogo no momento da emissão)
CREATE TABLE tb_item_documento (
    id_item_documento UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_documento UUID NOT NULL,
    id_item_catalogo UUID,
    ordem INTEGER NOT NULL,
    nome VARCHAR(200) NOT NULL,
    preco_unitario NUMERIC(15,2) NOT NULL,
    quantidade NUMERIC(12,3) NOT NULL,
    desconto_percentual INTEGER NOT NULL,
    valor_bruto NUMERIC(15,2) NOT NULL,
    valor_desconto NUMERIC(15,2) NOT NULL,
    valor_liquido NUMERIC(15,2) NOT NULL,
    CONSTRAINT fk_item_documento FOREIGN KEY (id_documento)
        REFERENCES tb_documento(id_documento) ON DELETE CASCADE,
    CONSTRAINT chk_desconto_percentual CHECK (desconto_percentual IN (0, 5, 10, 15, 30))
);

-- 5. Índices para performance
CREATE INDEX idx_catalogo_usuario ON tb_item_catalogo(id_usuario);
CREATE INDEX idx_documento_usuario ON tb_documento(id_usuario);
CREATE INDEX idx_item_documento ON tb_item_documento(id_documento);
