-- ============================================================
-- AutoRecibo — Esquema Oficial do Banco de Dados (Supabase / PostgreSQL)
-- Baseado no escopo unificado: Usuários, Catálogo, Documentos (Notas Fiscais, OS, Recibos) e Itens
-- ============================================================

-- Habilitar extensão para geração de UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. TABELA DE USUÁRIOS (Emissores / Microempreendedores)
CREATE TABLE IF NOT EXISTS tb_usuario (
    id_usuario UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    documento VARCHAR(14) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE tb_usuario IS 'Usuários emissores (MEIs/Autônomos) cadastrados no AutoRecibo.';
COMMENT ON COLUMN tb_usuario.documento IS 'CPF (11 dígitos) ou CNPJ (14 dígitos) apenas números.';

-- 2. TABELA DE CATÁLOGO (Produtos e Serviços cadastrados ou importados via planilha)
CREATE TABLE IF NOT EXISTS tb_item_catalogo (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_usuario UUID NOT NULL,
    nome VARCHAR(200) NOT NULL,
    descricao VARCHAR(500),
    preco NUMERIC(15,2) NOT NULL CHECK (preco >= 0),
    CONSTRAINT fk_catalogo_usuario FOREIGN KEY (id_usuario)
        REFERENCES tb_usuario(id_usuario) ON DELETE CASCADE
);

COMMENT ON TABLE tb_item_catalogo IS 'Catálogo de produtos e serviços pertencentes a cada usuário.';
COMMENT ON COLUMN tb_item_catalogo.preco IS 'Preço unitário de tabela protegido contra manipulação no cliente.';

-- 3. TABELA DE DOCUMENTOS (Notas Fiscais, Ordens de Serviço e Recibos)
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

COMMENT ON TABLE tb_documento IS 'Documentos comerciais emitidos (snapshot imutável pós-emissão).';
COMMENT ON COLUMN tb_documento.numero IS 'Número sequencial estrito e contínuo por emissor.';

-- 4. TABELA DE ITENS DO DOCUMENTO (Snapshot com múltiplos produtos/serviços por documento)
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

COMMENT ON TABLE tb_item_documento IS 'Linhas de itens incluídas em cada documento/nota fiscal emitida.';

-- 5. ÍNDICES PARA PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_usuario_email ON tb_usuario(email);
CREATE INDEX IF NOT EXISTS idx_usuario_documento ON tb_usuario(documento);
CREATE INDEX IF NOT EXISTS idx_catalogo_usuario ON tb_item_catalogo(id_usuario);
CREATE INDEX IF NOT EXISTS idx_documento_usuario ON tb_documento(id_usuario);
CREATE INDEX IF NOT EXISTS idx_documento_usuario_tipo ON tb_documento(id_usuario, tipo);
CREATE INDEX IF NOT EXISTS idx_item_documento ON tb_item_documento(id_documento);

-- 6. TRIGGER PARA NUMERAÇÃO SEQUENCIAL AUTOMÁTICA POR USUÁRIO (OPCIONAL/SUPABASE)
CREATE OR REPLACE FUNCTION fn_gerar_numero_documento()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.numero IS NULL OR NEW.numero = 0 THEN
        SELECT COALESCE(MAX(numero), 0) + 1
          INTO NEW.numero
          FROM tb_documento
         WHERE id_usuario = NEW.id_usuario;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_gerar_numero_documento ON tb_documento;
CREATE TRIGGER trg_gerar_numero_documento
BEFORE INSERT ON tb_documento
FOR EACH ROW
EXECUTE FUNCTION fn_gerar_numero_documento();

-- 7. HABILITAÇÃO DE ROW LEVEL SECURITY (RLS) NO SUPABASE
ALTER TABLE tb_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_item_catalogo ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_documento ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_item_documento ENABLE ROW LEVEL SECURITY;

-- Políticas de acesso público para a API Spring Boot (que se conecta com usuário do postgres)
-- Obs: Usuários conectados via 'service_role' ou 'postgres' têm bypass automático no Supabase.
CREATE POLICY "Permitir acesso completo via servico postgres" ON tb_usuario FOR ALL USING (true);
CREATE POLICY "Permitir acesso completo via servico postgres" ON tb_item_catalogo FOR ALL USING (true);
CREATE POLICY "Permitir acesso completo via servico postgres" ON tb_documento FOR ALL USING (true);
CREATE POLICY "Permitir acesso completo via servico postgres" ON tb_item_documento FOR ALL USING (true);

-- 8. CARGA INICIAL DE TESTES (Exemplo para usuário inicial se necessário)
-- Os 12 produtos e serviços modelados no sistema:
-- Formatação de SO, Manutenção Preventiva, Rede Wi-Fi, Troca de Tela, Consultoria TI,
-- Câmera de Segurança, SSD 480GB, Memória RAM 8GB, Fonte ATX 500W, Cabo Cat6,
-- Teclado e Mouse Sem Fio, Roteador Wi-Fi 6.
