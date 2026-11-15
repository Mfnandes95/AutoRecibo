-- ============================================================
-- AutoRecibo — Complemento de Índices, Trigger e RLS para Supabase (PostgreSQL)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Índices adicionais para chaves estrangeiras e consultas por tipo
CREATE INDEX IF NOT EXISTS idx_documento_usuario_tipo ON public.tb_documento(id_usuario, tipo);
CREATE INDEX IF NOT EXISTS idx_item_documento_catalogo ON public.tb_item_documento(id_item_catalogo);

-- Função e Trigger para numeração sequencial automática por usuário
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

-- Habilitação de Row Level Security (RLS) no Supabase
ALTER TABLE public.tb_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_item_catalogo ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_documento ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_item_documento ENABLE ROW LEVEL SECURITY;

-- Políticas de isolamento por usuário autenticado
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
