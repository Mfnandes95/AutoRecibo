package com.autorecibo.api.application;

import com.autorecibo.api.application.dto.DocumentoResumoResponse;
import com.autorecibo.api.application.service.CatalogoService;
import com.autorecibo.api.application.service.CatalogoService.ItemCatalogoResponse;
import com.autorecibo.api.application.service.DocumentoService;
import com.autorecibo.api.domain.model.Usuario;
import com.autorecibo.api.infrastructure.config.security.JwtService;
import com.autorecibo.api.infrastructure.persistence.repository.DocumentoRepository;
import com.autorecibo.api.infrastructure.persistence.repository.ItemCatalogoRepository;
import com.autorecibo.api.infrastructure.persistence.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final DocumentoRepository documentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final CatalogoService catalogoService;
    private final DocumentoService documentoService;

    // Controle de Rate Limiting em memória por IP (proteção contra força bruta)
    private static final int MAX_TENTATIVAS_POR_MINUTO = 20;
    private final Map<String, JanelaRateLimit> rateLimitPorIp = new ConcurrentHashMap<>();

    // ============================================================
    // LOGIN
    // ============================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!permitirRequisicao(httpRequest)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErroResponse("Muitas tentativas seguidas. Aguarde um minuto e tente novamente."));
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.login(),
                        request.senha()
                )
        );

        UserDetails usuario = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(usuario);

        return ResponseEntity.ok(new LoginResponse(token));
    }

    // ============================================================
    // REGISTRO (Com Conformidade LGPD e Prevenção a Enumeração)
    // ============================================================

    @PostMapping("/registro")
    public ResponseEntity<?> registro(
            @RequestBody RegistroRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!permitirRequisicao(httpRequest)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErroResponse("Muitas tentativas seguidas. Aguarde um minuto e tente novamente."));
        }

        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String documento = request.documento() == null ? "" : request.documento().replaceAll("\\D", "");
        String senha = request.senha();

        if (email.isBlank() || !email.matches("^\\S+@\\S+\\.\\S+$")) {
            return ResponseEntity.badRequest()
                    .body(new ErroResponse("Informe um endereço de e-mail válido."));
        }

        if (senha == null || senha.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(new ErroResponse("A senha deve possuir pelo menos 8 caracteres."));
        }

        if ((documento.length() != 11 && documento.length() != 14) || documento.matches("(\\d)\\1+")) {
            return ResponseEntity.badRequest()
                    .body(new ErroResponse("Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido."));
        }

        // Mensagem unificada para evitar enumeração de CPFs, CNPJs ou e-mails cadastrados
        if (usuarioRepository.existsByEmail(email) || usuarioRepository.existsByDocumento(documento)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErroResponse("Não foi possível concluir o cadastro com os dados informados."));
        }

        LocalDateTime agora = LocalDateTime.now();
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setDocumento(documento);
        usuario.setDataCriacao(agora);
        usuario.setTermosAceitosEm(agora);

        usuarioRepository.save(usuario);

        try {
            catalogoService.popularItensPadrao(email);
        } catch (Exception ignored) {}

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistroResponse("Usuário cadastrado com sucesso."));
    }

    // ============================================================
    // DIREITOS DO TITULAR — LGPD (Art. 18: Portabilidade e Exclusão)
    // ============================================================

    /**
     * Art. 18, II e V da LGPD: Exportação e portabilidade de todos os dados pessoais
     * e registros vinculados ao titular autenticado.
     */
    @GetMapping("/meus-dados")
    @Transactional(readOnly = true)
    public ResponseEntity<DadosTitularLgpdResponse> exportarMeusDados(
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
        }
        Usuario usuario = usuarioRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));

        List<ItemCatalogoResponse> catalogo = catalogoService.listar(usuario.getEmail());
        List<DocumentoResumoResponse> documentos = documentoService.listar(usuario.getEmail());

        return ResponseEntity.ok(new DadosTitularLgpdResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getDocumento(),
                usuario.getDataCriacao(),
                usuario.getTermosAceitosEm(),
                catalogo,
                documentos
        ));
    }

    /**
     * Art. 18, IV e VI da LGPD: Eliminação definitiva da conta e de todos os dados
     * pessoais, catálogo e documentos associados ao titular.
     */
    @DeleteMapping("/conta")
    @Transactional
    public ResponseEntity<Map<String, String>> excluirMinhaConta(
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
        }
        Usuario usuario = usuarioRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));

        documentoRepository.deleteAllByUsuarioId(usuario.getId());
        itemCatalogoRepository.deleteAllByUsuario_Id(usuario.getId());
        usuarioRepository.delete(usuario);

        return ResponseEntity.ok(Map.of(
                "mensagem", "Sua conta e todos os dados vinculados foram eliminados permanentemente conforme a LGPD."
        ));
    }

    private boolean permitirRequisicao(HttpServletRequest req) {
        String ip = req != null && req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
        long minutoAtual = System.currentTimeMillis() / 60_000L;
        JanelaRateLimit janela = rateLimitPorIp.compute(ip, (k, atual) -> {
            if (atual == null || atual.minuto != minutoAtual) {
                return new JanelaRateLimit(minutoAtual, new AtomicInteger(1));
            }
            atual.contador.incrementAndGet();
            return atual;
        });
        return janela.contador.get() <= MAX_TENTATIVAS_POR_MINUTO;
    }

    private record JanelaRateLimit(long minuto, AtomicInteger contador) {}

    // ============================================================
    // DTOs
    // ============================================================

    public record LoginRequest(String login, String senha) {}

    public record LoginResponse(String token) {}

    public record RegistroRequest(
            String email,
            String senha,
            String documento,
            Boolean aceiteLgpd
    ) {}

    public record RegistroResponse(String mensagem) {}

    public record ErroResponse(String message) {}

    public record DadosTitularLgpdResponse(
            UUID idUsuario,
            String email,
            String documento,
            LocalDateTime dataCriacao,
            LocalDateTime termosAceitosEm,
            List<ItemCatalogoResponse> catalogo,
            List<DocumentoResumoResponse> documentosEmitidos
    ) {}
}
