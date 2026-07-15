package com.betacom.mtgbazar.be.scryfall;

import java.time.LocalDate;
 
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
 
/** GET /sets/{code} — solo i campi che ci servono. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScryfallSet(
        String code,
        String name,
        @JsonProperty("set_type")     String setType,
        @JsonProperty("parent_set_code") String parentSetCode,
        @JsonProperty("released_at")  LocalDate releasedAt,
        @JsonProperty("icon_svg_uri") String iconSvgUri,
        @JsonProperty("card_count")   Integer cardCount) {
}