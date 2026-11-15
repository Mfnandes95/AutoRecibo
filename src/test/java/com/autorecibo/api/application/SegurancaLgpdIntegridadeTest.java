package com.autorecibo.api.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SegurancaLgpdIntegridadeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deve validar políticas de segurança no registro (senha >= 8, CPF/CNPJ válido e anti-enumeração)")
    void deveValidarSegurancaNoRegistro() throws Exception {
        String sufixo = String.valueOf(System.nanoTime()).substring(0, 9);
        String cpfValido = "98" + sufixo;
        String email = "seguranca." + UUID.randomUUID() + "@autorecibo.com.br";

        // 1. Rejeita senha curta (< 8 caracteres)
        mockMvc.perform(post("/api/auth/registro")
                        .with(req -> { req.setRemoteAddr("10.0.0.1"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "senha": "123456",
                                    "documento": "%s"
                                }
                                """.formatted(email, cpfValido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("8 caracteres")));

        // 2. Rejeita CPF com dígitos repetidos (ex: 11111111111)
        mockMvc.perform(post("/api/auth/registro")
                        .with(req -> { req.setRemoteAddr("10.0.0.2"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "senha": "senhaForte123",
                                    "documento": "111.111.111-11"
                                }
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("CPF")));

        // 3. Cadastro válido
        mockMvc.perform(post("/api/auth/registro")
                        .with(req -> { req.setRemoteAddr("10.0.0.3"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "senha": "senhaForte123",
                                    "documento": "%s",
                                    "aceiteLgpd": true
                                }
                                """.formatted(email, cpfValido)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));

        // 4. Prevenção de enumeração: duplicidade de e-mail ou CPF retorna mensagem idêntica
        String msgEsperada = "Não foi possível concluir o cadastro com os dados informados.";

        mockMvc.perform(post("/api/auth/registro")
                        .with(req -> { req.setRemoteAddr("10.0.0.4"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "senha": "senhaForte123",
                                    "documento": "12345678909"
                                }
                                """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(msgEsperada));

        mockMvc.perform(post("/api/auth/registro")
                        .with(req -> { req.setRemoteAddr("10.0.0.5"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "outro.%s@autorecibo.com.br",
                                    "senha": "senhaForte123",
                                    "documento": "%s"
                                }
                                """.formatted(UUID.randomUUID(), cpfValido)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(msgEsperada));
    }

    @Test
    @DisplayName("Deve garantir os direitos do titular LGPD (Art. 18): portabilidade de dados e exclusão definitiva da conta")
    void deveGarantirDireitosDoTitularLgpd() throws Exception {
        String sufixo = String.valueOf(System.nanoTime()).substring(0, 9);
        String cpf = "76" + sufixo;
        String email = "lgpd." + UUID.randomUUID() + "@autorecibo.com.br";

        // 1. Sem token, endpoints LGPD devem negar acesso (401)
        mockMvc.perform(get("/api/auth/meus-dados"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/auth/conta"))
                .andExpect(status().isUnauthorized());

        // 2. Registra titular e realiza login
        mockMvc.perform(post("/api/auth/registro")
                        .with(req -> { req.setRemoteAddr("10.0.1.1"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "senha": "senhaSegura123",
                                    "documento": "%s",
                                    "aceiteLgpd": true
                                }
                                """.formatted(email, cpf)))
                .andExpect(status().isCreated());

        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                        .with(req -> { req.setRemoteAddr("10.0.1.2"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "login": "%s",
                                    "senha": "senhaSegura123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tokenJson = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        String token = tokenJson.get("token").asText();

        // 3. Exportação / Portabilidade dos dados pessoais (Art. 18, II e V)
        mockMvc.perform(get("/api/auth/meus-dados")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.documento").value(cpf))
                .andExpect(jsonPath("$.termosAceitosEm", notNullValue()))
                .andExpect(jsonPath("$.catalogo", hasSize(12)));

        // 4. Eliminação definitiva da conta e dados (Art. 18, IV e VI)
        mockMvc.perform(delete("/api/auth/conta")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem", containsString("eliminados permanentemente")));

        // 5. Token do titular excluído não deve mais autenticar
        mockMvc.perform(get("/api/catalogo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
