package com.betacom.mtgbazar.be.dto.users;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un'opzione di spedizione proposta al carrello, col costo GIA' calcolato
 * per l'imponibile corrente. Il client la mostra e basta: non ricalcola
 * nulla, cosi' l'anteprima non puo' divergere dall'addebito.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpzioneSpedizioneDTO {
    private String tipo;                    // STANDARD | EXPRESS
    private String etichetta;
    private String tempi;
    private BigDecimal costo;               // 0 se offerta
    private BigDecimal totaleConSpedizione;
}