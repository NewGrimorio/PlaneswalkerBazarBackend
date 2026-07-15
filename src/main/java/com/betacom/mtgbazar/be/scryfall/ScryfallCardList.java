package com.betacom.mtgbazar.be.scryfall;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
 
/** GET /cards/search — pagina da max 175 carte con link alla successiva. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScryfallCardList(
        List<ScryfallCard> data,
        @JsonProperty("has_more")  Boolean hasMore,
        @JsonProperty("next_page") String nextPage,
        @JsonProperty("total_cards") Integer totalCards) {
}