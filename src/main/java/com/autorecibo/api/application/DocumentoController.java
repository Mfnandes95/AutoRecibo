package com.autorecibo.api.application;

import com.autorecibo.api.application.service.DocumentoService;
import com.autorecibo.api.application.dto.DocumentoRequest;
import com.autorecibo.api.application.dto.DocumentoResponse;
import com.autorecibo.api.application.dto.DocumentoResumoResponse;
import com.autorecibo.api.domain.model.TipoDocumento;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Todas as rotas exigem login (JWT). O usuário vem do token; o id do dono nunca vem do cliente.
 * O username do UserDetails é o email (ver CustomUserDetailsService).
 */
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService service;

    /** Espelho de revisão: calcula e devolve, sem gravar nada. */
    @PostMapping("/preview")
    public DocumentoResponse preview(@AuthenticationPrincipal UserDetails usuario,
                                     @Valid @RequestBody DocumentoRequest req) {
        return service.preview(usuario.getUsername(), req);
    }

    /** Emissão: recalcula, numera e persiste. Responde 201 com Location. */
    @PostMapping
    public ResponseEntity<DocumentoResponse> emitir(@AuthenticationPrincipal UserDetails usuario,
                                                    @Valid @RequestBody DocumentoRequest req) {
        DocumentoResponse emitido = service.emitir(usuario.getUsername(), req);
        return ResponseEntity.created(URI.create("/api/documentos/" + emitido.id())).body(emitido);
    }

    /** Últimos 50 documentos do usuário (alimenta o histórico do painel). */
    @GetMapping
    public List<DocumentoResumoResponse> listar(@AuthenticationPrincipal UserDetails usuario) {
        return service.listar(usuario.getUsername());
    }

    @GetMapping("/{id}")
    public DocumentoResponse buscar(@AuthenticationPrincipal UserDetails usuario, @PathVariable UUID id) {
        return service.buscar(usuario.getUsername(), id);
    }

    /**
     * Download do PDF gerado a partir do documento emitido.
     * Retorna o arquivo PDF pronto para visualização ou download.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UserDetails usuario, @PathVariable UUID id) {
        DocumentoResponse doc = service.buscar(usuario.getUsername(), id);
        byte[] pdf = service.gerarPdf(usuario.getUsername(), id);

        String prefixo = doc.tipo() == TipoDocumento.ORDEM_SERVICO ? "ordem-servico-" : "recibo-";
        String filename = prefixo + doc.numero() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}