package com.autorecibo.api.application;

import com.autorecibo.api.domain.model.Usuario;
import com.autorecibo.api.infrastructure.config.security.JwtService;
import com.autorecibo.api.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("Deve auto-popular e listar itens via API REST")
    void deveAutoPopularEListarItensViaApi() throws Exception {
        String email = "controller.teste." + UUID.randomUUID() + "@exemplo.com";
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

        // 1. GET inicial auto-popula com os 12 itens padrão de produtos e serviços
        mockMvc.perform(get("/api/catalogo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].nome", not(emptyOrNullString())))
                .andExpect(jsonPath("$[0].preco", notNullValue()));

        // 2. Cadastrar novo item manualmente
        mockMvc.perform(post("/api/catalogo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "nome": "Instalação de Servidor Linux",
                            "descricao": "Instalação e configuração de servidor Ubuntu",
                            "preco": 350.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Instalação de Servidor Linux"))
                .andExpect(jsonPath("$.preco").value(350.00));

        // 3. GET deve retornar 13 itens
        mockMvc.perform(get("/api/catalogo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(13)));

        // 4. Importar planilha
        File planilha = new File("planilha_produtos_teste.xlsx");
        MockMultipartFile file;
        try (FileInputStream fis = new FileInputStream(planilha)) {
            file = new MockMultipartFile(
                    "file",
                    "planilha_produtos_teste.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fis
            );
        }

        mockMvc.perform(multipart("/api/catalogo/importar")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalImportados").value(12));
    }
}
