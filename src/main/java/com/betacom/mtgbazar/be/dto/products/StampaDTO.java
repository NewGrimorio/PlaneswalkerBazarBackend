package com.betacom.mtgbazar.be.dto.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Una pubblicazione della carta: porta con se' i riferimenti leggibili
 * a carta ed espansione (id + nome), per liste e "altre versioni".
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StampaDTO {
	
    private Long id;
    private Long cartaId;
    private String cartaNome;
    private Long espansioneId;
    private String espansioneCodice;
    private String espansioneNome;
    private String numeroCollezione;
    private String rarita;
    private String artista;
    private Boolean promo;
    private Boolean hasNonFoil;
    private Boolean hasFoil;
    private Boolean hasEtchedFoil;
    private String effettiCornice;      // CSV frame_effects
    private String tipiPromo;           // CSV promo_types
    private String imageUrl;
    
}