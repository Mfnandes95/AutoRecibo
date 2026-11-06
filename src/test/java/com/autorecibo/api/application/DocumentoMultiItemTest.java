package com.autorecibo.api.application;

import com.autorecibo.api.application.dto.DocumentoRequest;
import com.autorecibo.api.application.dto.ItemDocumentoRequest;
import com.autorecibo.api.application.service.CatalogoService;
import com.autorecibo.api.domain.model.TipoDocumento;
import com.autorecibo.api.domain.model.Usuario;
import com.autorecibo.api.infrastructure.config.security.JwtService;
import com.autorecibo.api.infrastructure.persistence.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentoMultiItemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CatalogoService catalogoService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deve emitir uma única Nota Fiscal com múltiplos produtos e serviços e gerar PDF")
    void deveEmitirNotaFiscalComMultiplosItens() throws Exception {
        String email = "nota.fiscal." + UUID.randomUUID() + "@exemplo.com";
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenhaHash("hash123");
        usuario.setDocumento(String.valueOf(System.currentTimeMillis()).substring(0, 11));
        usuario.setDataCriacao(LocalDateTime.now());
        usuarioRepository.save(usuario);

        UserDetails userDetails = User.withUsername(email)
                .password("hash123")
                .authorities("ROLE_USER")
                .build();
        String token = jwtService.generateToken(userDetails);

        // 1. Obtém catálogo auto-populado (12 itens de produtos e serviços)
        List<CatalogoService.ItemCatalogoResponse> catalogo = catalogoService.listar(email);
        assertTrue(catalogo.size() >= 2, "Catálogo deve ter itens para teste");

        // Seleciona um serviço (ex: Formatação - 120.00) e um produto (ex: SSD - 210.00)
        CatalogoService.ItemCatalogoResponse itemServico = catalogo.stream()
                .filter(i -> i.nome().contains("Formatação"))
                .findFirst()
                .orElse(catalogo.get(0));

        CatalogoService.ItemCatalogoResponse itemProduto = catalogo.stream()
                .filter(i -> i.nome().contains("SSD") || !i.id().equals(itemServico.id()))
                .findFirst()
                .orElse(catalogo.get(1));

        // 2. Monta DocumentoRequest do tipo NOTA_FISCAL contendo os 2 itens
        DocumentoRequest request = new DocumentoRequest(
                TipoDocumento.NOTA_FISCAL,
                "Cliente Corporativo S/A",
                "12345678000195",
                "Faturamento referente a serviços de TI e peças aplicadas com garantia de 90 dias.",
                List.of(
                        new ItemDocumentoRequest(itemServico.id(), new BigDecimal("1.000"), 10), // 10% desc
                        new ItemDocumentoRequest(itemProduto.id(), new BigDecimal("2.000"), 0)   // 2 unidades, sem desc
                )
        );

        // 3. Emite a Nota Fiscal com múltiplos itens via POST /api/documentos
        MvcResult postResult = mockMvc.perform(post("/api/documentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("NOTA_FISCAL"))
                .andExpect(jsonPath("$.clienteNome").value("Cliente Corporativo S/A"))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(postResult.getResponse().getContentAsString());
        String documentoId = responseJson.get("id").asText();
        long numero = responseJson.get("numero").asLong();
        assertTrue(numero > 0);

        // 4. Gera e valida o PDF da Nota Fiscal com múltiplos itens via GET /api/documentos/{id}/pdf
        MvcResult pdfResult = mockMvc.perform(get("/api/documentos/" + documentoId + "/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 500, "O PDF da Nota Fiscal deve ter tamanho válido");
        String pdfHeader = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", pdfHeader, "O cabeçalho do arquivo gerado deve ser um PDF válido");
    }
}
