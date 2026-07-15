package com.betacom.mtgbazar.be.scryfall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScryfallImageUris(String small, String normal, String large) {
}