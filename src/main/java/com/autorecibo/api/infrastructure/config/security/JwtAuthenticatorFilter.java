package com.autorecibo.api.infrastructure.config.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que valida o token JWT nas requisições.
 *
 * IMPORTANTE: esta classe NÃO é um bean (sem @Component). Ela é instanciada na
 * SecurityConfig e entra apenas na cadeia do Spring Security. Como bean, o Spring
 * Boot a registraria também como filtro de servlet, e ela rodaria duas vezes.
 */
public class JwtAuthenticatorFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticatorFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Sem cabeçalho "Bearer ": segue a cadeia sem autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Extrai o token removendo "Bearer "
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            // 3. Valida e autentica no contexto do Spring Security
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Token expirado, malformado, com assinatura errada ou de usuário inexistente:
            // não autentica e deixa o Spring Security responder 401 (em vez de estourar 500).
            logger.debug("Token JWT rejeitado: " + e.getMessage());
        }

        // 4. Libera a requisição para o próximo filtro ou controlador
        filterChain.doFilter(request, response);
    }
}