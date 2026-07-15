package com.betacom.mtgbazar.be.indirizzo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.request.users.IndirizzoReq;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IIndirizzoServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;

import lombok.extern.slf4j.Slf4j;

/**
 * Test di IndirizzoImpl su H2 (Flyway V1+V2 dal contesto).
 * Gli utenti nascono via utenteS.registraUtente (prefisso ind...):
 * il test usa i service veri, non costruisce entity a mano.
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc   //Messa nei service perche' i primi controller la implementano
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IndirizzoServiceTest {

    @Autowired private IIndirizzoServices indirizzoS;
    @Autowired private IUtenteServices utenteS;
    @Autowired private IUtenteRepository utenteR;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private UtenteDTO utente;

    @BeforeEach
    public void setUp() {
        int n = SEQ.incrementAndGet();
        UtenteReq req = new UtenteReq();
        req.setEmail("ind" + n + "@test.it");
        req.setPassword("passwordSicura1");
        req.setNome("Anna");
        req.setCognome("Verdi");
        req.setDataNascita(LocalDate.of(1985, 3, 10));
        utente = utenteS.registraUtente(req);
        log.debug("setUp: creato utente {} ({})", utente.getId(), utente.getEmail());
    }

    /** Request di indirizzo valida, con etichetta riconoscibile. */
    private IndirizzoReq buildReq(String etichetta) {
        IndirizzoReq req = new IndirizzoReq();
        req.setUtenteId(utente.getId());
        req.setEtichetta(etichetta);
        req.setDestinatario("Anna Verdi");
        req.setVia("Via Etnea");
        req.setCivico("100");
        req.setCap("95131");
        req.setCitta("Catania");
        req.setProvincia("CT");
        req.setNazione("it");                 // minuscola: deve uscire "IT"
        return req;
    }

    private Long predefinitoIdSuDb() {
        var u = utenteR.findById(utente.getId()).orElseThrow();
        return u.getIndirizzoPredefinito() == null
                ? null : u.getIndirizzoPredefinito().getId();
    }

    // ------------------------------------------------------------------
    // CREAZIONE E PREDEFINITO
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    public void primoIndirizzoDiventaPredefinitoENormalizzaNazione() {
        log.debug("TEST 1: il primo indirizzo diventa predefinito da solo");

        IndirizzoDTO casa = indirizzoS.createIndirizzo(buildReq("Casa"));
        log.debug("creato: id={} predefinito={} nazione={}",
                casa.getId(), casa.getPredefinito(), casa.getNazione());

        assertTrue(casa.getPredefinito());                    // primo -> default
        assertEquals("IT", casa.getNazione());                // normalizzata
        assertEquals(casa.getId(), predefinitoIdSuDb());      // FK sull'utente
    }

    @Test
    @Order(2)
    public void secondoIndirizzoNonRubaIlDefaultSalvoRichiesta() {
        log.debug("TEST 2: il secondo indirizzo NON diventa predefinito, il terzo (esplicito) si'");

        IndirizzoDTO casa = indirizzoS.createIndirizzo(buildReq("Casa"));
        IndirizzoDTO ufficio = indirizzoS.createIndirizzo(buildReq("Ufficio"));
        log.debug("dopo il secondo: predefinito su DB = {}", predefinitoIdSuDb());

        assertEquals(casa.getId(), predefinitoIdSuDb());      // resta il primo
        assertFalse(ufficio.getPredefinito());

        IndirizzoReq mare = buildReq("Mare");
        mare.setPredefinito(Boolean.TRUE);                    // richiesta esplicita
        IndirizzoDTO alMare = indirizzoS.createIndirizzo(mare);
        log.debug("dopo il terzo (predefinito=true): predefinito su DB = {}", predefinitoIdSuDb());

        assertEquals(alMare.getId(), predefinitoIdSuDb());
    }

    @Test
    @Order(3)
    public void setPredefinitoPromuoveEListaMostraIlFlagGiusto() {
        log.debug("TEST 3: promozione esplicita e flag calcolato nella lista");

        IndirizzoDTO casa = indirizzoS.createIndirizzo(buildReq("Casa"));
        IndirizzoDTO ufficio = indirizzoS.createIndirizzo(buildReq("Ufficio"));

        indirizzoS.setPredefinito(ufficio.getId(), utente.getId());

        List<IndirizzoDTO> lista = indirizzoS.listIndirizzi(utente.getId());
        log.debug("lista: {}", lista.stream()
                .map(i -> i.getEtichetta() + "=" + i.getPredefinito()).toList());

        assertEquals(2, lista.size());
        // esattamente UN predefinito, ed e' l'ufficio
        assertEquals(1, lista.stream().filter(IndirizzoDTO::getPredefinito).count());
        assertTrue(lista.stream()
                .filter(IndirizzoDTO::getPredefinito)
                .allMatch(i -> i.getId().equals(ufficio.getId())));
        assertEquals(casa.getId(), lista.get(0).getId());     // ordine di creazione
    }

    // ------------------------------------------------------------------
    // MODIFICA
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    public void updateAggiornaSoloICampiPresenti() {
        log.debug("TEST 4: update null-safe");

        IndirizzoDTO casa = indirizzoS.createIndirizzo(buildReq("Casa"));

        IndirizzoReq update = new IndirizzoReq();
        update.setId(casa.getId());
        update.setUtenteId(utente.getId());
        update.setVia("Via Umberto");
        update.setCivico("7");
        // tutto il resto assente: NON deve cambiare

        IndirizzoDTO dopo = indirizzoS.updateIndirizzo(update);
        log.debug("dopo update: via={} civico={} citta={}",
                dopo.getVia(), dopo.getCivico(), dopo.getCitta());

        assertEquals("Via Umberto", dopo.getVia());
        assertEquals("7", dopo.getCivico());
        assertEquals("Catania", dopo.getCitta());             // intoccata
        assertEquals("Casa", dopo.getEtichetta());            // intoccata
        assertTrue(dopo.getPredefinito());                    // resta il default
    }

    // ------------------------------------------------------------------
    // RIMOZIONE E FK
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    public void rimozioneDelPredefinitoAzzeraLaFkENonAppareInLista() {
        log.debug("TEST 5: rimozione del predefinito -> FK azzerata, soft delete");

        IndirizzoDTO casa = indirizzoS.createIndirizzo(buildReq("Casa"));
        IndirizzoDTO ufficio = indirizzoS.createIndirizzo(buildReq("Ufficio"));

        indirizzoS.removeIndirizzo(casa.getId(), utente.getId());
        log.debug("dopo remove: predefinito su DB = {}", predefinitoIdSuDb());

        assertNull(predefinitoIdSuDb());                      // FK azzerata PRIMA

        List<IndirizzoDTO> lista = indirizzoS.listIndirizzi(utente.getId());
        assertEquals(1, lista.size());                        // soft delete: sparito
        assertEquals(ufficio.getId(), lista.get(0).getId());

        // il rimosso non e' piu' raggiungibile nemmeno per update
        IndirizzoReq update = new IndirizzoReq();
        update.setId(casa.getId());
        update.setUtenteId(utente.getId());
        update.setVia("Via Fantasma");
        MtgException ex = assertThrows(MtgException.class,
                () -> indirizzoS.updateIndirizzo(update));
        assertEquals("Indirizzo non trovato", ex.getMessage());
    }

    // ------------------------------------------------------------------
    // OWNERSHIP
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    public void indirizzoAltruiInvisibileEIntoccabile() {
        log.debug("TEST 6: l'utente B non vede e non tocca gli indirizzi di A");

        IndirizzoDTO diA = indirizzoS.createIndirizzo(buildReq("Casa"));

        // secondo utente
        int n = SEQ.incrementAndGet();
        UtenteReq reqB = new UtenteReq();
        reqB.setEmail("ind" + n + "b@test.it");
        reqB.setPassword("passwordSicura1");
        reqB.setNome("Bruno");
        reqB.setCognome("Bianchi");
        UtenteDTO b = utenteS.registraUtente(reqB);

        // B tenta update, remove e promozione sull'indirizzo di A:
        // per lui "non esiste" — mai rivelare che esiste
        IndirizzoReq update = new IndirizzoReq();
        update.setId(diA.getId());
        update.setUtenteId(b.getId());
        update.setVia("Via Ladra");

        MtgException exU = assertThrows(MtgException.class,
                () -> indirizzoS.updateIndirizzo(update));
        MtgException exR = assertThrows(MtgException.class,
                () -> indirizzoS.removeIndirizzo(diA.getId(), b.getId()));
        MtgException exP = assertThrows(MtgException.class,
                () -> indirizzoS.setPredefinito(diA.getId(), b.getId()));
        log.debug("i tre tentativi di B: '{}' / '{}' / '{}'",
                exU.getMessage(), exR.getMessage(), exP.getMessage());

        assertEquals("Indirizzo non trovato", exU.getMessage());
        assertEquals("Indirizzo non trovato", exR.getMessage());
        assertEquals("Indirizzo non trovato", exP.getMessage());

        // e l'indirizzo di A e' rimasto intatto e predefinito
        List<IndirizzoDTO> listaA = indirizzoS.listIndirizzi(utente.getId());
        assertEquals(1, listaA.size());
        assertEquals("Via Etnea", listaA.get(0).getVia());
        assertTrue(listaA.get(0).getPredefinito());
    }
}