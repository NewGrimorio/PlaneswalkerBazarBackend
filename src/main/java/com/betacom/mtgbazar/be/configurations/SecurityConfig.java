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
 *   - dev  : security disattivata (tutto permitAll). E' il profilo di
 *            lavoro quotidiano e quello dei test (H2). Il login emette
 *            comunque token veri: manca solo l'ENFORCEMENT.
 *   - prod : regole vere + validazione JWT come Resource Server.
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

    /**
     * Encoder BCrypt (spring-security-crypto): il salt lo genera e lo
     * include nell'hash lui; la verifica SOLO con matches(). E' anche
     * l'encoder che il DaoAuthenticationProvider usa nel login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * L'AuthenticationManager globale: dietro c'e' il
     * DaoAuthenticationProvider che Boot costruisce da solo trovando
     * il nostro CustomUserDetailsService + il PasswordEncoder.
     * Lo usa AuthController.login().
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS per l'Angular su 4200. allowCredentials(true) e' essenziale:
     * il refresh token viaggia in un cookie HttpOnly e senza credenziali
     * abilitate il browser non lo invierebbe mai. In produzione
     * l'origine diventera' il dominio reale del sito.
     */
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
    // PROFILO dev: tutto aperto, ma dichiarato ad alta voce nel log.
    // ------------------------------------------------------------------
    @Slf4j
    @Configuration
    @Profile("dev")
    public static class SicurezzaDev {

        @Bean
        public SecurityFilterChain filterChainDev(HttpSecurity http) throws Exception {
            log.warn(":::: PROFILO dev — Spring Security DISATTIVATA (tutto permitAll) ::::");

            http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

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
                        // identita': i soli endpoint di auth raggiungibili da sloggati
                        .requestMatchers("/api/auth/login", "/api/auth/registrazione",
                                         "/api/auth/refresh", "/api/auth/logout").permitAll()
                        // vetrina del negozio
                        .requestMatchers("/api/public/**").permitAll()
                        // immagini prodotti/profili: le vetrine le mostrano da sloggati
                        .requestMatchers("/immagini/**").permitAll()
                        // monitoraggio: il solo health check e' pubblico
                        .requestMatchers("/actuator/health").permitAll()
                        // documentazione API (da valutare se chiuderla al go-live)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // pannello di gestione
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // DEFAULT DENY: tutto il resto richiede un utente autenticato.
                        // NB: /api/auth/me cade volutamente qui.
                        .anyRequest().authenticated())
                // 401/403 della filter chain in JSON {"msg": ...}
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // validazione dei Bearer token: firma, scadenza, ruoli
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authEntryPoint));

            return http.build();
        }
    }
}