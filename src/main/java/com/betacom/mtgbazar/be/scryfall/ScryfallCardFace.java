package com.betacom.mtgbazar.be.scryfall;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
 
/** Una faccia delle carte bifronte/split (salvata come JSON in carta). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScryfallCardFace(
        String name,
        @JsonProperty("mana_cost")   String manaCost,
        @JsonProperty("type_line")   String typeLine,
        @JsonProperty("oracle_text") String oracleText,
        String power,
        String toughness,
        List<String> colors,
        @JsonProperty("image_uris")  ScryfallImageUris imageUris) {
}