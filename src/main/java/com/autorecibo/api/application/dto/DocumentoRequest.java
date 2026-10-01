package com.autorecibo.api.application.dto;

import com.autorecibo.api.domain.model.TipoDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Corpo de POST /api/documentos/preview e POST /api/documentos. */
public record DocumentoRequest(
        @NotNull TipoDocumento tipo,
        @NotBlank @Size(max = 120) String clienteNome,
        /** Opcional. CPF (11) ou CNPJ (14); pontuação é ignorada. */
        @Size(max = 18) String clienteDocumento,
        @Size(max = 500) String observacoes,
        @NotEmpty @Size(max = 100) List<@Valid ItemDocumentoRequest> itens
) {}