package com.autorecibo.api.application;

import com.autorecibo.api.application.service.CatalogoService;
import com.autorecibo.api.application.service.CatalogoService.ItemCatalogoResponse;
import com.autorecibo.api.application.service.CatalogoService.NovoItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService service;

    /**
     * Lista todos os itens do catálogo do usuário logado.
     * Caso o catálogo esteja vazio, popula automaticamente com os itens padrão.
     */
    @GetMapping
    public List<ItemCatalogoResponse> listar(@AuthenticationPrincipal UserDetails usuario) {
        return service.listar(usuario.getUsername());
    }

    /**
     * Cadastra um item manualmente no catálogo.
     */
    @PostMapping
    public ResponseEntity<ItemCatalogoResponse> criar(
            @AuthenticationPrincipal UserDetails usuario,
            @Valid @RequestBody NovoItemRequest request) {
        ItemCatalogoResponse salvo = service.criarItem(usuario.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    /**
     * Exclui um item específico do catálogo.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UserDetails usuario,
            @PathVariable UUID id) {
        service.excluirItem(usuario.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Importa itens a partir de uma planilha .xlsx.
     */
    @PostMapping("/importar")
    public ResponseEntity<Map<String, Object>> importar(
            @AuthenticationPrincipal UserDetails usuario,
            @RequestParam("file") MultipartFile file) {
        int total = service.importarPlanilha(usuario.getUsername(), file);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Planilha importada com sucesso.",
                "totalImportados", total
        ));
    }

    /**
     * Restaura os 12 itens de teste padrão no catálogo do usuário.
     */
    @PostMapping("/popular-teste")
    public ResponseEntity<Map<String, Object>> popularTeste(@AuthenticationPrincipal UserDetails usuario) {
        int total = service.popularItensPadrao(usuario.getUsername());
        return ResponseEntity.ok(Map.of(
                "mensagem", "12 itens de produtos e serviços para teste carregados com sucesso.",
                "totalImportados", total
        ));
    }
}
