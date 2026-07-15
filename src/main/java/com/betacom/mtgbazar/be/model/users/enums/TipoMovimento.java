package com.betacom.mtgbazar.be.model.users.enums;

/**
 * Tipo di movimento sul ledger del portafoglio.
 * L'importo è sempre positivo: il segno lo determina il tipo
 * (RICARICA/RIMBORSO accreditano, PRELIEVO/PAGAMENTO_ORDINE addebitano,
 * RETTIFICA è la correzione manuale admin).
 */
public enum TipoMovimento {
    RICARICA,
    PRELIEVO,
    PAGAMENTO_ORDINE,
    RIMBORSO,
    RETTIFICA
}