package com.betacom.mtgbazar.be.services.implementations.products;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.betacom.mtgbazar.be.cardtrader.CardtraderBlueprint;
import com.betacom.mtgbazar.be.cardtrader.CardtraderExpansion;
import com.betacom.mtgbazar.be.dto.products.CardtraderSyncDTO;
import com.betacom.mtgbazar.be.services.interfaces.products.ICardtraderSyncServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Arricchimento del catalogo con i blueprint_id di Cardtrader — versione
 * a MAPPA GLOBALE.
 *
 * Non aggancia più set-per-set sul codice: scarica i blueprint di TUTTE
 * le espansioni Magic di Cardtrader, costruisce un'unica mappa
 * scryfall_id -> blueprint_id, e la applica a tutte le nostre stampe con
 * un solo lookup a blocchi. Questo recupera anche le varianti (borderless,
 * showcase, promo) che Cardtrader tiene sotto espansioni-extra con un
 * codice diverso dal nostro: il match è sullo scryfall_id, non sul codice.
 *
 * Costo: una chiamata /blueprints/export per ogni espansione Magic
 * (qualche centinaio), dentro il rate limit grazie al throttle. È un job
 * admin occasionale, la lentezza è accettabile.
 *
 * BEST-EFFORT: un'espansione che fallisce il download viene saltata e
 * loggata, il giro prosegue. La transazione di scrittura vive nel
 * collaboratore CardtraderArricchimentoTx (invariato).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardtraderSyncImpl implements ICardtraderSyncServices {

    private static final int  GAME_ID_MAGIC    = 1;
    private static final long PAUSA_TRA_SET_MS = 120;   // rate limit: 200/10s

    private final RestClient cardtraderRestClient;
    private final CardtraderArricchimentoTx arricchimentoTx;

    @Override
    public CardtraderSyncDTO sincronizzaBlueprint() {
        long inizio = System.currentTimeMillis();

        // 1) Le espansioni Magic di Cardtrader (solo per iterarne gli id).
        List<CardtraderExpansion> espansioni = caricaEspansioniCardtrader();
        if (espansioni.isEmpty()) {
            log.warn("Cardtrader: nessuna espansione Magic ottenuta, sync interrotto");
            return esito(0, 0, 0, inizio);
        }

        // 2) Un'unica mappa scryfall_id -> blueprint_id da TUTTE le espansioni.
        //    A parità di scryfall_id vince l'ultimo (dedup varianti/token).
        Map<UUID, Integer> perScryfall = new HashMap<>();
        int setElaborati = 0, blueprintScartati = 0;

        for (CardtraderExpansion exp : espansioni) {
            try {
                List<CardtraderBlueprint> bps = esportaBlueprint(exp.id());
                for (CardtraderBlueprint bp : bps) {
                    if (bp.scryfallId() == null) { blueprintScartati++; continue; }
                    try {
                        perScryfall.put(UUID.fromString(bp.scryfallId()), bp.id());
                    } catch (IllegalArgumentException ignored) {
                        blueprintScartati++;   // scryfall_id malformato lato Cardtrader
                    }
                }
                setElaborati++;
            } catch (Exception e) {
                // best-effort per espansione: una che salta non ferma le altre
                log.warn("Cardtrader: export fallito per espansione {} ({}), proseguo",
                        exp.id(), e.getMessage());
            }
            pausa();
        }

        log.debug("Cardtrader: raccolti {} blueprint utili da {} espansioni ({} scartati)",
                perScryfall.size(), setElaborati, blueprintScartati);

        if (perScryfall.isEmpty())
            return esito(setElaborati, 0, 0, inizio);

        // 3) Un solo passaggio di scrittura: applica la mappa a tutte le stampe.
        //    Il Tx spezza il lavoro e fa il/i lookup findByScryfallIdIn a blocchi.
        CardtraderArricchimentoTx.Esito eu = arricchimentoTx.arricchisciDaMappa(perScryfall);

        long durata = System.currentTimeMillis() - inizio;
        log.info("Cardtrader sync globale: {} espansioni, {} stampe agganciate, "
               + "{} blueprint senza nostra stampa, {}ms",
                setElaborati, eu.aggiornate(), eu.senzaCorrispondenza(), durata);

        return esito(setElaborati, eu.aggiornate(), eu.senzaCorrispondenza(), inizio);
    }

    // ==================================================================
    // CHIAMATE HTTP
    // ==================================================================

    private List<CardtraderExpansion> caricaEspansioniCardtrader() {
        try {
            CardtraderExpansion[] exps = cardtraderRestClient.get()
                    .uri("/expansions")
                    .retrieve()
                    .body(CardtraderExpansion[].class);
            if (exps == null) return List.of();

            List<CardtraderExpansion> magic = new ArrayList<>();
            for (CardtraderExpansion e : exps)
                if (Integer.valueOf(GAME_ID_MAGIC).equals(e.gameId()))
                    magic.add(e);
            return magic;
        } catch (Exception e) {
            log.error("Cardtrader /expansions non raggiungibile: {}", e.getMessage());
            return List.of();
        }
    }

    private List<CardtraderBlueprint> esportaBlueprint(int ctExpId) {
        CardtraderBlueprint[] bps = cardtraderRestClient.get()
                .uri(u -> u.path("/blueprints/export")
                          .queryParam("expansion_id", ctExpId)
                          .build())
                .retrieve()
                .body(CardtraderBlueprint[].class);
        return bps == null ? List.of() : Arrays.asList(bps);
    }

    // ==================================================================
    // Helper
    // ==================================================================

    private CardtraderSyncDTO esito(int set, int agganciate, int senza, long inizio) {
        return CardtraderSyncDTO.builder()
                .espansioniElaborate(set)
                .stampeAggiornate(agganciate)
                .blueprintSenzaCorrispondenza(senza)
                .millisecondiImpiegati(System.currentTimeMillis() - inizio)
                .build();
    }

    private void pausa() {
        try {
            Thread.sleep(PAUSA_TRA_SET_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
}