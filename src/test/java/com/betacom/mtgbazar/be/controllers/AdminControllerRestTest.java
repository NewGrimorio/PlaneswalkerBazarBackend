package com.betacom.mtgbazar.be.controllers;

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
 
import com.betacom.mtgbazar.be.dto.users.IndirizzoDTO;
import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.dto.users.OrdineDTO;
import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
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
import com.betacom.mtgbazar.be.request.users.CheckoutReq;
import com.betacom.mtgbazar.be.request.users.IndirizzoReq;
import com.betacom.mtgbazar.be.request.users.RecensioneReq;
import com.betacom.mtgbazar.be.request.users.RicaricaReq;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.VoceCarrelloReq;
import com.betacom.mtgbazar.be.services.interfaces.users.ICarrelloServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IIndirizzoServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IOrdineServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IPortafoglioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IRecensioneServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;
 
import lombok.extern.slf4j.Slf4j;
 
/**
 * Test del layer web ADMIN via MockMvc. NB: AdminSincronizzazioneController
 * NON e' testato qui: chiamerebbe Scryfall via Internet a ogni run della
 * suite (collaudato a mano; un test con RestClient mockato e' un
 * raffinamento futuro). Prefisso adm.
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminControllerRestTest {
 
    @Autowired private MockMvc mockMvc;
 
    @Autowired private IUtenteServices utenteS;
    @Autowired private IIndirizzoServices indirizzoS;
    @Autowired private IPortafoglioServices portafoglioS;
    @Autowired private ICarrelloServices carrelloS;
    @Autowired private IOrdineServices ordineS;
    @Autowired private IRecensioneServices recensioneS;
    @Autowired private IUtenteRepository utenteR;
    @Autowired private IProdottoRepository prodottoR;
    @Autowired private IMagazzinoSKURepository skuR;
 
    private static final AtomicInteger SEQ = new AtomicInteger();
 
    // ------------------------------------------------------------------
    // Helper fixture (via service; sotto esame c'e' SOLO il layer web)
    // ------------------------------------------------------------------
 
    private UtenteDTO creaCliente() {
        UtenteReq req = new UtenteReq();
        int n = SEQ.incrementAndGet();
        req.setEmail("adm" + n + "@test.it");
        req.setUsername("adm" + n);
        req.setPassword("passwordSicura1");
        req.setNome("Rita");
        req.setCognome("Costa");
        req.setDataNascita(LocalDate.of(1988, 9, 9));
        return utenteS.registraUtente(req);
    }
 
    private Utente creaAdmin() {
        int n = SEQ.incrementAndGet();
        Utente a = new Utente();
        a.setEmail("admadmin" + n + "@test.it");
        a.setUsername("admadmin" + n);
        a.setPasswordHash("$2a$10$fintoHashPerITest0000000000000000000000000000000000");
        a.setRuolo(RuoloUtente.ADMIN);
        a.setNome("Alice");
        a.setCognome("Admin");
        return utenteR.save(a);
    }
 
    private Prodotto creaProdotto(String nome) {
        Prodotto p = new Prodotto();
        p.setTipoProdotto(TipoProdotto.ACCESSORIO);
        p.setNome(nome);
        p.setSlug(nome.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        return prodottoR.save(p);
    }
 
    private MagazzinoSKU creaSku(Prodotto p, String prezzo, int quantita) {
        MagazzinoSKU sku = new MagazzinoSKU();
        sku.setProdotto(p);
        sku.setPrezzo(new BigDecimal(prezzo));
        sku.setQuantita(quantita);
        return skuR.save(sku);
    }
 
    private void accredita(Long utenteId, String importo) {
        RicaricaReq ric = new RicaricaReq();
        ric.setUtenteId(utenteId);
        ric.setImporto(new BigDecimal(importo));
        ric.setMetodo(MetodoMovimento.BONIFICO);
        MovimentoDTO mov = portafoglioS.ricarica(ric);
        com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq conf =
                new com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq();
        conf.setMovimentoId(mov.getId());
        conf.setApprovato(Boolean.TRUE);
        portafoglioS.confermaMovimento(conf);
    }
 
    /** Ordine CREATO pronto per la lavorazione admin. */
    private OrdineDTO creaOrdine(UtenteDTO cliente, MagazzinoSKU sku) {
        IndirizzoReq ind = new IndirizzoReq();
        ind.setUtenteId(cliente.getId());
        ind.setDestinatario("Rita Costa");
        ind.setVia("Via Roma");
        ind.setCivico("1");
        ind.setCap("95100");
        ind.setCitta("Catania");
        IndirizzoDTO indirizzo = indirizzoS.createIndirizzo(ind);
        accredita(cliente.getId(), "50.00");
        VoceCarrelloReq voce = new VoceCarrelloReq();
        voce.setUtenteId(cliente.getId());
        voce.setSkuId(sku.getId());
        voce.setQuantita(1);
        carrelloS.addVoce(voce);
        CheckoutReq co = new CheckoutReq();
        co.setUtenteId(cliente.getId());
        co.setIndirizzoId(indirizzo.getId());
        return ordineS.checkout(co);
    }
 
    // ------------------------------------------------------------------
    // MAGAZZINO
    // ------------------------------------------------------------------
 
    @Test
    @Order(1)
    public void creaSkuViaHttpConDefaultEUnicoPerProdotto() throws Exception {
        log.debug("TEST 1: POST /api/admin/sku coi soli prezzo/quantita -> default; secondo SKU -> 400 (regola V7)");
        Prodotto p = creaProdotto("Adm Playmat " + SEQ.incrementAndGet());
 
        String body = """
                {"prodottoId": %d, "prezzo": 12.50, "quantita": 10}
                """.formatted(p.getId());
 
        mockMvc.perform(post("/api/admin/sku")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condizione").value("NA"))
                .andExpect(jsonPath("$.lingua").value("en"))
                .andExpect(jsonPath("$.finitura").value("NONFOIL"))
                .andExpect(jsonPath("$.disponibile").value(true));
 
        // REGOLA V7: un non-SINGLE ha al massimo UNO SKU — il guard risponde
        // PRIMA del controllo variante, qualunque sia la variante richiesta
        mockMvc.perform(post("/api/admin/sku")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("Questo prodotto ha gia' le scorte inserite: modificale dalla riga esistente"));
    }
 
    @Test
    @Order(2)
    public void updateSkuViaHttpCambiaPrezzoEGiacenza() throws Exception {
        log.debug("TEST 2: PUT /api/admin/sku -> prezzo nuovo, giacenza 0 -> non disponibile");
        Prodotto p = creaProdotto("Adm Deckbox " + SEQ.incrementAndGet());
        MagazzinoSKU sku = creaSku(p, "10.00", 5);
 
        String body = """
                {"id": %d, "prezzo": 14.00, "quantita": 0}
                """.formatted(sku.getId());
 
        mockMvc.perform(put("/api/admin/sku")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prezzo").value(14.00))
                .andExpect(jsonPath("$.quantita").value(0))
                .andExpect(jsonPath("$.disponibile").value(false));
    }
 
    // ------------------------------------------------------------------
    // CATALOGO
    // ------------------------------------------------------------------
 
    @Test
    @Order(3)
    public void creaProdottoViaHttpGeneraSlugERifiutaSingle() throws Exception {
        log.debug("TEST 3: POST prodotti -> slug generato; SINGLE dal form -> 400");
        int n = SEQ.incrementAndGet();
 
        String body = """
                {"tipoProdotto": "BOOSTER_BOX", "nome": "Adm Booster Box! N.%d"}
                """.formatted(n);
        mockMvc.perform(post("/api/admin/catalogo/prodotti")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("adm-booster-box-n-" + n))
                .andExpect(jsonPath("$.attivo").value(true));
 
        String single = """
                {"tipoProdotto": "SINGLE", "nome": "Carta furbetta %d"}
                """.formatted(n);
        mockMvc.perform(post("/api/admin/catalogo/prodotti")
                        .contentType(MediaType.APPLICATION_JSON).content(single))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("Il tipo di prodotto non e' modificabile"));
    }
 
    @Test
    @Order(4)
    public void creaEspansioneViaHttpNormalizzaERifiutaDuplicati() throws Exception {
        log.debug("TEST 4: POST espansioni -> codice minuscolo; doppione -> 400");
        int n = SEQ.incrementAndGet();
 
        String body = """
                {"codice": "AD%d", "nome": "Set admin %d", "tipoSet": "expansion"}
                """.formatted(n, n);
        mockMvc.perform(post("/api/admin/catalogo/espansioni")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codice").value("ad" + n));
 
        mockMvc.perform(post("/api/admin/catalogo/espansioni")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("Codice set gia' esistente"));
    }
 
    // ------------------------------------------------------------------
    // ORDINI: LA CODA E LE TRANSIZIONI
    // ------------------------------------------------------------------
 
    @Test
    @Order(5)
    public void codaOrdiniESpedizioneViaHttp() throws Exception {
        log.debug("TEST 5: coda CREATO -> spedisci -> transizione illegale -> 400");
        UtenteDTO cliente = creaCliente();
        Utente admin = creaAdmin();
        MagazzinoSKU sku = creaSku(creaProdotto("Adm Bustine " + SEQ.incrementAndGet()), "5.00", 10);
        OrdineDTO ordine = creaOrdine(cliente, sku);
 
        // l'ordine e' nella coda "da spedire"
        mockMvc.perform(get("/api/admin/ordini").param("stato", "CREATO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(ordine.getId())).exists());
 
        // spedizione via HTTP
        mockMvc.perform(post("/api/admin/ordini/{id}/spedisci", ordine.getId())
                        .param("adminId", admin.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("SPEDITO"));
 
        // cancellare uno SPEDITO e' illegale: la state machine via HTTP
        mockMvc.perform(post("/api/admin/ordini/{id}/cancella", ordine.getId())
                        .param("adminId", admin.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("Cambio di stato non consentito"));
    }
 
    @Test
    @Order(6)
    public void rimborsoViaHttpDopoReso() throws Exception {
        log.debug("TEST 6: reso richiesto -> POST rimborsa -> RIMBORSATO e saldo pieno");
        UtenteDTO cliente = creaCliente();
        Utente admin = creaAdmin();
        MagazzinoSKU sku = creaSku(creaProdotto("Adm Dado " + SEQ.incrementAndGet()), "8.00", 10);
        OrdineDTO ordine = creaOrdine(cliente, sku);
 
        // fixture via service fino a RESO_RICHIESTO
        ordineS.spedisci(ordine.getId(), admin.getId());
        ordineS.confermaConsegna(ordine.getId(), cliente.getId());
        ordineS.richiediReso(ordine.getId(), cliente.getId());
 
        mockMvc.perform(post("/api/admin/ordini/{id}/rimborsa", ordine.getId())
                        .param("adminId", admin.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("RIMBORSATO"));
 
        // il denaro e' tornato: saldo di nuovo 50.00 (via HTTP pubblica)
        mockMvc.perform(get("/api/portafoglio/{utenteId}", cliente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(50.00));
    }
 
    // ------------------------------------------------------------------
    // SPORTELLO BONIFICI
    // ------------------------------------------------------------------
 
    @Test
    @Order(7)
    public void codaMovimentiEConfermaViaHttp() throws Exception {
        log.debug("TEST 7: bonifico IN_ATTESA -> in coda -> conferma -> saldo accreditato");
        UtenteDTO cliente = creaCliente();
 
        RicaricaReq ric = new RicaricaReq();
        ric.setUtenteId(cliente.getId());
        ric.setImporto(new BigDecimal("75.00"));
        ric.setMetodo(MetodoMovimento.BONIFICO);
        MovimentoDTO mov = portafoglioS.ricarica(ric);
 
        // il movimento e' nella coda in-attesa (path KEBAB-CASE)
        mockMvc.perform(get("/api/admin/movimenti/in-attesa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(mov.getId())).exists());
 
        // conferma via HTTP
        String body = """
                {"movimentoId": %d, "approvato": true}
                """.formatted(mov.getId());
        mockMvc.perform(post("/api/admin/movimenti/conferma")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("COMPLETATO"));
 
        mockMvc.perform(get("/api/portafoglio/{utenteId}", cliente.getId()))
                .andExpect(jsonPath("$.saldo").value(75.00));
    }
 
    // ------------------------------------------------------------------
    // MODERAZIONE
    // ------------------------------------------------------------------
 
    @Test
    @Order(8)
    public void moderazioneViaHttpNascondeDallaLetturaPubblica() throws Exception {
        log.debug("TEST 8: modera(false) -> la GET pubblica non la mostra piu'");
        UtenteDTO cliente = creaCliente();
        Utente admin = creaAdmin();
        MagazzinoSKU sku = creaSku(creaProdotto("Adm Album " + SEQ.incrementAndGet()), "20.00", 10);
        OrdineDTO ordine = creaOrdine(cliente, sku);
        ordineS.spedisci(ordine.getId(), admin.getId());
        ordineS.confermaConsegna(ordine.getId(), cliente.getId());
 
        RecensioneReq rec = new RecensioneReq();
        rec.setUtenteId(cliente.getId());
        rec.setProdottoId(sku.getProdotto().getId());
        rec.setOrdineId(ordine.getId());
        rec.setVoto((short) 5);
        rec.setTitolo("Ottimo");
        rec.setTesto("Album perfetto per le mie Tazri.");
        RecensioneDTO recensione = recensioneS.saveRecensione(rec);
        Long prodottoId = sku.getProdotto().getId();
 
        // visibile al pubblico
        mockMvc.perform(get("/api/recensioni/prodotto/{prodottoId}", prodottoId))
                .andExpect(jsonPath("$.length()").value(1));
 
        // moderazione via HTTP
        mockMvc.perform(post("/api/admin/recensioni/{id}/modera", recensione.getId())
                        .param("approvata", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("RIFIUTATA"));
 
        // sparita dalla lettura pubblica, statistiche azzerate
        mockMvc.perform(get("/api/recensioni/prodotto/{prodottoId}", prodottoId))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/recensioni/prodotto/{prodottoId}/statistiche", prodottoId))
                .andExpect(jsonPath("$.conteggio").value(0));
    }
    
}
