package com.betacom.mtgbazar.be.dto.products;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Tendenze prezzo di una carta (stampa): una riga per ogni suo SKU. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TendenzaPrezzoCartaDTO {
    private Long stampaId;
    private String nomeCarta;
    private String codiceSet;
    private List<TendenzaSkuDTO> righe;
    private long millisecondiImpiegati;
}