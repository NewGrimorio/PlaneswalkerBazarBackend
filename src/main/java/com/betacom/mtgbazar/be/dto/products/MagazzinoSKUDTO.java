package com.betacom.mtgbazar.be.dto.products;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** La variante acquistabile mostrata nella pagina prodotto. */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagazzinoSKUDTO {
    private Long id;
    private Long prodottoId;
    private String condizione;
    private String lingua;
    private String finitura;
    private BigDecimal prezzo;
    private Integer quantita;
    private Boolean attivo;
    private Boolean disponibile;        // quantita > 0 && attivo
}