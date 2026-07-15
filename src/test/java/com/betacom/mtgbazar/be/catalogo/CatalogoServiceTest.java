package com.betacom.mtgbazar.be.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.betacom.mtgbazar.be.dto.products.EspansioneDTO;
import com.betacom.mtgbazar.be.dto.products.MagazzinoSKUDTO;
import com.betacom.mtgbazar.be.dto.products.ProdottoDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.model.products.enums.Condizione;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.request.products.EspansioneReq;
import com.betacom.mtgbazar.be.request.products.MagazzinoSKUReq;
import com.betacom.mtgbazar.be.request.products.ProdottoReq;
import com.betacom.mtgbazar.be.services.interfaces.products.IEspansioneServices;
import com.betacom.mtgbazar.be.services.interfaces.products.IMagazzinoSKUServices;
import com.betacom.mtgbazar.be.services.interfaces.products.IProdottoServices;

import lombok.extern.slf4j.Slf4j;
 
/**
 * Test dei tre service del catalogo su H2 (Flyway V1+V2+V3 dal
 * contesto). PRIMO test senza utenti: il catalogo vive di vita propria.
 * Codici set e nomi prodotto univoci via SEQ (prefisso ts/cat).
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc   //Messa nei service perche' i primi controller la implementano
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CatalogoServiceTest {
 
    @Autowired private IEspansioneServices espansioneS;
    @Autowired private IProdottoServices prodottoS;
    @Autowired private IMagazzinoSKUServices skuS;
 
    private static final AtomicInteger SEQ = new AtomicInteger();
 
    // ------------------------------------------------------------------
    // Helper fixture
    // ------------------------------------------------------------------
 
    private EspansioneReq buildEspansioneReq(String codice, LocalDate uscita) {
        EspansioneReq req = new EspansioneReq();
        req.setCodice(codice);
        req.setNome("Set di prova " + codice);
        req.setTipoSet("expansion");
        req.setDataUscita(uscita);
        return req;
    }
 
    private ProdottoReq buildProdottoReq(String nome, TipoProdotto tipo) {
        ProdottoReq req = new ProdottoReq();
        req.setTipoProdotto(tipo);
        req.setNome(nome);
        return req;
    }
 
    private MagazzinoSKUReq buildSkuReq(Long prodottoId, String prezzo, int quantita) {
        MagazzinoSKUReq req = new MagazzinoSKUReq();
        req.setProdottoId(prodottoId);
        req.setPrezzo(new BigDecimal(prezzo));
        req.setQuantita(quantita);
        return req;
    }
 
    // ------------------------------------------------------------------
    // ESPANSIONI
    // ------------------------------------------------------------------
 
    @Test
    @Order(1)
    public void createEspansioneNormalizzaIlCodiceERifiutaIDuplicati() {
        int n = SEQ.incrementAndGet();
        log.debug("TEST 1: codice 'TS{}' maiuscolo -> salvato minuscolo; doppione rifiutato", n);
 
        EspansioneDTO dto = espansioneS.createEspansione(
                buildEspansioneReq("TS" + n, LocalDate.of(2026, 1, 1)));
        log.debug("creata: id={} codice={}", dto.getId(), dto.getCodice());
        assertEquals("ts" + n, dto.getCodice());              // normalizzato
 
        // stesso codice, case diverso -> duplicato
        MtgException ex = assertThrows(MtgException.class,
                () -> espansioneS.createEspansione(
                        buildEspansioneReq("Ts" + n, LocalDate.of(2026, 2, 1))));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Codice set gia' esistente", ex.getMessage());
 
        // e il lookup per codice funziona anche "sporco"
        assertEquals(dto.getId(), espansioneS.getByCodice("  TS" + n + " ").getId());
    }
 
    @Test
    @Order(2)
    public void updateEspansioneNullSafeEIlCodiceEImmutabile() {
        int n = SEQ.incrementAndGet();
        log.debug("TEST 2: update del nome; il codice non si tocca");
 
        EspansioneDTO creata = espansioneS.createEspansione(
                buildEspansioneReq("ts" + n, LocalDate.of(2026, 3, 1)));
 
        EspansioneReq update = new EspansioneReq();
        update.setId(creata.getId());
        update.setNome("Nome corretto");
        update.setCodice("HACK");                             // deve essere IGNORATO
 
        EspansioneDTO dopo = espansioneS.updateEspansione(update);
        log.debug("dopo update: nome={} codice={}", dopo.getNome(), dopo.getCodice());
        assertEquals("Nome corretto", dopo.getNome());
        assertEquals("ts" + n, dopo.getCodice());             // immutabile
        assertEquals("expansion", dopo.getTipoSet());         // null-safe: intatto
    }
 
    // ------------------------------------------------------------------
    // PRODOTTI
    // ------------------------------------------------------------------
 
    @Test
    @Order(3)
    public void createProdottoGeneraLoSlugEGestisceLeCollisioni() {
        int n = SEQ.incrementAndGet();
        log.debug("TEST 3: slug generato dal nome; collisione -> suffisso -2");
 
        ProdottoDTO primo = prodottoS.createProdotto(
                buildProdottoReq("Bundle Gift Edition! N." + n, TipoProdotto.BOOSTER_BOX));
        log.debug("primo: slug={}", primo.getSlug());
        assertEquals("bundle-gift-edition-n-" + n, primo.getSlug());
 
        // stesso nome -> lo slug NON collide: arriva il suffisso
        ProdottoDTO secondo = prodottoS.createProdotto(
                buildProdottoReq("Bundle Gift Edition! N." + n, TipoProdotto.BOOSTER_BOX));
        log.debug("secondo: slug={}", secondo.getSlug());
        assertEquals("bundle-gift-edition-n-" + n + "-2", secondo.getSlug());
        assertNotEquals(primo.getId(), secondo.getId());
    }
 
    @Test
    @Order(4)
    public void singleNonSiCreaDalFormEIlTipoEImmutabile() {
        int n = SEQ.incrementAndGet();
        log.debug("TEST 4: SINGLE dal form rifiutato; cambio tipo in update rifiutato");
 
        MtgException exCreate = assertThrows(MtgException.class,
                () -> prodottoS.createProdotto(
                        buildProdottoReq("Carta furbetta " + n, TipoProdotto.SINGLE)));
        assertEquals("Il tipo di prodotto non e' modificabile", exCreate.getMessage());
 
        ProdottoDTO box = prodottoS.createProdotto(
                buildProdottoReq("Box regolare " + n, TipoProdotto.BOOSTER_BOX));
 
        ProdottoReq update = new ProdottoReq();
        update.setId(box.getId());
        update.setTipoProdotto(TipoProdotto.ACCESSORIO);      // cambio natura: no
        MtgException exUpdate = assertThrows(MtgException.class,
                () -> prodottoS.updateProdotto(update));
        log.debug("eccezione attesa: {}", exUpdate.getMessage());
        assertEquals("Il tipo di prodotto non e' modificabile", exUpdate.getMessage());
    }
 
    @Test
    @Order(5)
    public void prodottoDisattivatoInvisibileAlPubblico() {
        int n = SEQ.incrementAndGet();
        log.debug("TEST 5: attivo=false -> getBySlug e liste pubbliche non lo vedono");
 
        ProdottoDTO p = prodottoS.createProdotto(
                buildProdottoReq("Playmat ritirato " + n, TipoProdotto.ACCESSORIO));
        assertEquals(p.getId(), prodottoS.getBySlug(p.getSlug()).getId());   // visibile
 
        ProdottoReq spegni = new ProdottoReq();
        spegni.setId(p.getId());
        spegni.setAttivo(Boolean.FALSE);
        prodottoS.updateProdotto(spegni);
 
        MtgException ex = assertThrows(MtgException.class,
                () -> prodottoS.getBySlug(p.getSlug()));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Prodotto non trovato", ex.getMessage());
 
        boolean inLista = prodottoS.listByTipo(TipoProdotto.ACCESSORIO).stream()
                .anyMatch(x -> x.getId().equals(p.getId()));
        assertFalse(inLista);                                 // sparito dalla vetrina
    }
 
    @Test
    @Order(6)
    public void searchByNomeCaseInsensitive() {
        int n = SEQ.incrementAndGet();
        log.debug("TEST 6: ricerca 'dRAgoN' trova 'Dragon Shield'");
 
        prodottoS.createProdotto(
                buildProdottoReq("Dragon Shield Matte " + n, TipoProdotto.ACCESSORIO));
 
        List<ProdottoDTO> trovati = prodottoS.searchByNome("dRAgoN shield matte " + n);
        log.debug("trovati: {}", trovati.size());
        assertEquals(1, trovati.size());
    }
 
    // ------------------------------------------------------------------
    // MAGAZZINO SKU
    // ------------------------------------------------------------------
 
    @Test
    @Order(7)
    public void skuConDefaultVarianteDuplicataEUpdatePrezzi() {
        int n = SEQ.incrementAndGet();
        log.debug("TEST 7: default NA/en/NONFOIL; variante duplicata; update prezzo/giacenza");
 
        ProdottoDTO p = prodottoS.createProdotto(
                buildProdottoReq("Deckbox pro " + n, TipoProdotto.ACCESSORIO));
 
        // creazione con soli prezzo/quantita: la variante prende i default
        MagazzinoSKUDTO sku = skuS.createSku(buildSkuReq(p.getId(), "12.50", 10));
        log.debug("sku: {}/{}/{} prezzo={} disponibile={}", sku.getCondizione(),
                sku.getLingua(), sku.getFinitura(), sku.getPrezzo(), sku.getDisponibile());
        assertEquals("NA", sku.getCondizione());
        assertEquals("en", sku.getLingua());
        assertEquals("NONFOIL", sku.getFinitura());
        assertTrue(sku.getDisponibile());
 
        // stessa variante (anche con default espliciti) -> duplicata
        MagazzinoSKUReq doppione = buildSkuReq(p.getId(), "9.99", 5);
        doppione.setCondizione(Condizione.NA);
        doppione.setLingua("EN");                             // case diverso: normalizzata
        doppione.setFinitura(Finitura.NONFOIL);
        MtgException ex = assertThrows(MtgException.class, () -> skuS.createSku(doppione));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Variante gia' esistente per questo prodotto", ex.getMessage());
 
        // lingua diversa -> variante legittima
        MagazzinoSKUReq tedesco = buildSkuReq(p.getId(), "11.00", 3);
        tedesco.setLingua("de");
        skuS.createSku(tedesco);
        assertEquals(2, skuS.listByProdotto(p.getId()).size());
 
        // update: prezzo e giacenza a zero -> non disponibile
        MagazzinoSKUReq update = new MagazzinoSKUReq();
        update.setId(sku.getId());
        update.setPrezzo(new BigDecimal("14.00"));
        update.setQuantita(0);
        MagazzinoSKUDTO dopo = skuS.updateSku(update);
        log.debug("dopo update: prezzo={} quantita={} disponibile={}",
                dopo.getPrezzo(), dopo.getQuantita(), dopo.getDisponibile());
        assertEquals(0, new BigDecimal("14.00").compareTo(dopo.getPrezzo()));
        assertFalse(dopo.getDisponibile());
        
    }
}
 