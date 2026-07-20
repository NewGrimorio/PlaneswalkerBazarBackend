package com.betacom.mtgbazar.be.controllers.users;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.request.users.RecensioneReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IRecensioneServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Recensioni in SCRITTURA (acquisto verificato): serve un utente,
 * quindi tier autenticato. Le letture pubbliche della pagina prodotto
 * sono migrate in PublicRecensioneController (/api/public/recensioni):
 * un controller non puo' avere due base path, e questo era l'unico
 * genuinamente misto.
 */
@RestController
@RequestMapping("/api/recensioni")
@RequiredArgsConstructor
@Slf4j
public class RecensioneController {

    private final IRecensioneServices recensioneS;

    @PostMapping
    public RecensioneDTO save(@Validated @RequestBody RecensioneReq req) {
        log.debug("POST /api/recensioni utente={} prodotto={}",
                req.getUtenteId(), req.getProdottoId());
        return recensioneS.saveRecensione(req);
    }

}