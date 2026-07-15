package com.betacom.mtgbazar.be.dto.products;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Livello ORACLE per la pagina carta. legal viaggia come JSON grezzo:
 * lo interpreta il frontend (o un giorno un endpoint dedicato).
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartaDTO {
	
    private Long id;
    private String nome;
    private String costoMana;
    private BigDecimal valoreMana;
    private String tipoRiga;
    private String testoOracle;
    private String forza;
    private String costituzione;
    private String colori;              // sottoinsieme di "WUBRG"
    private String identitaColore;
    private String paroleChiave;        // CSV
    private String legal;               // JSON {"standard":"legal",...}
    
}