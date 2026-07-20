package com.betacom.mtgbazar.be.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Risposta di login E di refresh (stesso shape: un solo contratto per
 * il frontend). L'utente viaggia incorporato: risparmia il roundtrip
 * /me subito dopo il login e combacia col flusso Angular esistente
 * (authS.login(utente)).
 * Il refresh token NON e' qui: viaggia SOLO nel cookie HttpOnly.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    @ToString.Exclude          // il token e' una credenziale: mai nei log
    private String accessToken;

    private String tokenType;  // "Bearer"

    private UtenteDTO utente;
}