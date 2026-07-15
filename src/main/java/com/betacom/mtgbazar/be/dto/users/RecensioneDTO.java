package com.betacom.mtgbazar.be.dto.users;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Recensione in pagina prodotto. autore = nome visualizzabile
 * (es. "Marco R."), MAI email o id di altri utenti.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecensioneDTO {
    private Long id;
    private Short voto;
    private String titolo;
    private String testo;
    private String stato;
    private String autore;
    private Boolean acquistoVerificato;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
}