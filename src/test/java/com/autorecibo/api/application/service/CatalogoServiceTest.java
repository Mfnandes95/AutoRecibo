package com.autorecibo.api.application.service;

import com.autorecibo.api.domain.model.Usuario;
import com.autorecibo.api.infrastructure.persistence.repository.ItemCatalogoRepository;
import com.autorecibo.api.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CatalogoServiceTest {

    @Autowired
    private CatalogoService catalogoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ItemCatalogoRepository itemCatalogoRepository;

    @Test
    @DisplayName("Deve importar com sucesso a planilha planilha_produtos_teste.xlsx gerada")
    void deveImportarPlanilhaComSucesso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("teste.catalogo." + UUID.randomUUID() + "@exemplo.com");
        usuario.setSenhaHash("hash123");
        usuario.setDocumento(String.valueOf(System.currentTimeMillis()).substring(0, 11));
        usuario.setDataCriacao(LocalDateTime.now());
        usuario = usuarioRepository.save(usuario);

        File planilha = new File("planilha_produtos_teste.xlsx");
        assertTrue(planilha.exists(), "O arquivo da planilha gerada deve existir");

        MockMultipartFile multipartFile;
        try (FileInputStream fis = new FileInputStream(planilha)) {
            multipartFile = new MockMultipartFile(
                    "file",
                    "planilha_produtos_teste.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fis
            );
        }

        int totalImportados = catalogoService.importarPlanilha(usuario.getEmail(), multipartFile);
        assertEquals(12, totalImportados, "A planilha deve conter exatamente 12 produtos e serviços");

        List<CatalogoService.ItemCatalogoResponse> itens = catalogoService.listar(usuario.getEmail());
        assertEquals(12, itens.size());
        assertTrue(itens.stream().anyMatch(i -> i.nome().contains("Formatação")));
        assertTrue(itens.stream().anyMatch(i -> i.nome().contains("SSD 480GB")));
    }
}
