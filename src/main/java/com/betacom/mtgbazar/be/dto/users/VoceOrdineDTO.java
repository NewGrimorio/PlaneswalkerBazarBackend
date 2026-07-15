package com.betacom.mtgbazar.be.dto.users;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Voce d'ordine: descrizione e prezzo sono gli SNAPSHOT salvati al
 * checkout, non i dati vivi dello SKU — lo storico non cambia mai.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoceOrdineDTO {
    private Long id;
    private Long skuId;
    private String descrizione;
    private BigDecimal prezzoUnitario;
    private Integer quantita;
    private BigDecimal subtotale;
}