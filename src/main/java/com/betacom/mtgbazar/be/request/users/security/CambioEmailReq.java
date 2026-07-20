package com.betacom.mtgbazar.be.request.users.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Operazione sensibile: richiede la password corrente per conferma. */
@Getter
@Setter
@ToString
public class CambioEmailReq {

    /** FASE C: valorizzato dal controller (token), mai dal client. */
    private Long utenteId;

    @NotBlank(message = "utente.no.email")
    @Email(message = "utente.email.invalid")
    private String nuovaEmail;

    @NotBlank(message = "utente.no.pwd")
    @ToString.Exclude
    private String password;
}