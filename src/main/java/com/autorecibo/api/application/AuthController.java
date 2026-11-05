package com.autorecibo.api.application;

import com.autorecibo.api.domain.model.Usuario;
import com.autorecibo.api.infrastructure.config.security.JwtService;
import com.autorecibo.api.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;


    // ============================================================
    // LOGIN
    // ============================================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.login(),
                        request.senha()
                )
        );

        UserDetails usuario = (UserDetails) authentication.getPrincipal();

        String token = jwtService.generateToken(usuario);

        return ResponseEntity.ok(
                new LoginResponse(token)
        );
    }


    // ============================================================
    // REGISTRO
    // ============================================================

    @PostMapping("/registro")
    public ResponseEntity<?> registro(
            @RequestBody RegistroRequest request
    ) {

        // Remove espaços
        String email = request.email().trim().toLowerCase();
        String documento = request.documento().replaceAll("\\D", "");
        String senha = request.senha();

        // --------------------------------------------------------
        // Validações básicas
        // --------------------------------------------------------

        if (email.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(new ErroResponse("Email é obrigatório."));
        }

        if (senha == null || senha.length() < 6) {
            return ResponseEntity
                    .badRequest()
                    .body(new ErroResponse(
                            "A senha deve possuir pelo menos 6 caracteres."
                    ));
        }

        if (documento.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(new ErroResponse(
                            "CPF ou CNPJ é obrigatório."
                    ));
        }

        // --------------------------------------------------------
        // Verifica email duplicado
        // --------------------------------------------------------

        if (usuarioRepository.existsByEmail(email)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErroResponse(
                            "Email já cadastrado."
                    ));
        }

        // --------------------------------------------------------
        // Verifica documento duplicado
        // --------------------------------------------------------

        if (usuarioRepository.existsByDocumento(documento)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErroResponse(
                            "Documento já cadastrado."
                    ));
        }

        // --------------------------------------------------------
        // Cria usuário
        // --------------------------------------------------------

        Usuario usuario = new Usuario();

        usuario.setEmail(email);

        // NUNCA salvar a senha pura no banco.
        usuario.setSenhaHash(
                passwordEncoder.encode(senha)
        );

        usuario.setDocumento(documento);

        usuario.setDataCriacao(
                LocalDateTime.now()
        );

        usuarioRepository.save(usuario);

        // --------------------------------------------------------
        // Retorna sucesso
        // --------------------------------------------------------

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new RegistroResponse(
                        "Usuário cadastrado com sucesso."
                ));
    }


    // ============================================================
    // DTOs
    // ============================================================

    public record LoginRequest(
            String login,
            String senha
    ) {}


    public record LoginResponse(
            String token
    ) {}


    public record RegistroRequest(
            String email,
            String senha,
            String documento
    ) {}


    public record RegistroResponse(
            String mensagem
    ) {}


    public record ErroResponse(
            String message
    ) {}
}