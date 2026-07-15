package com.betacom.mtgbazar.be.model.products.enums;

/**
 * Condizione della carta per gli SKU di tipo SINGLE.
 * NA = valore sentinella per i prodotti non-single (sigillato, accessori):
 * mai NULL, così il vincolo UNIQUE (prodotto, condizione, lingua, finitura)
 * resta standard e portabile PG/H2. Da nascondere nella UI per i non-single.
 */
public enum Condizione {
    MT, 
    NM, 
    EX, 
    GD,
    LP,
    PL,
    PO,
    NA
}
