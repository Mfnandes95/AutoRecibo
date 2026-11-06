-- ============================================================
-- AutoRecibo — Script Completo de Inicialização no Supabase SQL Editor
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. TABELA DE USUÁRIOS
CREATE TABLE IF NOT EXISTS tb_usuario (
    id_usuario UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    documento VARCHAR(14) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. CATÁLOGO DE ITENS (PRODUTOS E SERVIÇOS)
CREATE TABLE IF NOT EXISTS tb_item_catalogo (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_usuario UUID NOT NULL,
    nome VARCHAR(200) NOT NULL,
    descricao VARCHAR(500),
    preco NUMERIC(15,2) NOT NULL CHECK (preco >= 0),
    CONSTRAINT fk_catalogo_usuario FOREIGN KEY (id_usuario)
        REFERENCES tb_usuario(id_usuario) ON DELETE CASCADE
);

-- 3. DOCUMENTOS EMITIDOS (NOTAS FISCAIS, ORDENS DE SERVIÇO, RECIBOS)
CREATE TABLE IF NOT EXISTS tb_documento (
    id_documento UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_usuario UUID NOT NULL,
    numero BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('NOTA_FISCAL', 'ORDEM_SERVICO', 'RECIBO')),
    cliente_nome VARCHAR(120) NOT NULL,
    cliente_documento VARCHAR(14),
    observacoes VARCHAR(500),
    subtotal NUMERIC(15,2) NOT NULL CHECK (subtotal >= 0),
    total_descontos NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (total_descontos >= 0),
    total NUMERIC(15,2) NOT NULL CHECK (total >= 0),
    emitido_em TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_documento_usuario FOREIGN KEY (id_usuario)
        REFERENCES tb_usuario(id_usuario) ON DELETE CASCADE,
    CONSTRAINT uk_documento_usuario_numero UNIQUE (id_usuario, numero)
);

-- 4. ITENS DO DOCUMENTO (MÚLTIPLOS PRODUTOS OU SERVIÇOS POR NOTA)
CREATE TABLE IF NOT EXISTS tb_item_documento (
    id_item_documento UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_documento UUID NOT NULL,
    id_item_catalogo UUID,
    ordem INTEGER NOT NULL DEFAULT 0,
    nome VARCHAR(200) NOT NULL,
    preco_unitario NUMERIC(15,2) NOT NULL CHECK (preco_unitario >= 0),
    quantidade NUMERIC(12,3) NOT NULL CHECK (quantidade > 0),
    desconto_percentual INTEGER NOT NULL CHECK (desconto_percentual IN (0, 5, 10, 15, 30)),
    valor_bruto NUMERIC(15,2) NOT NULL CHECK (valor_bruto >= 0),
    valor_desconto NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (valor_desconto >= 0),
    valor_liquido NUMERIC(15,2) NOT NULL CHECK (valor_liquido >= 0),
    CONSTRAINT fk_item_documento FOREIGN KEY (id_documento)
        REFERENCES tb_documento(id_documento) ON DELETE CASCADE,
    CONSTRAINT fk_item_origem_catalogo FOREIGN KEY (id_item_catalogo)
        REFERENCES tb_item_catalogo(id) ON DELETE SET NULL
);

-- 5. ÍNDICES DE PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_usuario_email ON tb_usuario(email);
CREATE INDEX IF NOT EXISTS idx_usuario_documento ON tb_usuario(documento);
CREATE INDEX IF NOT EXISTS idx_catalogo_usuario ON tb_item_catalogo(id_usuario);
CREATE INDEX IF NOT EXISTS idx_documento_usuario ON tb_documento(id_usuario);
CREATE INDEX IF NOT EXISTS idx_documento_usuario_tipo ON tb_documento(id_usuario, tipo);
CREATE INDEX IF NOT EXISTS idx_item_documento ON tb_item_documento(id_documento);

-- 6. SEGURANÇA E POLÍTICAS RLS (ROW LEVEL SECURITY)
ALTER TABLE tb_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_item_catalogo ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_documento ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_item_documento ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Permitir acesso completo para conexoes de servico" ON tb_usuario FOR ALL USING (true);
CREATE POLICY "Permitir acesso completo para conexoes de servico" ON tb_item_catalogo FOR ALL USING (true);
CREATE POLICY "Permitir acesso completo para conexoes de servico" ON tb_documento FOR ALL USING (true);
CREATE POLICY "Permitir acesso completo para conexoes de servico" ON tb_item_documento FOR ALL USING (true);
