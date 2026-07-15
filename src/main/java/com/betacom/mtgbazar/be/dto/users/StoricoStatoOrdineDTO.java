package com.betacom.mtgbazar.be.dto.users;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Una tappa della timeline dell'ordine. */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoricoStatoOrdineDTO {
    private String statoDa;
    private String statoA;
    private String eseguitoDa;     // nome visualizzabile, mai l'id utente altrui
    private String nota;
    private LocalDateTime creationDate;
}