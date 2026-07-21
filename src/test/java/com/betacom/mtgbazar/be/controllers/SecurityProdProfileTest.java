package com.betacom.mtgbazar.be.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * L'UNICO test che gira sotto profilo PROD: qui la security IMPONE,
 * non solo identifica. Codifica i tre curl del collaudo manuale
 * (401 / 200 / 401) e aggiunge il caso mai esercitato nemmeno a mano:
 * CLIENTE autenticato su /api/admin -> 403 (l'unico ramo
 * dell'AccessDeniedHandler che restava al buio).
 *
 * @ActiveProfiles("prod") sovrascrive lo spring.profiles.active=dev
 * delle properties di test: SOLO per questa classe si attiva
 * SicurezzaProd con le regole complete. Le altre classi restano in dev.
 *
 * NB tecnico: il vostro JwtAuthenticationConverter legge le authority
 * dal claim "roles" gia' prefissato ROLE_. Nei test col post-processor
 * jwt() quel converter NON gira, quindi le authority si iniettano a
 * mano con .authorities(...) usando gli stessi nomi ROLE_*, cosi' il
 * .hasRole("ADMIN") della filter chain le riconosce.
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("prod")
public class SecurityProdProfileTest {

    @Autowired private MockMvc mockMvc;

    /** Utente autenticato con un ruolo: subject + authority ROLE_<ruolo>. */
    private static RequestPostProcessor utente(long id, String ruolo) {
        return jwt()
                .jwt(j -> j.subject(String.valueOf(id)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + ruolo));
    }

    // ------------------------------------------------------------------
    // I tre curl del collaudo manuale, ora permanenti
    // ------------------------------------------------------------------

    @Test
    public void ospiteSuAreaClienteRespintoCon401() throws Exception {
        log.debug("PROD 1: nessun token su /api/carrello -> 401 (entry point JSON)");
        mockMvc.perform(get("/api/carrello"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("Accesso non autorizzato: effettua il login"));
    }

    @Test
    public void catalogoPubblicoApertoSenzaToken() throws Exception {
        log.debug("PROD 2: /api/public/espansioni -> 200 anche da ospite");
        mockMvc.perform(get("/api/public/espansioni"))
                .andExpect(status().isOk());
    }

    @Test
    public void ospiteSuAreaAdminRespintoCon401() throws Exception {
        log.debug("PROD 3: nessun token su /api/admin -> 401");
        mockMvc.perform(get("/api/admin/ordini").param("stato", "CREATO"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Il caso mai visto nemmeno a mano: autenticato ma senza il ruolo
    // ------------------------------------------------------------------

    @Test
    public void clienteSuAreaAdminRespintoCon403() throws Exception {
        log.debug("PROD 4: CLIENTE autenticato su /api/admin -> 403 (denied handler JSON)");
        mockMvc.perform(get("/api/admin/ordini").param("stato", "CREATO")
                        .with(utente(1L, "CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("Non hai i permessi per questa operazione"));
    }

    @Test
    public void adminSuAreaAdminAmmesso() throws Exception {
        log.debug("PROD 5: ADMIN autenticato su /api/admin -> passa la security (200)");
        // Nessun dato di setup: la coda per stato torna lista vuota, ma
        // cio' che conta e' che la SECURITY lo lasci passare (non 401/403).
        mockMvc.perform(get("/api/admin/ordini").param("stato", "CREATO")
                        .with(utente(1L, "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    public void clienteSuAreaClientePassaLaSecurity() throws Exception {
        log.debug("PROD 6: CLIENTE autenticato su /api/carrello -> la security lo ammette");
        // Superata la security, il controller ricava utenteId dal token
        // e chiede il carrello: l'importante e' che NON sia 401/403.
        mockMvc.perform(get("/api/carrello").with(utente(1L, "CLIENTE")))
                .andExpect(status().isOk());
    }
}