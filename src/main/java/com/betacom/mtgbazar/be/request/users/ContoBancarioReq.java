package com.betacom.mtgbazar.be.request.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Conto per il ritiro credito. Niente Update: un IBAN sbagliato si
 *  disattiva e se ne inserisce uno nuovo (il ledger referenzia il vecchio). */
@Getter
@Setter
@ToString
public class ContoBancarioReq {

    @NotNull(message = "conto.no.utente")
    private Long utenteId;

    @NotBlank(message = "conto.no.intestatario")
    @Size(max = 200, message = "conto.intestatario.maxlength")
    private String intestatario;

    @NotBlank(message = "conto.no.iban")
    @Pattern(regexp = "^[A-Za-z]{2}[0-9]{2}[A-Za-z0-9]{10,30}$", message = "conto.iban.invalid")
    private String iban;

    @Pattern(regexp = "^[A-Za-z0-9]{8,11}$", message = "conto.bic.invalid")
    private String bic;
}