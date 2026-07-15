package com.betacom.mtgbazar.be.dto.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
 
/** Il rapportino che l'admin vede a fine sincronizzazione. */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SincronizzazioneDTO {
    private String codiceSet;
    private String nomeSet;
    private Integer totaleStampe;       // dichiarate da Scryfall
    private Integer carteNuove;
    private Integer carteAggiornate;
    private Integer stampeNuove;
    private Integer stampeAggiornate;
    private Integer prodottiCreati;
    private Long durataMs;
}