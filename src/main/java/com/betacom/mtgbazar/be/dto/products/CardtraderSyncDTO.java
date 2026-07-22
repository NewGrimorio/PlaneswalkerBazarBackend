package com.betacom.mtgbazar.be.dto.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Esito del sync Cardtrader, pensato per essere mostrato nella pagina
 * "Sincronizza" dell'admin.
 *
 * - espansioniElaborate:          quante espansioni Magic sono state scorse
 * - stampeAggiornate:             quante stampe hanno ricevuto il blueprint_id
 * - blueprintSenzaCorrispondenza: blueprint con scryfall_id ma senza una
 *                                 nostra stampa (set non ancora importato,
 *                                 o carta assente) — e' la metrica da
 *                                 guardare per capire la copertura
 * - millisecondiImpiegati:        durata totale, utile con i rate limit
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardtraderSyncDTO {

    private int  espansioniElaborate;
    private int  stampeAggiornate;
    private int  blueprintSenzaCorrispondenza;
    private long millisecondiImpiegati;
}