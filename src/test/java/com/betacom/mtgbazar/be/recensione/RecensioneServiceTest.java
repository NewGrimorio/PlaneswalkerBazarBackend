package com.betacom.mtgbazar.be.recensione;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.betacom.mtgbazar.be.dto.users.IndirizzoDTO;
import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.dto.users.OrdineDTO;
import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.dto.users.RecensioneStatisticheDTO;
import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
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
import com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq;
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
 * Test di RecensioneImpl su H2: il diritto di recensire nasce dal
 * ciclo completo checkout -> spedizione -> consegna, quindi il setUp
 * attraversa TUTTI i service dell'area user. Prefisso rec.
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc   //Messa nei service perche' i primi controller la implementano
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RecensioneServiceTest {
 
    @Autowired private IRecensioneServices recensioneS;
    @Autowired private IOrdineServices ordineS;
    @Autowired private ICarrelloServices carrelloS;
    @Autowired private IPortafoglioServices portafoglioS;
    @Autowired private IIndirizzoServices indirizzoS;
    @Autowired private IUtenteServices utenteS;
    @Autowired private IUtenteRepository utenteR;
    @Autowired private IProdottoRepository prodottoR;
    @Autowired private IMagazzinoSKURepository skuR;
 
    private static final AtomicInteger SEQ = new AtomicInteger();
 
    private UtenteDTO utente;
    private IndirizzoDTO indirizzo;
    private Prodotto prodotto;
    private MagazzinoSKU sku;
    private Utente admin;
 
    @BeforeEach
    public void setUp() {
        int n = SEQ.incrementAndGet();
        utente = creaCliente("rec" + n + "@test.it");
        indirizzo = creaIndirizzo(utente.getId());
        admin = creaAdmin();
 
        prodotto = new Prodotto();
        prodotto.setTipoProdotto(TipoProdotto.ACCESSORIO);
        prodotto.setNome("Deckbox Ultra Pro " + n);
        prodotto.setSlug("deckbox-ultra-pro-" + n);
        prodottoR.save(prodotto);
 
        sku = new MagazzinoSKU();
        sku.setProdotto(prodotto);
        sku.setPrezzo(new BigDecimal("15.00"));
        sku.setQuantita(20);
        skuR.save(sku);
 
        log.debug("setUp: utente={} prodotto={} sku={}", utente.getId(),
                prodotto.getId(), sku.getId());
    }
 
    // ------------------------------------------------------------------
    // Helper fixture
    // ------------------------------------------------------------------
 
    private UtenteDTO creaCliente(String email) {
        UtenteReq req = new UtenteReq();
        req.setEmail(email);
        req.setUsername(email.substring(0, email.indexOf('@')));  // localpart: univoca come l'email
        req.setPassword("passwordSicura1");
        req.setNome("Rita");
        req.setCognome("Costa");
        req.setDataNascita(LocalDate.of(1988, 9, 9));
        return utenteS.registraUtente(req);
    }
 
    private IndirizzoDTO creaIndirizzo(Long utenteId) {
        IndirizzoReq req = new IndirizzoReq();
        req.setUtenteId(utenteId);
        req.setDestinatario("Rita Costa");
        req.setVia("Via Roma");
        req.setCivico("1");
        req.setCap("95100");
        req.setCitta("Catania");
        return indirizzoS.createIndirizzo(req);
    }
 
    private Utente creaAdmin() {
        int n = SEQ.incrementAndGet();
        Utente a = new Utente();
        a.setEmail("recadmin" + n + "@test.it");
        a.setUsername("recadmin" + n);
        a.setPasswordHash("$2a$10$fintoHashPerITest0000000000000000000000000000000000");
        a.setRuolo(RuoloUtente.ADMIN);
        a.setNome("Alice");
        a.setCognome("Admin");
        return utenteR.save(a);
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
 
    /** Ciclo completo: carrello -> checkout -> spedito -> CONSEGNATO. */
    private OrdineDTO ordineConsegnato(UtenteDTO cliente, IndirizzoDTO ind, Long skuId) {
        accredita(cliente.getId(), "50.00");
        VoceCarrelloReq voce = new VoceCarrelloReq();
        voce.setUtenteId(cliente.getId());
        voce.setSkuId(skuId);
        voce.setQuantita(1);
        carrelloS.addVoce(voce);
        CheckoutReq co = new CheckoutReq();
        co.setUtenteId(cliente.getId());
        co.setIndirizzoId(ind.getId());
        OrdineDTO ordine = ordineS.checkout(co);
        ordineS.spedisci(ordine.getId(), admin.getId());
        return ordineS.confermaConsegna(ordine.getId(), cliente.getId());
    }
 
    private RecensioneReq buildReq(Long ordineId, int voto, String titolo) {
        RecensioneReq req = new RecensioneReq();
        req.setUtenteId(utente.getId());
        req.setProdottoId(prodotto.getId());
        req.setOrdineId(ordineId);
        req.setVoto((short) voto);
        req.setTitolo(titolo);
        req.setTesto("Ottimo prodotto, consegna rapida.");
        return req;
    }
 
    // ------------------------------------------------------------------
    // DIRITTO DI RECENSIRE
    // ------------------------------------------------------------------
 
    @Test
    @Order(1)
    public void recensioneSuOrdineConsegnatoPubblicata() {
        log.debug("TEST 1: ciclo completo -> recensione a 5 stelle pubblicata");
        OrdineDTO ordine = ordineConsegnato(utente, indirizzo, sku.getId());
 
        RecensioneDTO dto = recensioneS.saveRecensione(buildReq(ordine.getId(), 5, "Perfetto"));
        log.debug("recensione: id={} voto={} autore={} stato={}",
                dto.getId(), dto.getVoto(), dto.getAutore(), dto.getStato());
 
        assertEquals((short) 5, dto.getVoto());
        assertEquals("APPROVATA", dto.getStato());
        //assertEquals("Rita C.", dto.getAutore());              // mai email/id
        assertEquals(utente.getUsername(), dto.getAutore());   // identita' pubblica: mai email/id
        assertTrue(dto.getAcquistoVerificato());
 
        List<RecensioneDTO> lista = recensioneS.listByProdotto(prodotto.getId());
        assertEquals(1, lista.size());
 
        RecensioneStatisticheDTO stats = recensioneS.getStatistiche(prodotto.getId());
        log.debug("statistiche: media={} conteggio={}", stats.getMedia(), stats.getConteggio());
        assertEquals(5.0, stats.getMedia());
        assertEquals(1L, stats.getConteggio());
    }
 
    @Test
    @Order(2)
    public void ordineNonConsegnatoNonDaDiritto() {
        log.debug("TEST 2: ordine solo CREATO -> recensione rifiutata");
        accredita(utente.getId(), "50.00");
        VoceCarrelloReq voce = new VoceCarrelloReq();
        voce.setUtenteId(utente.getId());
        voce.setSkuId(sku.getId());
        voce.setQuantita(1);
        carrelloS.addVoce(voce);
        CheckoutReq co = new CheckoutReq();
        co.setUtenteId(utente.getId());
        co.setIndirizzoId(indirizzo.getId());
        OrdineDTO ordine = ordineS.checkout(co);   // resta CREATO
 
        MtgException ex = assertThrows(MtgException.class,
                () -> recensioneS.saveRecensione(buildReq(ordine.getId(), 5, "Troppo presto")));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Puoi recensire solo prodotti di ordini consegnati", ex.getMessage());
    }
 
    @Test
    @Order(3)
    public void prodottoNonNellOrdineNonRecensibile() {
        log.debug("TEST 3: l'ordine consegnato contiene un ALTRO prodotto");
        OrdineDTO ordine = ordineConsegnato(utente, indirizzo, sku.getId());
 
        // secondo prodotto MAI acquistato dall'utente
        Prodotto altro = new Prodotto();
        altro.setTipoProdotto(TipoProdotto.ACCESSORIO);
        altro.setNome("Dado spindown " + SEQ.incrementAndGet());
        altro.setSlug("dado-spindown-" + SEQ.get());
        prodottoR.save(altro);
 
        RecensioneReq req = buildReq(ordine.getId(), 5, "Mai comprato");
        req.setProdottoId(altro.getId());
 
        MtgException ex = assertThrows(MtgException.class,
                () -> recensioneS.saveRecensione(req));
        assertEquals("Puoi recensire solo prodotti di ordini consegnati", ex.getMessage());
    }
 
    @Test
    @Order(4)
    public void ordineAltruiNonDaDiritto() {
        log.debug("TEST 4: B prova a recensire citando l'ordine consegnato di A");
        OrdineDTO ordineDiA = ordineConsegnato(utente, indirizzo, sku.getId());
 
        UtenteDTO b = creaCliente("rec" + SEQ.incrementAndGet() + "b@test.it");
        RecensioneReq req = buildReq(ordineDiA.getId(), 1, "Scrocco");
        req.setUtenteId(b.getId());
 
        MtgException ex = assertThrows(MtgException.class,
                () -> recensioneS.saveRecensione(req));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Ordine non trovato", ex.getMessage());   // ownership
    }
 
    // ------------------------------------------------------------------
    // UNA PER PRODOTTO, MODERAZIONE, MEDIA
    // ------------------------------------------------------------------
 
    @Test
    @Order(5)
    public void secondoSalvataggioAggiornaSenzaDuplicare() {
        log.debug("TEST 5: risalvataggio -> stessa recensione, voto aggiornato");
        OrdineDTO ordine = ordineConsegnato(utente, indirizzo, sku.getId());
 
        RecensioneDTO prima = recensioneS.saveRecensione(buildReq(ordine.getId(), 5, "Perfetto"));
        RecensioneDTO dopo = recensioneS.saveRecensione(buildReq(ordine.getId(), 2, "Ripensamento"));
        log.debug("prima: id={} voto=5 — dopo: id={} voto={}",
                prima.getId(), dopo.getId(), dopo.getVoto());
 
        assertEquals(prima.getId(), dopo.getId());             // STESSA riga
        assertEquals((short) 2, dopo.getVoto());
        assertEquals(1, recensioneS.listByProdotto(prodotto.getId()).size());
        assertEquals(2.0, recensioneS.getStatistiche(prodotto.getId()).getMedia());
    }
 
    @Test
    @Order(6)
    public void moderazioneNascondeERipristina() {
        log.debug("TEST 6: rifiuto admin -> sparisce da lista e statistiche; poi ripristino");
        OrdineDTO ordine = ordineConsegnato(utente, indirizzo, sku.getId());
        RecensioneDTO r = recensioneS.saveRecensione(buildReq(ordine.getId(), 4, "Buono"));
 
        recensioneS.modera(r.getId(), Boolean.FALSE);
        assertEquals(0, recensioneS.listByProdotto(prodotto.getId()).size());
        RecensioneStatisticheDTO stats = recensioneS.getStatistiche(prodotto.getId());
        log.debug("dopo il rifiuto: media={} conteggio={}", stats.getMedia(), stats.getConteggio());
        assertEquals(0.0, stats.getMedia());                   // niente null, mai
        assertEquals(0L, stats.getConteggio());
 
        recensioneS.modera(r.getId(), Boolean.TRUE);
        assertEquals(1, recensioneS.listByProdotto(prodotto.getId()).size());
    }
 
    @Test
    @Order(7)
    public void mediaSuPiuUtentiArrotondataAUnDecimale() {
        log.debug("TEST 7: voti 5 e 4 da due clienti -> media 4.5, conteggio 2");
        OrdineDTO ordineA = ordineConsegnato(utente, indirizzo, sku.getId());
        recensioneS.saveRecensione(buildReq(ordineA.getId(), 5, "Perfetto"));
 
        UtenteDTO b = creaCliente("rec" + SEQ.incrementAndGet() + "c@test.it");
        IndirizzoDTO indB = creaIndirizzo(b.getId());
        OrdineDTO ordineB = ordineConsegnato(b, indB, sku.getId());
        RecensioneReq reqB = buildReq(ordineB.getId(), 4, "Molto buono");
        reqB.setUtenteId(b.getId());
        recensioneS.saveRecensione(reqB);
 
        RecensioneStatisticheDTO stats = recensioneS.getStatistiche(prodotto.getId());
        log.debug("statistiche finali: media={} conteggio={}",
                stats.getMedia(), stats.getConteggio());
        assertEquals(4.5, stats.getMedia());
        assertEquals(2L, stats.getConteggio());
        assertEquals(2, recensioneS.listByProdotto(prodotto.getId()).size());
    }
    
}