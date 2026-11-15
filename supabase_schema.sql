-- ============================================================
-- AutoRecibo — Esquema Oficial do Banco de Dados no Supabase (PostgreSQL 17)
-- Projeto Supabase: AutoRecibo (ref: ewyircatwaityzznpqtv | sa-east-1)
-- ============================================================

-- Extensão para geração de UUIDs
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. TABELA DE USUÁRIOS (Emissores / MEIs)
CREATE TABLE IF NOT EXISTS public.tb_usuario (
    id_usuario UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    documento VARCHAR(14) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE public.tb_usuario IS 'Usuários emissores (MEIs e autônomos) cadastrados no sistema AutoRecibo.';

-- 2. CATÁLOGO DE ITENS (Produtos e Serviços)
CREATE TABLE IF NOT EXISTS public.tb_item_catalogo (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_usuario UUID NOT NULL,
    nome VARCHAR(200) NOT NULL,
    descricao VARCHAR(500),
    preco NUMERIC(15,2) NOT NULL CHECK (preco >= 0),
    CONSTRAINT fk_catalogo_usuario FOREIGN KEY (id_usuario)
        REFERENCES public.tb_usuario(id_usuario) ON DELETE CASCADE
);

COMMENT ON TABLE public.tb_item_catalogo IS 'Catálogo de produtos e serviços pertencentes a cada usuário.';

-- 3. DOCUMENTOS EMITIDOS (Notas Fiscais, Ordens de Serviço e Recibos)
CREATE TABLE IF NOT EXISTS public.tb_documento (
    id_documento UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_usuario UUID NOT NULL,
    numero BIGINT,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('NOTA_FISCAL', 'ORDEM_SERVICO', 'RECIBO')),
    cliente_nome VARCHAR(120) NOT NULL,
    cliente_documento VARCHAR(14),
    observacoes VARCHAR(500),
    subtotal NUMERIC(15,2) NOT NULL CHECK (subtotal >= 0),
    total_descontos NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (total_descontos >= 0),
    total NUMERIC(15,2) NOT NULL CHECK (total >= 0),
    emitido_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documento_usuario FOREIGN KEY (id_usuario)
        REFERENCES public.tb_usuario(id_usuario) ON DELETE CASCADE,
    CONSTRAINT uk_documento_usuario_numero UNIQUE (id_usuario, numero)
);

COMMENT ON TABLE public.tb_documento IS 'Documentos comerciais emitidos (Notas Fiscais, OS e Recibos).';

-- 4. ITENS DO DOCUMENTO (Snapshot com múltiplos produtos ou serviços por nota)
CREATE TABLE IF NOT EXISTS public.tb_item_documento (
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
        REFERENCES public.tb_documento(id_documento) ON DELETE CASCADE,
    CONSTRAINT fk_item_origem_catalogo FOREIGN KEY (id_item_catalogo)
        REFERENCES public.tb_item_catalogo(id) ON DELETE SET NULL
);

COMMENT ON TABLE public.tb_item_documento IS 'Itens discriminados de cada documento emitido (snapshot imutável).';

-- 5. ÍNDICES DE PERFORMANCE (Chaves Estrangeiras e Filtros)
CREATE INDEX IF NOT EXISTS idx_catalogo_usuario ON public.tb_item_catalogo(id_usuario);
CREATE INDEX IF NOT EXISTS idx_documento_usuario ON public.tb_documento(id_usuario);
CREATE INDEX IF NOT EXISTS idx_documento_usuario_tipo ON public.tb_documento(id_usuario, tipo);
CREATE INDEX IF NOT EXISTS idx_item_documento ON public.tb_item_documento(id_documento);
CREATE INDEX IF NOT EXISTS idx_item_documento_catalogo ON public.tb_item_documento(id_item_catalogo);

-- 6. FUNÇÃO E TRIGGER PARA NUMERAÇÃO SEQUENCIAL AUTOMÁTICA POR USUÁRIO
CREATE OR REPLACE FUNCTION public.fn_gerar_numero_documento()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
    IF NEW.numero IS NULL OR NEW.numero = 0 THEN
        SELECT COALESCE(MAX(numero), 0) + 1
          INTO NEW.numero
          FROM public.tb_documento
         WHERE id_usuario = NEW.id_usuario;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE TRIGGER trg_gerar_numero_documento
BEFORE INSERT ON public.tb_documento
FOR EACH ROW
EXECUTE FUNCTION public.fn_gerar_numero_documento();

-- 7. SEGURANÇA E POLÍTICAS RLS (ROW LEVEL SECURITY)
ALTER TABLE public.tb_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_item_catalogo ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_documento ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_item_documento ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Usuarios acessam apenas seu proprio registro"
    ON public.tb_usuario FOR ALL
    TO authenticated
    USING ((SELECT auth.uid()) = id_usuario)
    WITH CHECK ((SELECT auth.uid()) = id_usuario);

CREATE POLICY "Usuarios gerenciam apenas seu proprio catalogo"
    ON public.tb_item_catalogo FOR ALL
    TO authenticated
    USING ((SELECT auth.uid()) = id_usuario)
    WITH CHECK ((SELECT auth.uid()) = id_usuario);

CREATE POLICY "Usuarios gerenciam apenas seus proprios documentos"
    ON public.tb_documento FOR ALL
    TO authenticated
    USING ((SELECT auth.uid()) = id_usuario)
    WITH CHECK ((SELECT auth.uid()) = id_usuario);

CREATE POLICY "Usuarios acessam itens apenas de seus proprios documentos"
    ON public.tb_item_documento FOR ALL
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.tb_documento d
            WHERE d.id_documento = tb_item_documento.id_documento
              AND d.id_usuario = (SELECT auth.uid())
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.tb_documento d
            WHERE d.id_documento = tb_item_documento.id_documento
              AND d.id_usuario = (SELECT auth.uid())
        )
    );
