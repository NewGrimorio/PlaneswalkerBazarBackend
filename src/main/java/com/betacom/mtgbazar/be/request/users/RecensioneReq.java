package com.betacom.mtgbazar.be.request.users;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Recensione post-consegna. Il service verifica il diritto:
 * l'ordine e' dell'utente, e' CONSEGNATO e contiene il prodotto.
 * Vale sia per la creazione sia per la modifica della propria
 * recensione (una sola per utente/prodotto).
 */

/**
 * Recensione post-consegna. Il service verifica il diritto:
 * l'ordine e' dell'utente, e' CONSEGNATO e contiene il prodotto.
 *
 * FASE C: utenteId perde il @NotNull (lo mette il controller dal
 * token). prodottoId e ordineId RESTANO obbligatori: sono dati che
 * il client fornisce davvero (quale prodotto, quale ordine).
 */
@Getter
@Setter
@ToString
public class RecensioneReq {
 
    /** FASE C: valorizzato dal controller dal token, mai dal client. */
    private Long utenteId;
 
    @NotNull(message = "recensione.no.prodotto")
    private Long prodottoId;
 
    @NotNull(message = "recensione.no.ordine")
    private Long ordineId;
 
    @NotNull(message = "recensione.no.voto")
    @Min(value = 1, message = "recensione.voto.range")
    @Max(value = 5, message = "recensione.voto.range")
    private Short voto;
 
    @Size(max = 150, message = "recensione.titolo.maxlength")
    private String titolo;
 
    @Size(max = 4000, message = "recensione.testo.maxlength")
    private String testo;
    
}