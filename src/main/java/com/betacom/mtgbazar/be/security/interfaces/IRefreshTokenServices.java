package com.betacom.mtgbazar.be.security.interfaces;

import com.betacom.mtgbazar.be.model.users.enums.RuoloUtente;

/**
 * Ciclo di vita dei refresh token OPACHI e persistiti (V11).
 * Il token in chiaro esiste solo nel valore di ritorno di emetti/ruota:
 * da li' finisce nel cookie HttpOnly e non viene mai piu' letto dal
 * server se non come hash.
 *
 * Errori di validazione -> AuthTokenException (401 dal handler):
 * il client che la riceve sul /refresh sa che deve rifare il login.
 */
public interface IRefreshTokenServices {

    /** Esito della rotazione: nuovo token in chiaro + identita' per l'access token. */
    record RotazioneEsito(String nuovoToken, Long utenteId,
                          String username, RuoloUtente ruolo) {}

    /**
     * Emissione al LOGIN: famiglia nuova di zecca.
     * @return il token in chiaro (unica esistenza fuori dal cookie).
     */
    String emetti(Long utenteId, String userAgent);

    /**
     * Rotazione al /refresh: valida, revoca il token usato, ne emette
     * uno nuovo NELLA STESSA famiglia. Il riuso di un token gia'
     * revocato e' il segnale di furto: revoca dell'intera famiglia.
     */
    RotazioneEsito ruota(String tokenChiaro, String userAgent);

    /** Logout: revoca la famiglia del token presentato. Idempotente e silenziosa. */
    void revoca(String tokenChiaro);
}