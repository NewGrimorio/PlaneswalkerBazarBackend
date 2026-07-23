package com.betacom.mtgbazar.be.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
 
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
 
import com.betacom.mtgbazar.be.dto.users.IndirizzoDTO;
import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.RuoloUtente;
import com.betacom.mtgbazar.be.repositories.products.IMagazzinoSKURepository;
import com.betacom.mtgbazar.be.repositories.products.IProdottoRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq;
import com.betacom.mtgbazar.be.request.users.IndirizzoReq;
import com.betacom.mtgbazar.be.request.users.RicaricaReq;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IIndirizzoServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IOrdineServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IPortafoglioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;
import com.fasterxml.jackson.databind.ObjectMapper;
 
import lombok.extern.slf4j.Slf4j;
 
/**
 * Test del LAYER WEB via MockMvc: path (kebab-case), JSON, gruppi di
 * validazione e GlobalExceptionHandler — cio' che i test service non
 * vedono. Le fixture usano i service veri; le chiamate sotto esame
 * passano da HTTP. Prefisso api.
 *
 * FASE C: l'identita' non viaggia piu' nel body/param/URL ma nel token.
 * Nei test si simula con asUser(id) -> .with(jwt().jwt(sub = id)).
 * Gira in profilo dev: resource server ATTIVO (popola l'Authentication),
 * autorizzazione permitAll (nessuna porta chiusa).
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ControllerRestTest {
	
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
 
    @Autowired private IUtenteServices utenteS;
    @Autowired private IIndirizzoServices indirizzoS;
    @Autowired private IPortafoglioServices portafoglioS;
    @Autowired private IOrdineServices ordineS;
    @Autowired private IUtenteRepository utenteR;
    @Autowired private IProdottoRepository prodottoR;
    @Autowired private IMagazzinoSKURepository skuR;
 
    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String PASSWORD = "passwordSicura1";

    /** FASE C: "la richiesta arriva da questo utente" — subject = id. */
    private static RequestPostProcessor asUser(Long id) {
        return jwt().jwt(j -> j.subject(id.toString()));
    }
 
    // ------------------------------------------------------------------
    // Helper fixture (via service, NON via HTTP: sotto esame c'e' il web layer)
    // ------------------------------------------------------------------
 
    private UtenteDTO creaCliente() {
        UtenteReq req = new UtenteReq();
        int n = SEQ.incrementAndGet();
        req.setEmail("api" + n + "@test.it");
        req.setUsername("api" + n);
        req.setPassword(PASSWORD);
        req.setNome("Aldo");
        req.setCognome("Baglio");
        req.setDataNascita(LocalDate.of(1990, 1, 1));
        return utenteS.registraUtente(req);
    }
 
    private IndirizzoDTO creaIndirizzo(Long utenteId) {
        IndirizzoReq req = new IndirizzoReq();
        req.setUtenteId(utenteId);
        req.setDestinatario("Aldo Baglio");
        req.setVia("Via Etnea");
        req.setCivico("1");
        req.setCap("95131");
        req.setCitta("Catania");
        return indirizzoS.createIndirizzo(req);
    }
 
    private void accredita(Long utenteId, String importo) {
        RicaricaReq ric = new RicaricaReq();
        ric.setUtenteId(utenteId);
        ric.setImporto(new BigDecimal(importo));
        ric.setMetodo(MetodoMovimento.BONIFICO);
        MovimentoDTO mov = portafoglioS.ricarica(ric);
        ConfermaMovimentoReq conf = new ConfermaMovimentoReq();
        conf.setMovimentoId(mov.getId());
        conf.setApprovato(Boolean.TRUE);
        portafoglioS.confermaMovimento(conf);
    }
 
    private MagazzinoSKU creaProdottoConSku(String nome, String prezzo, int quantita) {
        Prodotto p = new Prodotto();
        p.setTipoProdotto(TipoProdotto.ACCESSORIO);
        p.setNome(nome);
        p.setSlug(nome.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        prodottoR.save(p);
        MagazzinoSKU sku = new MagazzinoSKU();
        sku.setProdotto(p);
        sku.setPrezzo(new BigDecimal(prezzo));
        sku.setQuantita(quantita);
        skuR.save(sku);
        return sku;
    }
 
    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }
 
    // ------------------------------------------------------------------
    // VALIDAZIONE E HANDLER VIA HTTP
    // ------------------------------------------------------------------
 
    @Test
    @Order(1)
    public void registrazioneViaHttpEPasswordMaiNelJson() throws Exception {
        log.debug("TEST 1: POST /registrazione -> 200, email normalizzata, NIENTE password nel JSON");

        UtenteReq req = new UtenteReq();
        String email = "api" + SEQ.incrementAndGet() + "@test.it";
        String username = email.substring(0, email.indexOf('@'));
        req.setEmail(email.toUpperCase());        // solo MAIUSCOLE: @Email la accetta,
                                                  // gli spazi no (validazione al confine!)
        req.setUsername(username.toUpperCase());  // idem: normalizzato dal service
        req.setPassword(PASSWORD);
        req.setNome("Giovanni");
        req.setCognome("Storti");
        req.setDataNascita(LocalDate.of(1985, 2, 2));

        mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))          // normalizzata in minuscolo
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.ruolo").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
    
    @Test
    @Order(2)
    public void registrazioneSenzaEmailBoccataDalGruppoCreate() throws Exception {
        log.debug("TEST 2: gruppo Create + handler + V2, tutto via HTTP");
 
        UtenteReq sporca = new UtenteReq();
        sporca.setEmail("  api" + SEQ.get() + "@test.it ");
        sporca.setUsername("marina" + SEQ.get());  // valido: l'unica violazione deve restare l'email
        sporca.setPassword(PASSWORD);
        sporca.setNome("Marina");
        sporca.setCognome("Massironi");
        mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(sporca)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("Formato email non valido"));
    }

    @Test
    @Order(3)
    public void updateProfiloSenzaTokenERespinto() throws Exception {
        log.debug("TEST 3: FASE C — PUT profilo senza token -> 401 (id dal token, non dal body)");

        UtenteReq req = new UtenteReq();
        req.setNome("Senza");
        req.setCognome("Token");

        // Nessun .with(jwt()): in dev il principal e' anonimo, SecurityUtils
        // non riesce a ricavare l'id e alza AuthTokenException -> 401.
        mockMvc.perform(put("/api/utenti/profilo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    public void updateProfiloUsaIdDalTokenNonDalBody() throws Exception {
        log.debug("TEST 3b: FASE C — il profilo si aggiorna sull'utente del TOKEN, "
                + "un id altrui nel body viene ignorato (IDOR-proof)");

        UtenteDTO vero = creaCliente();      // e' lui che deve essere aggiornato
        UtenteDTO altro = creaCliente();     // la sua identita' NON deve essere toccata

        // Nel body infilo di proposito l'id di 'altro': deve essere ignorato.
        UtenteReq req = new UtenteReq();
        req.setId(altro.getId());            // <- tentativo di IDOR
        req.setNome("NuovoNome");
        req.setCognome("NuovoCognome");
        req.setUsername(vero.getUsername()); // Update valida lo username col Pattern

        // Il token dice: sono 'vero' (subject = id come stringa)
        mockMvc.perform(put("/api/utenti/profilo")
                        .with(asUser(vero.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vero.getId()))        // ha vinto il TOKEN
                .andExpect(jsonPath("$.nome").value("NuovoNome"));

        // Controprova: 'altro' e' rimasto intatto
        UtenteDTO altroDopo = utenteS.getById(altro.getId());
        org.junit.jupiter.api.Assertions.assertNotEquals("NuovoNome", altroDopo.getNome());
    }
 
    @Test
    @Order(4)
    public void loginErratoRestituisce401ColMessaggioDelHandler() throws Exception {
        log.debug("TEST 4: credenziali errate -> handler -> 401 via HTTP");
 
        UtenteDTO utente = creaCliente();
        String body = """
                {"identificativo": "%s", "password": "passwordSbagliata"}
                """.formatted(utente.getEmail());
 
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("Credenziali errate"));
    }
 
    // ------------------------------------------------------------------
    // CATALOGO PUBBLICO VIA HTTP
    // ------------------------------------------------------------------
 
    @Test
    @Order(5)
    public void paginaProdottoViaSlugConSkus() throws Exception {
        log.debug("TEST 5: GET /api/public/prodotti/{slug} -> dettaglio con varianti");
 
        int n = SEQ.incrementAndGet();
        MagazzinoSKU sku = creaProdottoConSku("Api Playmat " + n, "10.00", 5);
        String slug = sku.getProdotto().getSlug();
 
        mockMvc.perform(get("/api/public/prodotti/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Api Playmat " + n))
                .andExpect(jsonPath("$.skus[0].prezzo").value(10.00))
                .andExpect(jsonPath("$.skus[0].disponibile").value(true));
 
        // e uno slug inventato e' un 400 pulito, non un 500
        mockMvc.perform(get("/api/public/prodotti/{slug}", "non-esiste-" + n))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("Prodotto non trovato"));
    }
 
    // ------------------------------------------------------------------
    // IL FLUSSO E-COMMERCE COMPLETO VIA HTTP
    // ------------------------------------------------------------------
 
    @Test
    @Order(6)
    public void flussoCompletoCarrelloCheckoutConsegnaViaHttp() throws Exception {
        log.debug("TEST 6: add-to-cart -> checkout -> conferma-consegna, tutto via HTTP");
 
        UtenteDTO utente = creaCliente();
        IndirizzoDTO indirizzo = creaIndirizzo(utente.getId());
        accredita(utente.getId(), "50.00");
        MagazzinoSKU sku = creaProdottoConSku("Api Deckbox " + SEQ.incrementAndGet(), "15.00", 5);
 
        // add-to-cart via HTTP — FASE C: niente utenteId nel body, arriva dal token
        String voceBody = """
                {"skuId": %d, "quantita": 2}
                """.formatted(sku.getId());
        mockMvc.perform(post("/api/carrello/voci").with(asUser(utente.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voceBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroArticoli").value(2))
                .andExpect(jsonPath("$.totale").value(30.00));
 
        // checkout via HTTP
        String checkoutBody = """
                {"indirizzoId": %d}
                """.formatted(indirizzo.getId());
        MvcResult res = mockMvc.perform(post("/api/ordini/checkout").with(asUser(utente.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody))
			        .andExpect(status().isOk())
			        .andExpect(jsonPath("$.stato").value("CREATO"))
			        .andExpect(jsonPath("$.totale").value(34.90))     // 30.00 merce + 4.90 STANDARD
			        .andExpect(jsonPath("$.spedVia").value("Via Etnea"))
			        .andExpect(jsonPath("$.voci[0].quantita").value(2))
			        .andReturn();
        long ordineId = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("id").asLong();
        log.debug("ordine creato via HTTP: id={}", ordineId);
 
        // carrello svuotato — FASE C: GET /api/carrello (niente segmento), token
        mockMvc.perform(get("/api/carrello").with(asUser(utente.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroArticoli").value(0));
 
        // spedizione (fixture via service)
        int nAdmin = SEQ.incrementAndGet();
        Utente admin = new Utente();
        admin.setEmail("apiadmin" + nAdmin + "@test.it");
        admin.setUsername("apiadmin" + nAdmin);
        admin.setPasswordHash("$2a$10$fintoHashPerITest0000000000000000000000000000000000");
        admin.setRuolo(RuoloUtente.ADMIN);
        admin.setNome("Alice");
        admin.setCognome("Admin");
        utenteR.save(admin);
        ordineS.spedisci(ordineId, admin.getId());
 
        // conferma-consegna: il path KEBAB-CASE sotto esame, identita' dal token
        mockMvc.perform(post("/api/ordini/{id}/conferma-consegna", ordineId)
                        .with(asUser(utente.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("CONSEGNATO"));
 
        // timeline: 3 tappe via HTTP
        mockMvc.perform(get("/api/ordini/{id}/timeline", ordineId)
                        .with(asUser(utente.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].statoA").value("CONSEGNATO"));
    }
 
    @Test
    @Order(7)
    public void ordineAltruiViaHttpNonEsiste() throws Exception {
        log.debug("TEST 7: FASE C — B non puo' nemmeno DICHIARARSI un altro: e' il token a dirlo");
 
        UtenteDTO a = creaCliente();
        IndirizzoDTO indA = creaIndirizzo(a.getId());
        accredita(a.getId(), "50.00");
        MagazzinoSKU sku = creaProdottoConSku("Api Bustine " + SEQ.incrementAndGet(), "5.00", 10);
 
        String voceBody = """
                {"skuId": %d, "quantita": 1}
                """.formatted(sku.getId());
        mockMvc.perform(post("/api/carrello/voci").with(asUser(a.getId()))
                .contentType(MediaType.APPLICATION_JSON).content(voceBody))
                .andExpect(status().isOk());
        String checkoutBody = """
                {"indirizzoId": %d}
                """.formatted(indA.getId());
        MvcResult res = mockMvc.perform(post("/api/ordini/checkout").with(asUser(a.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutBody))
                .andExpect(status().isOk()).andReturn();
        long ordineId = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("id").asLong();
 
        // B, autenticato col SUO token, chiede l'ordine di A: il service non
        // lo trova per B -> 400. B non ha mai potuto "fingersi" A.
        UtenteDTO b = creaCliente();
        mockMvc.perform(get("/api/ordini/{id}", ordineId)
                        .with(asUser(b.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("Ordine non trovato"));
    }
 
    @Test
    @Order(8)
    public void ricaricaPayPalViaHttpCalcolaLaCommissione() throws Exception {
        log.debug("TEST 8: POST /api/portafoglio/ricarica -> commissione server-side nel JSON");
 
        UtenteDTO utente = creaCliente();
        // FASE C: niente utenteId nel body, arriva dal token
        String body = """
                {"importo": 10.00, "metodo": "PAYPAL"}
                """;
 
        mockMvc.perform(post("/api/portafoglio/ricarica").with(asUser(utente.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("COMPLETATO"))
                .andExpect(jsonPath("$.commissione").value(0.85));
 
        // GET saldo — FASE C: /api/portafoglio (niente segmento), token
        mockMvc.perform(get("/api/portafoglio").with(asUser(utente.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(9.15));
    }
    
}