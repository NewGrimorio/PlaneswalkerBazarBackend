package com.betacom.mtgbazar.be.carrello;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.betacom.mtgbazar.be.dto.users.CarrelloDTO;
import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.dto.users.VoceCarrelloDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.repositories.products.IMagazzinoSKURepository;
import com.betacom.mtgbazar.be.repositories.products.IProdottoRepository;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.VoceCarrelloReq;
import com.betacom.mtgbazar.be.services.interfaces.users.ICarrelloServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;

import lombok.extern.slf4j.Slf4j;
 
/**
 * Test di CarrelloImpl su H2 (Flyway V1+V2 dal contesto).
 * Le fixture di catalogo (prodotto ACCESSORIO + SKU) sono create via
 * repository: i service product non esistono ancora. Prefisso crt.
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc   //Messa nei service perche' i primi controller la implementano
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CarrelloServiceTest {
 
    @Autowired private ICarrelloServices carrelloS;
    @Autowired private IUtenteServices utenteS;
    @Autowired private IProdottoRepository prodottoR;
    @Autowired private IMagazzinoSKURepository skuR;
 
    private static final AtomicInteger SEQ = new AtomicInteger();
 
    private UtenteDTO utente;
    private MagazzinoSKU sku;          // giacenza 5, prezzo 10.00
 
    @BeforeEach
    public void setUp() {
        int n = SEQ.incrementAndGet();
 
        UtenteReq req = new UtenteReq();
        req.setEmail("crt" + n + "@test.it");
        req.setPassword("passwordSicura1");
        req.setNome("Clara");
        req.setCognome("Neri");
        req.setDataNascita(LocalDate.of(1992, 7, 1));
        utente = utenteS.registraUtente(req);
 
        // Fixture di catalogo: un ACCESSORIO (nessuna stampa richiesta)
        Prodotto p = new Prodotto();
        p.setTipoProdotto(TipoProdotto.ACCESSORIO);
        p.setNome("Bustine protettive Dragon Shield " + n);
        p.setSlug("bustine-dragon-shield-" + n);
        prodottoR.save(p);
 
        sku = new MagazzinoSKU();
        sku.setProdotto(p);
        sku.setPrezzo(new BigDecimal("10.00"));
        sku.setQuantita(5);
        skuR.save(sku);
 
        log.debug("setUp: utente={} prodotto={} sku={} (giacenza 5, prezzo 10.00)",
                utente.getId(), p.getId(), sku.getId());
    }
 
    private VoceCarrelloReq buildReq(Long skuId, int quantita) {
        VoceCarrelloReq req = new VoceCarrelloReq();
        req.setUtenteId(utente.getId());
        req.setSkuId(skuId);
        req.setQuantita(quantita);
        return req;
    }
 
    // ------------------------------------------------------------------
    // CREAZIONE PIGRA E TOTALI
    // ------------------------------------------------------------------
 
    @Test
    @Order(1)
    public void getCarrelloCreaPigramenteUnCarrelloVuoto() {
        log.debug("TEST 1: primo accesso, carrello creato al volo e vuoto");
 
        CarrelloDTO dto = carrelloS.getCarrello(utente.getId());
        log.debug("carrello: id={} voci={} totale={}", dto.getId(),
                dto.getVoci().size(), dto.getTotale());
 
        assertEquals(0, dto.getVoci().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getTotale()));  // 0, mai null
        assertEquals(0, dto.getNumeroArticoli());
    }
 
    @Test
    @Order(2)
    public void addVoceCalcolaITotaliEIlSecondoAddIncrementa() {
        log.debug("TEST 2: add 2 pezzi (tot 20.00), poi add 1 -> quantita 3 (tot 30.00)");
 
        CarrelloDTO dto = carrelloS.addVoce(buildReq(sku.getId(), 2));
        log.debug("dopo primo add: voci={} totale={}", dto.getVoci().size(), dto.getTotale());
 
        assertEquals(1, dto.getVoci().size());
        VoceCarrelloDTO voce = dto.getVoci().get(0);
        assertEquals(2, voce.getQuantita());
        assertEquals(0, new BigDecimal("20.00").compareTo(voce.getSubtotale()));
        assertEquals(0, new BigDecimal("20.00").compareTo(dto.getTotale()));
        assertTrue(voce.getDisponibile());
 
        dto = carrelloS.addVoce(buildReq(sku.getId(), 1));   // stessa variante
        log.debug("dopo secondo add: quantita={} totale={}",
                dto.getVoci().get(0).getQuantita(), dto.getTotale());
 
        assertEquals(1, dto.getVoci().size());               // sempre UNA voce
        assertEquals(3, dto.getVoci().get(0).getQuantita()); // incrementata
        assertEquals(0, new BigDecimal("30.00").compareTo(dto.getTotale()));
        assertEquals(3, dto.getNumeroArticoli());
    }
 
    @Test
    @Order(3)
    public void addOltreLaGiacenzaRifiutato() {
        log.debug("TEST 3: 3 in carrello + add 3 su giacenza 5 -> rifiuto");
 
        carrelloS.addVoce(buildReq(sku.getId(), 3));
 
        MtgException ex = assertThrows(MtgException.class,
                () -> carrelloS.addVoce(buildReq(sku.getId(), 3)));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Quantita' richiesta non disponibile", ex.getMessage());
 
        // e il carrello e' rimasto com'era
        assertEquals(3, carrelloS.getCarrello(utente.getId()).getNumeroArticoli());
    }
 
    // ------------------------------------------------------------------
    // UPDATE E REMOVE
    // ------------------------------------------------------------------
 
    @Test
    @Order(4)
    public void updateVoceImpostaLaQuantitaEsatta() {
        log.debug("TEST 4: update a quantita esatta, poi oltre giacenza, poi voce inesistente");
 
        carrelloS.addVoce(buildReq(sku.getId(), 3));
 
        CarrelloDTO dto = carrelloS.updateVoce(buildReq(sku.getId(), 1));  // 3 -> 1
        log.debug("dopo update: quantita={}", dto.getVoci().get(0).getQuantita());
        assertEquals(1, dto.getVoci().get(0).getQuantita());               // esatta, non somma
 
        MtgException exOltre = assertThrows(MtgException.class,
                () -> carrelloS.updateVoce(buildReq(sku.getId(), 6)));     // > 5
        assertEquals("Quantita' richiesta non disponibile", exOltre.getMessage());
 
        MtgException exManca = assertThrows(MtgException.class,
                () -> carrelloS.updateVoce(buildReq(999999L, 1)));
        assertEquals("Articolo non presente nel carrello", exManca.getMessage());
    }
 
    @Test
    @Order(5)
    public void removeVoceConOwnershipCheck() {
        log.debug("TEST 5: rimozione propria ok; la voce di A per B 'non esiste'");
 
        CarrelloDTO dto = carrelloS.addVoce(buildReq(sku.getId(), 2));
        Long voceId = dto.getVoci().get(0).getId();
 
        // utente B
        int n = SEQ.incrementAndGet();
        UtenteReq reqB = new UtenteReq();
        reqB.setEmail("crt" + n + "b@test.it");
        reqB.setPassword("passwordSicura1");
        reqB.setNome("Bruno");
        reqB.setCognome("Bianchi");
        UtenteDTO b = utenteS.registraUtente(reqB);
 
        MtgException ex = assertThrows(MtgException.class,
                () -> carrelloS.removeVoce(b.getId(), voceId));
        log.debug("tentativo di B: {}", ex.getMessage());
        assertEquals("Articolo non presente nel carrello", ex.getMessage());
 
        // A rimuove la sua: carrello vuoto
        dto = carrelloS.removeVoce(utente.getId(), voceId);
        assertEquals(0, dto.getVoci().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getTotale()));
    }
 
    @Test
    @Order(6)
    public void clearCarrelloSvuotaTutto() {
        log.debug("TEST 6: clear con due voci dentro");
 
        // seconda variante dello stesso prodotto (lingua diversa)
        MagazzinoSKU sku2 = new MagazzinoSKU();
        sku2.setProdotto(sku.getProdotto());
        sku2.setLingua("de");
        sku2.setPrezzo(new BigDecimal("8.00"));
        sku2.setQuantita(10);
        skuR.save(sku2);
 
        carrelloS.addVoce(buildReq(sku.getId(), 2));
        carrelloS.addVoce(buildReq(sku2.getId(), 1));
        assertEquals(2, carrelloS.getCarrello(utente.getId()).getVoci().size());
 
        carrelloS.clearCarrello(utente.getId());
 
        CarrelloDTO dto = carrelloS.getCarrello(utente.getId());
        log.debug("dopo clear: voci={} totale={}", dto.getVoci().size(), dto.getTotale());
        assertEquals(0, dto.getVoci().size());
        assertEquals(0, dto.getNumeroArticoli());
    }
 
    // ------------------------------------------------------------------
    // VETRINA: LA GIACENZA PUO' CAMBIARE SOTTO I PIEDI
    // ------------------------------------------------------------------
 
    @Test
    @Order(7)
    public void giacenzaScesaSottoIlCarrelloAccendeIlFlagNonDisponibile() {
        log.debug("TEST 7: 4 in carrello, la giacenza scende a 2 (acquisto altrui) -> disponibile=false");
 
        carrelloS.addVoce(buildReq(sku.getId(), 4));
 
        // simulo l'acquisto di un altro utente: giacenza 5 -> 2
        MagazzinoSKU vivo = skuR.findById(sku.getId()).orElseThrow();
        vivo.setQuantita(2);
        skuR.save(vivo);
 
        CarrelloDTO dto = carrelloS.getCarrello(utente.getId());
        VoceCarrelloDTO voce = dto.getVoci().get(0);
        log.debug("voce: quantita={} disponibile={}", voce.getQuantita(), voce.getDisponibile());
 
        // il carrello NON prenota: la voce resta, ma il flag avvisa
        assertEquals(4, voce.getQuantita());
        assertFalse(voce.getDisponibile());
    }
    
}
 