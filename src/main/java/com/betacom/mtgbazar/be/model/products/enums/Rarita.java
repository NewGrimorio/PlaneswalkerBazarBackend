package com.betacom.mtgbazar.be.model.products.enums;


/**
* Rarità di una stampa. I valori Scryfall arrivano in minuscolo
* ("mythic"): convertire nel sync con Rarita.valueOf(s.toUpperCase()).
*/
public enum Rarita {
   COMMON,
   UNCOMMON,
   RARE,
   MYTHIC,
   SPECIAL,
   BONUS
}