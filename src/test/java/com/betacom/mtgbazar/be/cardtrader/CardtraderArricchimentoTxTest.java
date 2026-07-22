package com.betacom.mtgbazar.be.cardtrader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.betacom.mtgbazar.be.model.products.Carta;
import com.betacom.mtgbazar.be.model.products.Espansione;
import com.betacom.mtgbazar.be.model.products.Stampa;
import com.betacom.mtgbazar.be.model.products.enums.Rarita;
import com.betacom.mtgbazar.be.repositories.products.ICartaRepository;
import com.betacom.mtgbazar.be.repositories.products.IEspansioneRepository;
import com.betacom.mtgbazar.be.repositories.products.IStampaRepository;
import com.betacom.mtgbazar.be.services.implementations.products.CardtraderArricchimentoTx;

import lombok.extern.slf4j.Slf4j;

/**
 * Collaudo dell'aggancio blueprint->stampa (parte pura, senza HTTP).
 * Il collaboratore transazionale riceve blueprint gia' pronti: qui
 * verifichiamo match sullo scryfall_id, dedup, e i due contatori.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class CardtraderArricchimentoTxTest {

    @Autowired private CardtraderArricchimentoTx arricchimentoTx;
    @Autowired private IEspansioneRepository espansioneR;
    @Autowired private ICartaRepository cartaR;
    @Autowired private IStampaRepository stampaR;

    private static final String COD = "cttest";

    /** Crea una stampa con un dato scryfall_id, agganciata a carta/espansione fresche. */
    private Stampa creaStampa(String numero, UUID scryfallId) {
        Espansione e = espansioneR.findByCodice(COD).orElseGet(() -> {
            Espansione nuova = new Espansione();
            nuova.setCodice(COD);
            nuova.setNome("Cardtrader Test Set");
            nuova.setTipoSet("expansion");
            return espansioneR.save(nuova);
        });

        Carta c = new Carta();
        c.setOracleId(UUID.randomUUID());
        c.setNome("Carta " + numero);
        cartaR.save(c);

        Stampa s = new Stampa();
        s.setCarta(c);
        s.setEspansione(e);
        s.setNumeroCollezione(numero);
        s.setRarita(Rarita.COMMON);
        s.setScryfallId(scryfallId);
        return stampaR.save(s);
    }

    private CardtraderBlueprint blueprint(int id, UUID scryfallId) {
        return new CardtraderBlueprint(id, "BP " + id, 1, 1, 8,
                scryfallId == null ? null : scryfallId.toString(), List.of());
    }

    @Test
    @Order(1)
    public void agganciaSoloLeStampeConScryfallCorrispondente() {
        log.debug("TEST 1: match esatto sullo scryfall_id + un blueprint senza corrispondenza");
        UUID sfA = UUID.randomUUID();
        UUID sfB = UUID.randomUUID();
        Stampa a = creaStampa("101", sfA);
        Stampa b = creaStampa("102", sfB);

        // Tre blueprint: due che matchano, uno per una carta che non abbiamo
        List<CardtraderBlueprint> bps = List.of(
                blueprint(1001, sfA),
                blueprint(1002, sfB),
                blueprint(1003, UUID.randomUUID()));   // nessuna nostra stampa

        var esito = arricchimentoTx.arricchisci(bps);

        assertEquals(2, esito.aggiornate());
        assertEquals(1, esito.senzaCorrispondenza());
        assertEquals(1001, stampaR.findById(a.getId()).orElseThrow().getCardtraderBlueprintId());
        assertEquals(1002, stampaR.findById(b.getId()).orElseThrow().getCardtraderBlueprintId());
    }

    @Test
    @Order(2)
    public void blueprintConScryfallMalformatoIgnorato() {
        log.debug("TEST 2: scryfall_id nullo o non-UUID -> saltato senza esplodere");
        UUID sf = UUID.randomUUID();
        Stampa s = creaStampa("201", sf);

        var bp = new CardtraderBlueprint(2001, "rotto", 1, 1, 8, "non-un-uuid", List.of());
        var esito = arricchimentoTx.arricchisci(List.of(bp, blueprint(2002, sf)));

        assertEquals(1, esito.aggiornate());
        assertEquals(2002, stampaR.findById(s.getId()).orElseThrow().getCardtraderBlueprintId());
    }

    @Test
    @Order(3)
    public void stampaSenzaBlueprintRestaNull() {
        log.debug("TEST 3: se nessun blueprint matcha, il campo resta null");
        Stampa s = creaStampa("301", UUID.randomUUID());

        var esito = arricchimentoTx.arricchisci(List.of(blueprint(3001, UUID.randomUUID())));

        assertEquals(0, esito.aggiornate());
        assertNull(stampaR.findById(s.getId()).orElseThrow().getCardtraderBlueprintId());
    }
}