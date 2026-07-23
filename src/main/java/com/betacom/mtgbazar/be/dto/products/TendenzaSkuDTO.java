package com.betacom.mtgbazar.be.dto.products;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tendenza di un singolo SKU. Cardtrader è per-variante (lowest + market);
 * Cardmarket è per-finitura (stesso valore su tutte le condizioni di quella
 * finitura). Il "precedente" serve al frontend per la freccia ↑/↓.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TendenzaSkuDTO {
    private Long skuId;
    private String condizione;      // scala nostra (NM, EX, ...)
    private String lingua;
    private String finitura;

    private BigDecimal ctLowest;        // Cardtrader: offerta più bassa per la variante
    private BigDecimal ctMarket;        // Cardtrader: media delle prime N offerte
    private BigDecimal ctPrecedente;    // ultimo CT lowest storicizzato prima di ora

    private BigDecimal cardmarket;      // Scryfall eur per la finitura
    private BigDecimal cardmarketPrecedente;

    private String valuta;
}