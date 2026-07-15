package com.betacom.mtgbazar.be.model.users.enums;

/**
 * State machine dell'ordine (stessi 8 stati della Fase 2):
 * CREATO -> SPEDITO -> CONSEGNATO / NON_CONSEGNATO
 * CREATO -> ANNULLATO (cliente) / CANCELLATO (admin)
 * CONSEGNATO -> RESO_RICHIESTO -> RIMBORSATO
 */
public enum StatOrdine {
    CREATO,
    SPEDITO,
    CONSEGNATO,
    NON_CONSEGNATO,
    ANNULLATO,
    CANCELLATO,
    RESO_RICHIESTO,
    RIMBORSATO
}