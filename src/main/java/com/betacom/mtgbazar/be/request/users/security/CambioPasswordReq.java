package com.betacom.mtgbazar.be.request.users.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CambioPasswordReq {

    /** FASE C: valorizzato dal controller (token), mai dal client. */
    private Long utenteId;

    @NotBlank(message = "utente.no.pwd")
    @ToString.Exclude
    private String vecchiaPassword;

    @NotBlank(message = "utente.no.pwd")
    @Size(min = 8, max = 72, message = "utente.pwd.length")
    @ToString.Exclude
    private String nuovaPassword;
}