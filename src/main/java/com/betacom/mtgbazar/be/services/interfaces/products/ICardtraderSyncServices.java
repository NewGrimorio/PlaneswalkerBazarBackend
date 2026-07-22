package com.betacom.mtgbazar.be.services.interfaces.products;

import com.betacom.mtgbazar.be.dto.products.CardtraderSyncDTO;

/**
 * Arricchimento del catalogo con i blueprint_id di Cardtrader.
 *
 * NON e' un sync di prima creazione: le stampe le crea gia' Scryfall.
 * Questo service fa SOLO un arricchimento non distruttivo — riempie
 * stampa.cardtrader_blueprint_id dove trova una corrispondenza, e non
 * tocca nient'altro. La casella c'era gia' nello schema (nullable): qui
 * arriva la fonte che la valorizza.
 *
 * GIUNZIONE: il blueprint di Cardtrader espone il proprio scryfall_id.
 * Poiche' stampa.scryfall_id e' UNIQUE, il match e' esatto e 1:1, senza
 * bisogno di allineare a mano espansioni o numeri di collezione.
 *
 * ORDINE DI ESECUZIONE: va lanciato DOPO il sync Scryfall, che e' cio'
 * che popola stampa.scryfall_id — senza quello non ci sarebbe su cosa
 * agganciarsi.
 *
 * IDEMPOTENTE: rilanciarlo non crea duplicati; riscrive lo stesso
 * blueprint_id sulle stampe gia' associate e completa quelle nuove.
 */
public interface ICardtraderSyncServices {

    /**
     * Scarica i blueprint di tutte le espansioni Magic di Cardtrader e
     * imposta il blueprint_id sulle stampe con scryfall_id corrispondente.
     * Il lavoro e' batch per espansione (una sola query di lookup a
     * blocco), per non cadere nell'N+1.
     *
     * @return riepilogo dell'esecuzione (per la pagina sync admin)
     */
    CardtraderSyncDTO sincronizzaBlueprint();
    
}