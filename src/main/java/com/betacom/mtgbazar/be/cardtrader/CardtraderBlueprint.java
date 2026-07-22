package com.betacom.mtgbazar.be.cardtrader;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardtraderBlueprint(
        Integer id,                                     // -> cardtrader_blueprint_id
        String name,
        @JsonProperty("game_id")      Integer gameId,
        @JsonProperty("category_id")  Integer categoryId,
        @JsonProperty("expansion_id") Integer expansionId,
        @JsonProperty("scryfall_id")  String  scryfallId,   // LA chiave di giunzione
        @JsonProperty("card_market_ids") List<Integer> cardMarketIds
) {
	
}