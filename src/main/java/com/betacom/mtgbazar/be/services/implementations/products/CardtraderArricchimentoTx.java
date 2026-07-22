package com.betacom.mtgbazar.be.services.implementations.products;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.mtgbazar.be.cardtrader.CardtraderBlueprint;
import com.betacom.mtgbazar.be.model.products.Stampa;
import com.betacom.mtgbazar.be.repositories.products.IStampaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Confine transazionale dell'arricchimento Cardtrader.
 *
 * Esiste come bean SEPARATO — e non come metodo privato dell'orchestratore
 * — di proposito: @Transactional ha effetto solo se il metodo e' invocato
 * ATTRAVERSO il proxy di Spring. Chiamato da un altro bean il proxy c'e';
 * una chiamata interna l'avrebbe scavalcato (auto-invocazione), lasciando
 * l'annotazione senza alcun effetto.
 *
 * Una transazione PER SET: un set che fallisce non rolla indietro quelli
 * gia' agganciati, e il persistence context non si gonfia con tutte le
 * stampe del catalogo in un colpo solo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CardtraderArricchimentoTx {

    private final IStampaRepository stampaR;

    /** Esito dell'arricchimento di un singolo set. */
    public record Esito(int aggiornate, int senzaCorrispondenza) {}

    @Transactional
    public Esito arricchisci(List<CardtraderBlueprint> blueprints) {
        // 1) mappa scryfallId -> blueprintId (dedup: a parita' di scryfall vince l'ultimo)
        Map<UUID, Integer> perScryfall = new HashMap<>();
        for (CardtraderBlueprint bp : blueprints) {
            if (bp.scryfallId() == null) continue;
            try {
                perScryfall.put(UUID.fromString(bp.scryfallId()), bp.id());
            } catch (IllegalArgumentException ignored) {
                // scryfall_id malformato lato Cardtrader: si salta
            }
        }
        if (perScryfall.isEmpty()) return new Esito(0, 0);

        // 2) UNA sola query: le nostre stampe con quegli scryfall_id (anti-N+1)
        List<Stampa> stampe = stampaR.findByScryfallIdIn(perScryfall.keySet());

        // 3) set del blueprint_id sulle entita' managed -> flush al commit
        for (Stampa s : stampe)
            s.setCardtraderBlueprintId(perScryfall.get(s.getScryfallId()));

        int aggiornate = stampe.size();
        int senza = perScryfall.size() - aggiornate;   // blueprint senza una nostra stampa
        return new Esito(aggiornate, senza);
    }
    
    /**
     * Applica una mappa globale scryfall_id -> blueprint_id a tutte le stampe
     * corrispondenti. Il lookup è a blocchi per non passare una IN gigante al
     * DB in un colpo solo.
     */
    @Transactional
    public Esito arricchisciDaMappa(Map<UUID, Integer> perScryfall) {
        if (perScryfall.isEmpty()) return new Esito(0, 0);

        List<UUID> ids = new ArrayList<>(perScryfall.keySet());
        int aggiornate = 0;
        final int BLOCCO = 500;   // evita una IN con migliaia di parametri

        for (int i = 0; i < ids.size(); i += BLOCCO) {
            List<UUID> fetta = ids.subList(i, Math.min(i + BLOCCO, ids.size()));
            List<Stampa> stampe = stampaR.findByScryfallIdIn(fetta);
            for (Stampa s : stampe) {
                s.setCardtraderBlueprintId(perScryfall.get(s.getScryfallId()));
                aggiornate++;
            }
        }

        //int senza = perScryfall.size() - aggiornate;   // blueprint senza una nostra stampa
        long scoperte = stampaR.countByCardtraderBlueprintIdIsNull();
        return new Esito(aggiornate, (int) scoperte);
        
    }
    
}