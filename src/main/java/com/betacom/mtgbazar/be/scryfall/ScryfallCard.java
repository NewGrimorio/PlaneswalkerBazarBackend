package com.betacom.mtgbazar.be.scryfall;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Una STAMPA nel modello Scryfall (con dentro i dati oracle).
 * Il nostro sync la smonta nei tre livelli: Carta / Stampa / Prodotto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScryfallCard(
        UUID id,                                            // -> stampa.scryfallId
        @JsonProperty("oracle_id") UUID oracleId,           // -> carta.oracleId
        String name,
        String lang,
        @JsonProperty("mana_cost")   String manaCost,
        BigDecimal cmc,
        @JsonProperty("type_line")   String typeLine,
        @JsonProperty("oracle_text") String oracleText,
        String power,
        String toughness,
        List<String> colors,
        @JsonProperty("color_identity") List<String> colorIdentity,
        List<String> keywords,
        Map<String, String> legalities,
        @JsonProperty("collector_number") String collectorNumber,
        String rarity,
        String artist,
        Boolean promo,
        List<String> finishes,                              // nonfoil/foil/etched
        @JsonProperty("frame_effects") List<String> frameEffects,
        @JsonProperty("promo_types")   List<String> promoTypes,
        @JsonProperty("image_uris")    ScryfallImageUris imageUris,
        @JsonProperty("card_faces")    List<ScryfallCardFace> cardFaces,
        @JsonProperty("multiverse_ids") List<Integer> multiverseIds,
        @JsonProperty("cardmarket_id")  Integer cardmarketId,
        ScryfallPrices prices) {

    /** Immagine: dal fronte, o dalla prima faccia per le bifronte. */
    public String immagine() {
        if (imageUris != null && imageUris.normal() != null)
            return imageUris.normal();
        if (cardFaces != null && !cardFaces.isEmpty()
                && cardFaces.get(0).imageUris() != null)
            return cardFaces.get(0).imageUris().normal();
        return null;
    }
    
}