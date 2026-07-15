package com.betacom.mtgbazar.be.scryfall;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScryfallPrices(
        BigDecimal eur,
        @JsonProperty("eur_foil") BigDecimal eurFoil) {
}