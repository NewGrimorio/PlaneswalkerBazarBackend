package com.betacom.mtgbazar.be.configurations;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Sicurezza dell'applicazione, governata dai PROFILI Spring:
 *
 *   - dev  : security disattivata (tutto permitAll). E' il profilo di
 *            lavoro quotidiano e quello dei test (H2).
 *   - prod : regole vere. In Fase B qui si aggancia la validazione JWT
 *            (oauth2ResourceServer); per ora documenta la mappa dei
 *            permessi ed e' fail-closed.
 *
 * Proprieta' voluta: se NESSUN profilo e' attivo non esiste alcuna
 * filter chain nostra e Boot blinda tutto da solo. L'errore possibile
 * e' "troppo chiuso", mai "troppo aperto".
 *
 * Il CORS vive QUI e non piu' in un WebMvcConfigurer (WebAngularConfig,
 * rimossa): quando la filter chain e' attiva e' lei a dover conoscere
 * origini e credenziali, altrimenti i preflight muoiono prima di
 * arrivare a Spring MVC. Una sola fonte di verita'.
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
     * include nell'hash lui; la verifica SOLO con matches(), mai
     * confrontando stringhe. Bean unico, condiviso da entrambi i profili
     * e dai test.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS per l'Angular su 4200. allowCredentials(true) serve gia' ora
     * e servira' soprattutto in Fase B: il refresh token viaggia in un
     * cookie HttpOnly. In produzione l'origine diventera' il dominio
     * reale del sito.
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
    // E' la BasicSecurityConfig del tutor, governata pero' dal profilo
    // e non da un'annotazione da togliere a mano.
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
    // PROFILO prod: la mappa dei permessi del sito, sei righe leggibili
    // come un contratto. STATELESS: niente sessione HTTP, l'identita'
    // arrivera' dal Bearer token a ogni richiesta (Fase B).
    // ------------------------------------------------------------------
    @Slf4j
    @Configuration
    @Profile("prod")
    public static class SicurezzaProd {

        @Bean
        public SecurityFilterChain filterChainProd(HttpSecurity http) throws Exception {
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
                        .anyRequest().authenticated());

            // FASE B: qui si aggancia la validazione dei Bearer token:
            // http.oauth2ResourceServer(oauth -> oauth.jwt(...));

            return http.build();
        }
    }
}