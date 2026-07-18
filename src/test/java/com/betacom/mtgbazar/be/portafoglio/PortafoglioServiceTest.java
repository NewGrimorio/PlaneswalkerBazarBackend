package com.betacom.mtgbazar.be.portafoglio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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

import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.model.users.ContoBancario;
import com.betacom.mtgbazar.be.model.users.Portafoglio;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.RuoloUtente;
import com.betacom.mtgbazar.be.repositories.users.IContoBancarioRepository;
import com.betacom.mtgbazar.be.repositories.users.IPortafoglioRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq;
import com.betacom.mtgbazar.be.request.users.PrelievoReq;
import com.betacom.mtgbazar.be.request.users.RicaricaReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IPortafoglioServices;

import lombok.extern.slf4j.Slf4j;

/**
 * Test di PortafoglioImpl su H2 (Flyway V1+V2 applicate dal contesto).
 * Ogni test crea il PROPRIO utente con email univoca (prefisso PWT):
 * i test restano indipendenti anche se ordinati con @Order.
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc   //Messa nei service perche' i primi controller la implementano
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PortafoglioServiceTest {

    @Autowired private IPortafoglioServices portafoglioS;
    @Autowired private IUtenteRepository utenteR;
    @Autowired private IPortafoglioRepository portafoglioR;
    @Autowired private IContoBancarioRepository contoR;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private Utente utente;
    private ContoBancario conto;

    @BeforeEach
    public void setUp() {
    	int n = SEQ.incrementAndGet();
        utente = new Utente();
        utente.setEmail("pwt" + n + "@test.it");
        utente.setUsername("pwt" + n);
        utente.setPasswordHash("$2a$10$fintoHashPerITest0000000000000000000000000000000000");
        utente.setRuolo(RuoloUtente.CLIENTE);
        utente.setNome("Test");
        utente.setCognome("Portafoglio");
        utenteR.save(utente);
        log.debug("setUp: creato utente {} ({})", utente.getId(), utente.getUsername());

        Portafoglio p = new Portafoglio();
        p.setUtente(utente);
        p.setSaldo(BigDecimal.ZERO);
        portafoglioR.save(p);

        conto = new ContoBancario();
        conto.setUtente(utente);
        conto.setIntestatario("Test Portafoglio");
        conto.setIban("IT60X0542811101000000123456");
        contoR.save(conto);
    }

    /** Ricarica il portafoglio del test con un bonifico gia' confermato. */
    private void accredita(String importo) {
        RicaricaReq ric = new RicaricaReq();
        ric.setUtenteId(utente.getId());
        ric.setImporto(new BigDecimal(importo));
        ric.setMetodo(MetodoMovimento.BONIFICO);
        MovimentoDTO mov = portafoglioS.ricarica(ric);

        ConfermaMovimentoReq conf = new ConfermaMovimentoReq();
        conf.setMovimentoId(mov.getId());
        conf.setApprovato(Boolean.TRUE);
        portafoglioS.confermaMovimento(conf);
        log.debug("accredita: saldo portato a {}", importo);
    }

    private BigDecimal saldoAttuale() {
        return portafoglioR.findByUtenteId(utente.getId()).orElseThrow().getSaldo();
    }

    // ------------------------------------------------------------------
    // RICARICA
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    public void ricaricaPayPalAccreditaIlNettoConCommissione() {
        log.debug("TEST 1: ricarica PayPal 10.00, attesi commissione 0.85 e saldo 9.15");

        RicaricaReq req = new RicaricaReq();
        req.setUtenteId(utente.getId());
        req.setImporto(new BigDecimal("10.00"));
        req.setMetodo(MetodoMovimento.PAYPAL);

        MovimentoDTO mov = portafoglioS.ricarica(req);
        log.debug("movimento: id={} stato={} commissione={}",
                mov.getId(), mov.getStato(), mov.getCommissione());

        // commissione = 10 * 5% + 0.35 = 0.85 -> netto 9.15
        assertEquals(0, new BigDecimal("0.85").compareTo(mov.getCommissione()));
        assertEquals("COMPLETATO", mov.getStato());
        assertNotNull(mov.getCompletionDate());
        assertEquals(0, new BigDecimal("9.15").compareTo(saldoAttuale()));
    }

    @Test
    @Order(2)
    public void ricaricaBonificoNonToccaIlSaldoFinoAllaConferma() {
        log.debug("TEST 2: ricarica bonifico 50.00, saldo fermo fino alla conferma admin");

        RicaricaReq req = new RicaricaReq();
        req.setUtenteId(utente.getId());
        req.setImporto(new BigDecimal("50.00"));
        req.setMetodo(MetodoMovimento.BONIFICO);

        MovimentoDTO mov = portafoglioS.ricarica(req);
        log.debug("movimento in attesa: id={} stato={}", mov.getId(), mov.getStato());

        assertEquals("IN_ATTESA", mov.getStato());
        assertEquals(0, BigDecimal.ZERO.compareTo(saldoAttuale()));   // saldo intoccato

        ConfermaMovimentoReq conf = new ConfermaMovimentoReq();
        conf.setMovimentoId(mov.getId());
        conf.setApprovato(Boolean.TRUE);
        MovimentoDTO confermato = portafoglioS.confermaMovimento(conf);
        log.debug("dopo conferma: stato={} saldo={}", confermato.getStato(), saldoAttuale());

        assertEquals("COMPLETATO", confermato.getStato());
        assertEquals(0, new BigDecimal("50.00").compareTo(saldoAttuale()));
    }

    @Test
    @Order(3)
    public void ricaricaConMetodoInternoRifiutata() {
        log.debug("TEST 3: ricarica con metodo INTERNO deve essere respinta");

        RicaricaReq req = new RicaricaReq();
        req.setUtenteId(utente.getId());
        req.setImporto(new BigDecimal("10.00"));
        req.setMetodo(MetodoMovimento.INTERNO);

        MtgException ex = assertThrows(MtgException.class, () -> portafoglioS.ricarica(req));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Metodo di ricarica non consentito", ex.getMessage());  // V2 su H2!
    }

    // ------------------------------------------------------------------
    // PRELIEVO
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    public void prelievoConSaldoInsufficienteRifiutato() {
        log.debug("TEST 4: prelievo 10.00 con saldo zero deve fallire");

        PrelievoReq req = new PrelievoReq();
        req.setUtenteId(utente.getId());
        req.setImporto(new BigDecimal("10.00"));
        req.setContoBancarioId(conto.getId());

        MtgException ex = assertThrows(MtgException.class, () -> portafoglioS.preleva(req));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Saldo del portafoglio insufficiente", ex.getMessage());
    }

    @Test
    @Order(5)
    public void prelievoDecurtaSubitoEIlRifiutoRiaccredita() {
        log.debug("TEST 5: prelievo 40 su 100 decurta subito; il rifiuto ri-accredita");
        accredita("100.00");

        PrelievoReq req = new PrelievoReq();
        req.setUtenteId(utente.getId());
        req.setImporto(new BigDecimal("40.00"));
        req.setContoBancarioId(conto.getId());
        MovimentoDTO mov = portafoglioS.preleva(req);
        log.debug("dopo prelievo: stato={} saldo={}", mov.getStato(), saldoAttuale());

        // decurtazione IMMEDIATA (prenotazione del denaro)
        assertEquals("IN_ATTESA", mov.getStato());
        assertEquals(0, new BigDecimal("60.00").compareTo(saldoAttuale()));

        // rifiuto admin -> ri-accredito
        ConfermaMovimentoReq conf = new ConfermaMovimentoReq();
        conf.setMovimentoId(mov.getId());
        conf.setApprovato(Boolean.FALSE);
        conf.setNota("IBAN errato");
        MovimentoDTO rifiutato = portafoglioS.confermaMovimento(conf);
        log.debug("dopo rifiuto: stato={} saldo={}", rifiutato.getStato(), saldoAttuale());

        assertEquals("RIFIUTATO", rifiutato.getStato());
        assertEquals(0, new BigDecimal("100.00").compareTo(saldoAttuale()));
    }

    @Test
    @Order(6)
    public void movimentoGiaLavoratoNonRilavorabile() {
        log.debug("TEST 6: doppia conferma dello stesso movimento (ledger append-only)");
        accredita("10.00");

        PrelievoReq req = new PrelievoReq();
        req.setUtenteId(utente.getId());
        req.setImporto(new BigDecimal("10.00"));
        req.setContoBancarioId(conto.getId());
        MovimentoDTO mov = portafoglioS.preleva(req);

        ConfermaMovimentoReq conf = new ConfermaMovimentoReq();
        conf.setMovimentoId(mov.getId());
        conf.setApprovato(Boolean.TRUE);
        portafoglioS.confermaMovimento(conf);
        log.debug("prima conferma ok, tento la seconda...");

        // seconda conferma sullo stesso movimento: ledger append-only
        MtgException ex = assertThrows(MtgException.class,
                () -> portafoglioS.confermaMovimento(conf));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Il movimento non e' piu' modificabile", ex.getMessage());
    }

    // ------------------------------------------------------------------
    // CONCORRENZA (pattern doppio CountDownLatch, Fase 2)
    // ------------------------------------------------------------------

    @Test
    @Order(7)
    public void duePrelieviConcorrentiNonSuperanoMaiIlSaldo() throws Exception {
        log.debug("TEST 7: due prelievi da 60 su saldo 100, ne deve passare UNO solo");
        accredita("100.00");

        CountDownLatch pronti = new CountDownLatch(2);
        CountDownLatch via = new CountDownLatch(1);
        AtomicInteger successi = new AtomicInteger();
        AtomicInteger rifiutati = new AtomicInteger();

        Runnable prelievo = () -> {
            PrelievoReq req = new PrelievoReq();
            req.setUtenteId(utente.getId());
            req.setImporto(new BigDecimal("60.00"));
            req.setContoBancarioId(conto.getId());
            try {
                pronti.countDown();
                via.await();                       // partenza simultanea
                portafoglioS.preleva(req);
                successi.incrementAndGet();
                log.debug("thread {}: prelievo RIUSCITO", Thread.currentThread().getName());
            } catch (MtgException e) {
                rifiutati.incrementAndGet();       // saldo.insufficiente
                log.debug("thread {}: prelievo RIFIUTATO ({})",
                        Thread.currentThread().getName(), e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(prelievo);
        pool.submit(prelievo);
        pronti.await();
        via.countDown();                           // sparo: partono insieme
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        log.debug("esito: successi={} rifiutati={} saldo={}",
                successi.get(), rifiutati.get(), saldoAttuale());

        // Il lock pessimistico serializza: uno passa, uno trova 40 < 60
        assertEquals(1, successi.get());
        assertEquals(1, rifiutati.get());
        assertEquals(0, new BigDecimal("40.00").compareTo(saldoAttuale()));
    }
}