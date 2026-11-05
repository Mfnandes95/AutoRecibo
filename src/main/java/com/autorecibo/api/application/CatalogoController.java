package com.autorecibo.api.application;

import com.autorecibo.api.application.service.CatalogoService;
import com.autorecibo.api.application.service.CatalogoService.ItemCatalogoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService service;

    /**
     * Lista todos os itens do catálogo do usuário logado.
     */
    @GetMapping
    public List<ItemCatalogoResponse> listar(@AuthenticationPrincipal UserDetails usuario) {
        return service.listar(usuario.getUsername());
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
}
