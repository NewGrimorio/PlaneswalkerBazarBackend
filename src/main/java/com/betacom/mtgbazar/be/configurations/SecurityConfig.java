package com.betacom.mtgbazar.be.configurations;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Sicurezza dell'applicazione, governata dai PROFILI Spring:
 *
 *   - dev  : IDENTIFICA ma non IMPONE (Fase C). Il Resource Server
 *            valida i Bearer token, quindi i controller conoscono
 *            l'identita' (subject = id utente); l'autorizzazione
 *            pero' resta permitAll: nessuna porta chiusa in sviluppo.
 *   - prod : identificazione E enforcement (regole complete).
 *
 * Proprieta' voluta: se NESSUN profilo e' attivo non esiste alcuna
 * filter chain nostra e Boot blinda tutto da solo. L'errore possibile
 * e' "troppo chiuso", mai "troppo aperto".
 *
 * Convenzione dei path (il livello di autorizzazione si legge nell'URL):
 *   /api/auth/**    login, registrazione, refresh, logout  -> aperti
 *   /api/public/**  vetrina del negozio                    -> aperti
 *   /api/admin/**   pannello di gestione                   -> ADMIN
 *   tutto il resto                                         -> autenticato
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ------------------------------------------------------------------
    // PROFILO dev (Fase C): Resource Server ATTIVO, autorizzazione NO.
    // Chi manda un Bearer valido viene riconosciuto (i controller
    // leggono l'id dal token, come in prod); chi non lo manda passa
    // comunque, ma da anonimo — e gli endpoint identitari, dovendo
    // fare Long.valueOf del subject, falliranno: in dev il frontend
    // il token lo manda sempre (interceptor), quindi non accade mai
    // nell'uso normale, solo nei curl a mano senza token.
    // ------------------------------------------------------------------
    @Slf4j
    @Configuration
    @Profile("dev")
    public static class SicurezzaDev {

        @Bean
        public SecurityFilterChain filterChainDev(HttpSecurity http,
                                                  JwtDecoder jwtDecoder,
                                                  JwtAuthenticationConverter jwtAuthenticationConverter,
                                                  AuthenticationEntryPoint authEntryPoint)
                throws Exception {
            log.warn(":::: PROFILO dev — identita' dai token ATTIVA, autorizzazione DISATTIVATA ::::");

            http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authEntryPoint));

            return http.build();
        }
    }

    // ------------------------------------------------------------------
    // PROFILO prod: la mappa dei permessi + la validazione dei Bearer
    // token come Resource Server OAuth2. STATELESS: niente sessione,
    // l'identita' arriva dal token a ogni richiesta.
    // ------------------------------------------------------------------
    @Slf4j
    @Configuration
    @Profile("prod")
    public static class SicurezzaProd {

        @Bean
        public SecurityFilterChain filterChainProd(HttpSecurity http,
                                                   JwtDecoder jwtDecoder,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter,
                                                   AuthenticationEntryPoint authEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler)
                throws Exception {
            log.info(":::: PROFILO prod — Spring Security ATTIVA ::::");

            http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/registrazione",
                                         "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/immagini/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authEntryPoint));

            return http.build();
        }
    }
}