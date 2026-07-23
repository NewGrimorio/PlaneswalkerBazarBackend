package com.betacom.mtgbazar.be.cardtrader;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Un'offerta del marketplace Cardtrader (GET /marketplace/products).
 * La risposta è una mappa blueprint_id -> lista di questi prodotti,
 * ordinati per prezzo crescente (i 25 più economici per blueprint).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CardtraderMarketplaceProduct(
        Long id,
        @JsonProperty("blueprint_id") Integer blueprintId,
        Integer quantity,
        Prezzo price,
        @JsonProperty("properties_hash") Proprieta properties) {   // <-- QUI

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Prezzo(Integer cents, String currency) {
        public BigDecimal euro() {
            return cents == null ? null : BigDecimal.valueOf(cents).movePointLeft(2);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Proprieta(
            String condition,
            @JsonProperty("mtg_language") String mtgLanguage,
            @JsonProperty("mtg_foil")     Boolean mtgFoil) {
    }
    
}