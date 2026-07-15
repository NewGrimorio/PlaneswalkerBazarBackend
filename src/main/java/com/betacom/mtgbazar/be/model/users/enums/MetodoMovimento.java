package com.betacom.mtgbazar.be.model.users.enums;

/**
 * PAYPAL:   accredito istantaneo, commissione 5% + 0,35 EUR
 * BONIFICO: gratuito, accredito fino a 5 giorni lavorativi (stato IN_ATTESA)
 * INTERNO:  movimenti generati dal sistema (pagamenti ordine, rimborsi)
 */
public enum MetodoMovimento {
	PAYPAL,
    BONIFICO,
    INTERNO
}
