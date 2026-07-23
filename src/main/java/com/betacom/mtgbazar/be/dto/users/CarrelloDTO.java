package com.betacom.mtgbazar.be.dto.users;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Il totale lo calcola il service sommando i subtotali: mai il client. */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrelloDTO {
    private Long id;
    private List<VoceCarrelloDTO> voci;
    private BigDecimal totale;
    private Integer numeroArticoli;
    private Boolean spedizioneOfferta;              // sopra soglia
    private BigDecimal mancaPerSpedizioneGratuita;  // 0 se gia' offerta
    private List<OpzioneSpedizioneDTO> opzioniSpedizione;
}