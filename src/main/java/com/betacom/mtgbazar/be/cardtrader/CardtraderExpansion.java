package com.betacom.mtgbazar.be.cardtrader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardtraderExpansion(
        Integer id,
        @JsonProperty("game_id") Integer gameId,
        String code,
        String name
) {
	
}