package com.betacom.mtgbazar.be.dto.users;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * L'indirizzo e' lo SNAPSHOT dell'ordine (colonne sped_*), non un
 * IndirizzoDTO dalla rubrica: quello che vedi e' dove e' stato
 * davvero spedito, anche se l'utente ha cambiato rubrica dopo.
 * Le voci sono presenti nel dettaglio, null nelle liste.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdineDTO {
    private Long id;
    private String stato;
    private BigDecimal totale;
    private BigDecimal speseSpedizione;

    private String spedDestinatario;
    private String spedVia;
    private String spedCivico;
    private String spedCap;
    private String spedCitta;
    private String spedProvincia;
    private String spedNazione;

    private LocalDateTime creationDate;
    private LocalDateTime updateDate;

    private List<VoceOrdineDTO> voci;
}