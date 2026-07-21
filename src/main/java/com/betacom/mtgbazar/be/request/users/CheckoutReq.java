package com.betacom.mtgbazar.be.request.users;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Checkout: volutamente minimale. Il client indica SOLO chi compra e
 * dove spedire; righe, prezzi, totale e addebito sono calcolati dal
 * service dai dati vivi (carrello + SKU + portafoglio, con lock).
 * L'indirizzo viene verificato (ownership + attivo) e SNAPSHOTTATO
 * nell'ordine: da quel momento l'ordine non dipende piu' dalla rubrica.
 */
@Getter
@Setter
@ToString
public class CheckoutReq {

    
    private Long utenteId;

    @NotNull(message = "ordine.no.indirizzo")
    private Long indirizzoId;
}