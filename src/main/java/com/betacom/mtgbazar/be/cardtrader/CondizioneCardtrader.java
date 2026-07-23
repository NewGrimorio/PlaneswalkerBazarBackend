package com.betacom.mtgbazar.be.cardtrader;

import java.util.Set;

import com.betacom.mtgbazar.be.model.products.enums.Condizione;

/**
 * Scala di condizione di Cardtrader (5 gradi), separata dalla nostra
 * Condizione (7 gradi Cardmarket, usata dagli SKU).
 *
 * Ogni valore porta l'etichetta ESATTA usata da Cardtrader nelle properties
 * dei prodotti e l'insieme delle NOSTRE condizioni che vi ricadono. La
 * mappatura è un DATO sull'enum, non uno switch altrove.
 */
public enum CondizioneCardtrader {

    NEAR_MINT("Near Mint",                 Condizione.MT, Condizione.NM),
    SLIGHTLY_PLAYED("Slightly Played",     Condizione.EX),
    MODERATELY_PLAYED("Moderately Played", Condizione.GD, Condizione.LP),
    PLAYED("Played",                       Condizione.PL),
    POOR("Poor",                           Condizione.PO);

    private final String etichetta;
    private final Set<Condizione> corCmrkt; //Corrispondenti alla qualità di Cardmarket
    
    //... rappresenta un varargs. questo metodo accetta un numero qualsiasi di Condizione, da zero in su
    CondizioneCardtrader(String etichetta, Condizione... corCmrkt) {
        this.etichetta = etichetta;
        this.corCmrkt = Set.of(corCmrkt);
    }

    public String etichetta() {
        return etichetta;
    }

    public Set<Condizione> corCmrkt() {
        return corCmrkt;
    }
    
}