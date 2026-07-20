package com.betacom.mtgbazar.be.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.betacom.mtgbazar.be.dto.ResponseDTO;
import com.betacom.mtgbazar.be.services.IMessaggioServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UNICO punto di gestione errori:
 * - validazione Req fallita -> 400 col codice risolto via MessaggioServices
 * - MtgException dai service (messaggio gia' risolto) -> 400
 * - BadCredentials/Disabled dal login -> 401 "Credenziali errate"
 *   (Disabled STESSO messaggio: un account disattivato non si distingue
 *   da una password sbagliata — anti-enumeration)
 * - AuthTokenException (refresh invalido, /me senza identita') -> 401
 * - AccessDenied lanciata DENTRO i controller -> 403
 *   (quella della filter chain la scrive l'AccessDeniedHandler)
 * - RuntimeException imprevista -> 500 generico, dettagli solo nel log
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final IMessaggioServices msg;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO> handleValidation(MethodArgumentNotValidException e) {
        String code = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("errore.validazione");
        log.debug("Validazione fallita: {}", code);
        return ResponseEntity.badRequest()
                .body(ResponseDTO.builder().msg(msg.get(code)).build());
    }

    /** Errori di BUSINESS: i service lanciano MtgException gia' risolta. */
    @ExceptionHandler(MtgException.class)
    public ResponseEntity<ResponseDTO> handleBusiness(MtgException e) {
        log.warn("Errore applicativo: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ResponseDTO.builder().msg(e.getMessage()).build());
    }

    /**
     * Login fallito: 401, non 400 — sono le CREDENZIALI a essere
     * sbagliate, non la richiesta. Messaggio unico per password errata,
     * identificativo inesistente e account disattivato.
     */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ResponseDTO> handleCredenziali(RuntimeException e) {
        log.warn("Autenticazione rifiutata: {}", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseDTO.builder().msg(msg.get("utente.credenziali.errate")).build());
    }

    /** Token invalido/scaduto/riusato: il client sa che deve rifare il login. */
    @ExceptionHandler(AuthTokenException.class)
    public ResponseEntity<ResponseDTO> handleToken(AuthTokenException e) {
        log.warn("Token rifiutato: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseDTO.builder().msg(e.getMessage()).build());
    }

    /** Ruolo insufficiente dentro i controller (futura method security). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseDTO> handleAccessoNegato(AccessDeniedException e) {
        log.warn("Accesso negato");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseDTO.builder().msg(msg.get("auth.accesso.negato")).build());
    }

    /** Tutto il resto e' un BUG o un guasto: 500, mai dettagli al client. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseDTO> handleUnexpected(RuntimeException e) {
        log.error("Errore imprevisto", e);
        return ResponseEntity.internalServerError()
                .body(ResponseDTO.builder().msg(msg.get("errore.generico")).build());
    }
}
