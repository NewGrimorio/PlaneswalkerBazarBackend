package com.betacom.mtgbazar.be.services.interfaces.products;

import com.betacom.mtgbazar.be.dto.products.SincronizzazioneDTO;

/**
 * Il "telecomando" del catalogo: importa/aggiorna un intero set da
 * Scryfall. IDEMPOTENTE: rilanciarlo aggiorna i dati esistenti
 * (upsert via oracleId/scryfallId), non duplica mai.
 */
public interface ISincronizzazioneServices {
 
    /**
     * Sincronizza il set col codice dato (es. "mh3"):
     * espansione -> per ogni stampa: carta oracle (upsert) + stampa
     * (upsert) + prodotto SINGLE (solo alla prima importazione)
     * + rilevazione prezzo di riferimento EUR.
     */
    SincronizzazioneDTO sincronizzaSet(String codiceSet);
}
 