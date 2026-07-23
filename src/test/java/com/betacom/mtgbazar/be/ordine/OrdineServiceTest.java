package com.betacom.mtgbazar.be.ordine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import com.betacom.mtgbazar.be.dto.users.StoricoStatoOrdineDTO;
import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.RuoloUtente;
import com.betacom.mtgbazar.be.model.users.enums.TipoSpedizione;
import com.betacom.mtgbazar.be.repositories.products.IMagazzinoSKURepository;
import com.betacom.mtgbazar.be.repositories.products.IProdottoRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.request.users.CheckoutReq;
import com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq;
import com.betacom.mtgbazar.be.request.users.IndirizzoReq;
import com.betacom.mtgbazar.be.request.users.RicaricaReq;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.VoceCarrelloReq;
import com.betacom.mtgbazar.be.services.interfaces.users.ICarrelloServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IIndirizzoServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IOrdineServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IPortafoglioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;

import lombok.extern.slf4j.Slf4j;
 
/**
 * Test di OrdineImpl su H2: il checkout attraversa TUTTI i service
 * costruiti finora (utente, indirizzo, portafoglio, carrello).
 * Fixture: prodotto ACCESSORIO con SKU (giacenza 5, prezzo 10.00).
 * Prefisso ord.
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc   //Messa nei service perche' i primi controller la implementano
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrdineServiceTest {
 
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
    private MagazzinoSKU sku;          // giacenza 5, prezzo 10.00
 
    @BeforeEach
    public void setUp() {
        int n = SEQ.incrementAndGet();
 
        utente = creaCliente("ord" + n + "@test.it");
        indirizzo = creaIndirizzo(utente.getId());
 
        Prodotto p = new Prodotto();
        p.setTipoProdotto(TipoProdotto.ACCESSORIO);
        p.setNome("Playmat Planeswalker " + n);
        p.setSlug("playmat-planeswalker-" + n);
        prodottoR.save(p);
 
        sku = new MagazzinoSKU();
        sku.setProdotto(p);
        sku.setPrezzo(new BigDecimal("10.00"));
        sku.setQuantita(5);
        skuR.save(sku);
 
        log.debug("setUp: utente={} indirizzo={} sku={} (giacenza 5, prezzo 10.00)",
                utente.getId(), indirizzo.getId(), sku.getId());
    }
 
    // ------------------------------------------------------------------
    // Helper fixture
    // ------------------------------------------------------------------
 
    private UtenteDTO creaCliente(String email) {
        UtenteReq req = new UtenteReq();
        req.setEmail(email);
        req.setUsername(email.substring(0, email.indexOf('@')));  // localpart: univoca come l'email
        req.setPassword("passwordSicura1");
        req.setNome("Anna");
        req.setCognome("Verdi");
        req.setDataNascita(LocalDate.of(1990, 4, 4));
        return utenteS.registraUtente(req);
    }
 
    private IndirizzoDTO creaIndirizzo(Long utenteId) {
        IndirizzoReq req = new IndirizzoReq();
        req.setUtenteId(utenteId);
        req.setEtichetta("Casa");
        req.setDestinatario("Anna Verdi");
        req.setVia("Via Etnea");
        req.setCivico("100");
        req.setCap("95131");
        req.setCitta("Catania");
        req.setProvincia("CT");
        return indirizzoS.createIndirizzo(req);
    }
 
    /** Porta il saldo dell'utente all'importo dato (bonifico + conferma). */
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
 
    private void mettiNelCarrello(Long utenteId, Long skuId, int quantita) {
        VoceCarrelloReq req = new VoceCarrelloReq();
        req.setUtenteId(utenteId);
        req.setSkuId(skuId);
        req.setQuantita(quantita);
        carrelloS.addVoce(req);
    }
 
    private CheckoutReq checkoutReq(Long utenteId, Long indirizzoId) {
        CheckoutReq req = new CheckoutReq();
        req.setUtenteId(utenteId);
        req.setIndirizzoId(indirizzoId);
        return req;
    }
 
    private int giacenza() {
        return skuR.findById(sku.getId()).orElseThrow().getQuantita();
    }
 
    private BigDecimal saldo(Long utenteId) {
        return portafoglioS.getByUtente(utenteId).getSaldo();
    }
 
    // ------------------------------------------------------------------
    // CHECKOUT
    // ------------------------------------------------------------------
 
    @Test
    @Order(1)
    public void checkoutFeliceConTuttiGliEffetti() {
        log.debug("TEST 1: checkout 3x10.00 con saldo 50 — verifica di TUTTI gli effetti");
        accredita(utente.getId(), "50.00");
        mettiNelCarrello(utente.getId(), sku.getId(), 3);

        OrdineDTO ordine = ordineS.checkout(checkoutReq(utente.getId(), indirizzo.getId()));
        log.debug("ordine creato: id={} stato={} totale={}",
                ordine.getId(), ordine.getStato(), ordine.getTotale());

        // ordine e snapshot — 30.00 di merce, sotto soglia: STANDARD a 4.90
        assertEquals("CREATO", ordine.getStato());
        assertEquals(0, new BigDecimal("34.90").compareTo(ordine.getTotale()));
        assertEquals(0, new BigDecimal("4.90").compareTo(ordine.getSpeseSpedizione()));
        assertEquals("STANDARD", ordine.getTipoSpedizione());
        assertEquals("Via Etnea", ordine.getSpedVia());
        assertEquals("Catania", ordine.getSpedCitta());
        assertEquals(1, ordine.getVoci().size());
        assertEquals(3, ordine.getVoci().get(0).getQuantita());
        // la spedizione NON e' una voce d'ordine: il prezzo di riga resta la merce
        assertEquals(0, new BigDecimal("10.00").compareTo(ordine.getVoci().get(0).getPrezzoUnitario()));
        assertTrue(ordine.getVoci().get(0).getDescrizione().contains("Playmat"));

        // effetti collaterali
        assertEquals(2, giacenza());                                   // 5 - 3
        assertEquals(0, new BigDecimal("15.10").compareTo(saldo(utente.getId())));  // 50 - 34.90
        assertEquals(0, carrelloS.getCarrello(utente.getId()).getVoci().size());    // svuotato

        // ledger: l'ultimo movimento e' il PAGAMENTO_ORDINE
        List<MovimentoDTO> storico = portafoglioS.storico(utente.getId());
        log.debug("ledger: {}", storico.stream().map(MovimentoDTO::getTipo).toList());
        assertEquals("PAGAMENTO_ORDINE", storico.get(0).getTipo());
        assertEquals(ordine.getId(), storico.get(0).getOrdineId());

        // timeline: una riga, null -> CREATO
        List<StoricoStatoOrdineDTO> timeline = ordineS.getTimeline(ordine.getId(), utente.getId());
        assertEquals(1, timeline.size());
        assertNull(timeline.get(0).getStatoDa());
        assertEquals("CREATO", timeline.get(0).getStatoA());
        assertEquals(utente.getUsername(), timeline.get(0).getEseguitoDa());
    }
 
    @Test
    @Order(2)
    public void checkoutConSaldoInsufficienteFaRollbackTotale() {
        log.debug("TEST 2: saldo 10, carrello da 30 -> rifiuto e NULLA e' cambiato");
        accredita(utente.getId(), "10.00");
        mettiNelCarrello(utente.getId(), sku.getId(), 3);
 
        MtgException ex = assertThrows(MtgException.class,
                () -> ordineS.checkout(checkoutReq(utente.getId(), indirizzo.getId())));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Saldo del portafoglio insufficiente", ex.getMessage());
 
        // ROLLBACK: scorte, saldo e carrello intatti; nessun ordine
        assertEquals(5, giacenza());
        assertEquals(0, new BigDecimal("10.00").compareTo(saldo(utente.getId())));
        assertEquals(3, carrelloS.getCarrello(utente.getId()).getNumeroArticoli());
        assertEquals(0, ordineS.listOrdini(utente.getId()).size());
    }
 
    @Test
    @Order(3)
    public void checkoutConScorteInsufficientiRifiutato() {
        log.debug("TEST 3: 4 in carrello, la giacenza scende a 2 -> il checkout DEVE fermarsi");
        accredita(utente.getId(), "100.00");
        mettiNelCarrello(utente.getId(), sku.getId(), 4);
 
        // un altro acquisto brucia la giacenza sotto il carrello
        MagazzinoSKU vivo = skuR.findById(sku.getId()).orElseThrow();
        vivo.setQuantita(2);
        skuR.save(vivo);
 
        MtgException ex = assertThrows(MtgException.class,
                () -> ordineS.checkout(checkoutReq(utente.getId(), indirizzo.getId())));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Quantita' richiesta non disponibile", ex.getMessage());
 
        assertEquals(0, new BigDecimal("100.00").compareTo(saldo(utente.getId())));  // intatto
        assertEquals(0, ordineS.listOrdini(utente.getId()).size());
    }
 
    @Test
    @Order(4)
    public void checkoutConCarrelloVuotoRifiutato() {
        log.debug("TEST 4: checkout senza nulla nel carrello");
        accredita(utente.getId(), "50.00");
 
        MtgException ex = assertThrows(MtgException.class,
                () -> ordineS.checkout(checkoutReq(utente.getId(), indirizzo.getId())));
        assertEquals("Il carrello e' vuoto", ex.getMessage());
    }
 
    // ------------------------------------------------------------------
    // TRANSIZIONI E STATE MACHINE
    // ------------------------------------------------------------------
 
    @Test
    @Order(5)
    public void annullaRipristinaScorteERimborsa() {
        log.debug("TEST 5: annullo di un ordine CREATO -> scorte e denaro tornano");
        accredita(utente.getId(), "50.00");
        mettiNelCarrello(utente.getId(), sku.getId(), 3);
        OrdineDTO ordine = ordineS.checkout(checkoutReq(utente.getId(), indirizzo.getId()));
        assertEquals(2, giacenza());
        // addebito PRIMA dell'annullo: senza questo assert il test resterebbe
        // verde anche se la spedizione non venisse mai addebitata (50 -> 50)
        assertEquals(0, new BigDecimal("15.10").compareTo(saldo(utente.getId())));

        OrdineDTO annullato = ordineS.annulla(ordine.getId(), utente.getId());
        log.debug("dopo annullo: stato={} giacenza={} saldo={}",
                annullato.getStato(), giacenza(), saldo(utente.getId()));

        assertEquals("ANNULLATO", annullato.getStato());
        assertEquals(5, giacenza());                                        // ripristinate
        assertEquals(0, new BigDecimal("50.00").compareTo(saldo(utente.getId())));  // rimborsato TUTTO
        assertEquals("RIMBORSO", portafoglioS.storico(utente.getId()).get(0).getTipo());
        assertEquals(2, ordineS.getTimeline(ordine.getId(), utente.getId()).size());
    }
 
    @Test
    @Order(6)
    public void stateMachineGovernaIlCicloDiVitaCompleto() {
        log.debug("TEST 6: transizioni illegali rifiutate; flusso completo fino al rimborso");
        accredita(utente.getId(), "50.00");
        mettiNelCarrello(utente.getId(), sku.getId(), 2);
        OrdineDTO ordine = ordineS.checkout(checkoutReq(utente.getId(), indirizzo.getId()));
        assertEquals(0, new BigDecimal("4.90").compareTo(ordine.getSpeseSpedizione()));
        Utente admin = creaAdmin();
 
        // CREATO: confermaConsegna e' illegale
        MtgException ex = assertThrows(MtgException.class,
                () -> ordineS.confermaConsegna(ordine.getId(), utente.getId()));
        assertEquals("Cambio di stato non consentito", ex.getMessage());
 
        // CREATO -> SPEDITO (admin); ora annullare e' illegale
        ordineS.spedisci(ordine.getId(), admin.getId());
        assertThrows(MtgException.class, () -> ordineS.annulla(ordine.getId(), utente.getId()));
 
        // SPEDITO -> CONSEGNATO -> RESO_RICHIESTO -> RIMBORSATO
        ordineS.confermaConsegna(ordine.getId(), utente.getId());
        ordineS.richiediReso(ordine.getId(), utente.getId());
        BigDecimal saldoPrima = saldo(utente.getId());
        int giacenzaPrima = giacenza();
        OrdineDTO rimborsato = ordineS.rimborsa(ordine.getId(), admin.getId());
        log.debug("dopo rimborso: stato={} saldo={} giacenza={}",
                rimborsato.getStato(), saldo(utente.getId()), giacenza());
 
        assertEquals("RIMBORSATO", rimborsato.getStato());
        assertEquals(0, saldoPrima.add(ordine.getTotale()).compareTo(saldo(utente.getId())));
        assertEquals(giacenzaPrima, giacenza());   // NESSUN ripristino automatico
 
        // timeline completa: 5 tappe, l'ultima firmata dall'admin
        List<StoricoStatoOrdineDTO> timeline = ordineS.getTimeline(ordine.getId(), utente.getId());
        log.debug("timeline: {}", timeline.stream()
                .map(t -> t.getStatoDa() + "->" + t.getStatoA() + " (" + t.getEseguitoDa() + ")")
                .toList());
        assertEquals(5, timeline.size());
        assertEquals("RIMBORSATO", timeline.get(4).getStatoA());
    }
 
    @Test
    @Order(7)
    public void ordineAltruiInvisibile() {
        log.debug("TEST 7: l'ordine di A per B 'non esiste'");
        accredita(utente.getId(), "50.00");
        mettiNelCarrello(utente.getId(), sku.getId(), 1);
        OrdineDTO ordine = ordineS.checkout(checkoutReq(utente.getId(), indirizzo.getId()));
 
        UtenteDTO b = creaCliente("ord" + SEQ.incrementAndGet() + "b@test.it");
 
        MtgException ex = assertThrows(MtgException.class,
                () -> ordineS.getDettaglio(ordine.getId(), b.getId()));
        assertEquals("Ordine non trovato", ex.getMessage());
        assertThrows(MtgException.class, () -> ordineS.annulla(ordine.getId(), b.getId()));
    }
 
    // ------------------------------------------------------------------
    // CONCORRENZA: GLI ULTIMI PEZZI
    // ------------------------------------------------------------------
 
    @Test
    @Order(8)
    public void dueCheckoutConcorrentiSugliUltimiPezzi() throws Exception {
        log.debug("TEST 8: giacenza 3, due checkout da 2 pezzi -> ne passa UNO solo");
 
        // giacenza ridotta a 3: i due carrelli da 2 non ci stanno entrambi
        MagazzinoSKU vivo = skuR.findById(sku.getId()).orElseThrow();
        vivo.setQuantita(3);
        skuR.save(vivo);
 
        // secondo cliente completo
        UtenteDTO b = creaCliente("ord" + SEQ.incrementAndGet() + "c@test.it");
        IndirizzoDTO indB = creaIndirizzo(b.getId());
        accredita(utente.getId(), "50.00");
        accredita(b.getId(), "50.00");
        mettiNelCarrello(utente.getId(), sku.getId(), 2);
        mettiNelCarrello(b.getId(), sku.getId(), 2);
 
        CountDownLatch pronti = new CountDownLatch(2);
        CountDownLatch via = new CountDownLatch(1);
        AtomicInteger successi = new AtomicInteger();
        AtomicInteger rifiutati = new AtomicInteger();
 
        Runnable checkoutA = () -> corriAlCheckout(utente.getId(), indirizzo.getId(),
                pronti, via, successi, rifiutati);
        Runnable checkoutB = () -> corriAlCheckout(b.getId(), indB.getId(),
                pronti, via, successi, rifiutati);
 
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(checkoutA);
        pool.submit(checkoutB);
        pronti.await();
        via.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
 
        log.debug("esito: successi={} rifiutati={} giacenza={}",
                successi.get(), rifiutati.get(), giacenza());
 
        // il lock serializza: uno compra, l'altro trova 1 < 2
        assertEquals(1, successi.get());
        assertEquals(1, rifiutati.get());
        assertEquals(1, giacenza());               // 3 - 2, MAI negativa
    }
 
    private void corriAlCheckout(Long utenteId, Long indirizzoId,
            CountDownLatch pronti, CountDownLatch via,
            AtomicInteger successi, AtomicInteger rifiutati) {
        try {
            pronti.countDown();
            via.await();
            ordineS.checkout(checkoutReq(utenteId, indirizzoId));
            successi.incrementAndGet();
            log.debug("thread {}: checkout RIUSCITO", Thread.currentThread().getName());
        } catch (MtgException e) {
            rifiutati.incrementAndGet();
            log.debug("thread {}: checkout RIFIUTATO ({})",
                    Thread.currentThread().getName(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
 
    /** L'admin non nasce dalla registrazione: fixture via repository. */
    private Utente creaAdmin() {
        int n = SEQ.incrementAndGet();
        Utente admin = new Utente();
        admin.setEmail("ordadmin" + n + "@test.it");
        admin.setUsername("ordadmin" + n);
        admin.setPasswordHash("$2a$10$fintoHashPerITest0000000000000000000000000000000000");
        admin.setRuolo(RuoloUtente.ADMIN);
        admin.setNome("Alice");
        admin.setCognome("Admin");
        return utenteR.save(admin);
    }
    
 // ------------------------------------------------------------------
    // SPEDIZIONE
    // ------------------------------------------------------------------

    @Test
    @Order(9)
    public void spedizioneExpressSottoSogliaVieneAddebitata() {
        log.debug("TEST 9: express scelto sotto soglia -> 7.90 addebitati");
        accredita(utente.getId(), "50.00");
        mettiNelCarrello(utente.getId(), sku.getId(), 2);      // 20.00 di merce

        CheckoutReq req = checkoutReq(utente.getId(), indirizzo.getId());
        req.setTipoSpedizione(TipoSpedizione.EXPRESS);
        OrdineDTO ordine = ordineS.checkout(req);

        assertEquals(0, new BigDecimal("27.90").compareTo(ordine.getTotale()));
        assertEquals(0, new BigDecimal("7.90").compareTo(ordine.getSpeseSpedizione()));
        assertEquals("EXPRESS", ordine.getTipoSpedizione());
        assertEquals(0, new BigDecimal("22.10").compareTo(saldo(utente.getId())));
    }

    @Test
    @Order(10)
    public void sopraSogliaLaSpedizioneEOffertaEDiventaExpress() {
        log.debug("TEST 10: merce a 70.00 -> express offerta, la preferenza del client e' IGNORATA");
        // la fixture nasce con giacenza 5: serve piu' merce per toccare la soglia
        MagazzinoSKU vivo = skuR.findById(sku.getId()).orElseThrow();
        vivo.setQuantita(10);
        skuR.save(vivo);

        accredita(utente.getId(), "100.00");
        mettiNelCarrello(utente.getId(), sku.getId(), 7);      // 70.00 esatti: il confine

        CheckoutReq req = checkoutReq(utente.getId(), indirizzo.getId());
        req.setTipoSpedizione(TipoSpedizione.STANDARD);        // chiede standard...
        OrdineDTO ordine = ordineS.checkout(req);

        assertEquals(0, new BigDecimal("70.00").compareTo(ordine.getTotale()));   // nessun sovrapprezzo
        assertEquals(0, BigDecimal.ZERO.compareTo(ordine.getSpeseSpedizione()));
        assertEquals("EXPRESS", ordine.getTipoSpedizione());   // ...ma riceve express
        assertEquals(0, new BigDecimal("30.00").compareTo(saldo(utente.getId())));
    }

    @Test
    @Order(11)
    public void saldoCheCopreSoloLaMerceVieneRifiutato() {
        log.debug("TEST 11: saldo pari alla merce ma non alla spedizione -> rifiuto pulito");
        accredita(utente.getId(), "30.00");                    // esattamente la merce
        mettiNelCarrello(utente.getId(), sku.getId(), 3);      // 30.00 + 4.90 di spedizione

        MtgException ex = assertThrows(MtgException.class,
                () -> ordineS.checkout(checkoutReq(utente.getId(), indirizzo.getId())));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Saldo del portafoglio insufficiente", ex.getMessage());

        // rollback totale: nessun addebito, nessuna scorta consumata
        assertEquals(0, new BigDecimal("30.00").compareTo(saldo(utente.getId())));
        assertEquals(5, giacenza());
    }
    
}