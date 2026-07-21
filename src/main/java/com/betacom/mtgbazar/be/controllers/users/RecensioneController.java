package com.betacom.mtgbazar.be.controllers.users;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.request.users.RecensioneReq;
import com.betacom.mtgbazar.be.security.SecurityUtils;
import com.betacom.mtgbazar.be.services.interfaces.users.IRecensioneServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Recensioni in SCRITTURA — FASE C: l'autore nasce dal token, non dal
 * body. Il service verifica poi il diritto (ordine dell'utente,
 * CONSEGNATO, contenente il prodotto): ora quel controllo lavora su
 * un utenteId FIDATO. Le letture pubbliche vivono in
 * PublicRecensioneController (/api/public/recensioni).
 */
@RestController
@RequestMapping("/api/recensioni")
@RequiredArgsConstructor
@Slf4j
public class RecensioneController {

    private final IRecensioneServices recensioneS;

    @PostMapping
    public RecensioneDTO save(@Validated @RequestBody RecensioneReq req,
                              Authentication authentication) {
        req.setUtenteId(SecurityUtils.utenteId(authentication));
        log.debug("POST /api/recensioni utente={} prodotto={}",
                req.getUtenteId(), req.getProdottoId());
        return recensioneS.saveRecensione(req);
    }

}