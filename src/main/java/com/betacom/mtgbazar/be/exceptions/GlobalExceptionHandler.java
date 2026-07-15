package com.betacom.mtgbazar.be.exceptions;

import org.springframework.http.ResponseEntity;
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

    /** Tutto il resto e' un BUG o un guasto: 500, mai dettagli al client. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseDTO> handleUnexpected(RuntimeException e) {
        log.error("Errore imprevisto", e);
        return ResponseEntity.internalServerError()
                .body(ResponseDTO.builder().msg(msg.get("errore.generico")).build());
    }
}