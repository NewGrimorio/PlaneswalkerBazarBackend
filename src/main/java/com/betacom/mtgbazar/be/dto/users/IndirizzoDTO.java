package com.betacom.mtgbazar.be.dto.users;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * "predefinito" e' calcolato dal service confrontando l'id con
 * utente.indirizzoPredefinito: nel DB non esiste piu' come colonna.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndirizzoDTO {
    private Long id;
    private String etichetta;
    private String destinatario;
    private String via;
    private String civico;
    private String cap;
    private String citta;
    private String provincia;
    private String nazione;
    private Boolean predefinito;
}