package com.betacom.mtgbazar.be.request.users;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Azione ADMIN sui movimenti IN_ATTESA (bonifici in entrata da
 * confermare, prelievi da eseguire): approva o rifiuta.
 * Solo alla conferma il saldo viene effettivamente accreditato
 * (ricariche) — il service usa il lock sul portafoglio.
 */
@Getter
@Setter
@ToString
public class ConfermaMovimentoReq {

    @NotNull(message = "movimento.no.id")
    private Long movimentoId;

    @NotNull(message = "movimento.no.esito")
    private Boolean approvato;

    @Size(max = 300, message = "movimento.nota.maxlength")
    private String nota;
}