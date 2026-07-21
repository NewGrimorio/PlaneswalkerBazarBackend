package com.betacom.mtgbazar.be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * I contatori della dashboard admin, tutti da COUNT (mai liste in
 * memoria). Tre azionabili (portano a una coda di lavoro) e uno
 * informativo (recensioni pubblicate).
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private long ordiniDaSpedire;       // ordini CREATO
    private long bonificiInAttesa;      // movimenti IN_ATTESA
    private long skuSottoScorta;        // SKU attivi con giacenza <= soglia
    private long recensioniPubblicate;  // recensioni APPROVATE (informativo)
}