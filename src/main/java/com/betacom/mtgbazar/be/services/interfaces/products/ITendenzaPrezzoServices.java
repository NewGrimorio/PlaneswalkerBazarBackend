package com.betacom.mtgbazar.be.services.interfaces.products;

import com.betacom.mtgbazar.be.dto.products.TendenzaPrezzoCartaDTO;

/**
 * Tendenze prezzo in tempo reale di una carta (stampa) e dei suoi SKU.
 * Cardtrader è per-variante (blueprint + finitura/lingua server-side,
 * condizione filtrata sulle offerte); Cardmarket è per-finitura (via
 * Scryfall). Ogni rilevazione viene storicizzata in prezzo_riferimento.
 */
public interface ITendenzaPrezzoServices {

    TendenzaPrezzoCartaDTO tendenzeCarta(Long stampaId);
}