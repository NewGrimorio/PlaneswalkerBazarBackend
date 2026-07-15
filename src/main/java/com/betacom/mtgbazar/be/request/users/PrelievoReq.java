package com.betacom.mtgbazar.be.request.users;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * "Ritira credito": bonifico in uscita verso un conto dell'utente.
 * Il service verifica ownership del conto, saldo sufficiente (con lock)
 * e crea il movimento PRELIEVO in stato IN_ATTESA.
 */
@Getter
@Setter
@ToString
public class PrelievoReq {

    @NotNull(message = "portafoglio.no.utente")
    private Long utenteId;

    @NotNull(message = "portafoglio.no.importo")
    @DecimalMin(value = "0.01", message = "portafoglio.importo.min")
    @Digits(integer = 10, fraction = 2, message = "portafoglio.importo.invalid")
    private BigDecimal importo;

    @NotNull(message = "portafoglio.no.conto")
    private Long contoBancarioId;
}