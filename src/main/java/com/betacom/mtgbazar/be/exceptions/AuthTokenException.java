package com.betacom.mtgbazar.be.exceptions;

/**
 * Fallimento legato ai TOKEN (refresh invalido/scaduto/riusato, /me
 * senza identita'). Sorella di MtgException ma con semantica diversa:
 * MtgException = errore di business -> 400;
 * AuthTokenException = credenziale non valida -> 401.
 * Il client che riceve 401 dal /refresh sa che deve rifare il login.
 * Il messaggio arriva GIA' risolto da messaggi_sistema, come per MtgException.
 */
public class AuthTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AuthTokenException(String messaggio) {
        super(messaggio);
    }
}