package com.autorecibo.api.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final UserDetailsService userDetailsService;
	private final JwtService jwtService;
	private final Environment environment;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		final boolean dev = environment.acceptsProfiles(Profiles.of("dev"));

		// Filtro criado aqui (e não como bean) para rodar só na cadeia do Security
		JwtAuthenticatorFilter jwtFilter = new JwtAuthenticatorFilter(jwtService, userDetailsService);

		http
			// API stateless com JWT: sem sessão, sem CSRF, sem form-login
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> {
				// Apenas login e registro são públicos; endpoints LGPD (/api/auth/meus-dados e /api/auth/conta) exigem JWT
				auth.requestMatchers("/api/auth/login", "/api/auth/registro", "/auth/login", "/auth/registro").permitAll();
				// Frontend estático (a tela de login precisa abrir sem token)
				auth.requestMatchers("/", "/index.html", "/cadastro.html", "/painel.html", "/css/**", "/js/**", "/favicon.ico", "/*.xlsx", "/*.csv").permitAll();
				// Console H2 somente no perfil dev
				if (dev) {
					auth.requestMatchers("/error", "/h2-console/**").permitAll();
				}
				auth.anyRequest().authenticated();
			})
			.headers(headers -> {
				headers.contentTypeOptions(cto -> {});
				headers.referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
				if (dev) {
					headers.frameOptions(frame -> frame.sameOrigin());
				} else {
					headers.frameOptions(frame -> frame.deny());
				}
			})
			// Sem token ou token inválido: 401 (o padrão seria 403)
			.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// static: evita dependência circular quando outros beans dependem do encoder
	@Bean
	static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

}
