package com.betacom.mtgbazar.be.dto.users;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Voce del carrello con il prezzo LIVE dello SKU (non snapshot:
 * nel carrello i prezzi sono sempre quelli correnti del negozio).
 * disponibile segnala al frontend se la quantita' richiesta e'
 * ancora coperta dalla giacenza.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoceCarrelloDTO {
    private Long id;
    private Long skuId;
    private String nomeProdotto;
    private String tipoProdotto;
    private String condizione;
    private String lingua;
    private String finitura;
    private String imageUrl;
    private BigDecimal prezzoUnitario;
    private Integer quantita;
    private BigDecimal subtotale;
    private Boolean disponibile;
}