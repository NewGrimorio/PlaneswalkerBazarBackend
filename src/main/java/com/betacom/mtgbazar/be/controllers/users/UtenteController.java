package com.betacom.mtgbazar.be.controllers.users;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.request.ValidationGroups;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.security.CambioEmailReq;
import com.betacom.mtgbazar.be.request.users.security.CambioPasswordReq;
import com.betacom.mtgbazar.be.security.SecurityUtils;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Profilo utente — FASE C: l'identita' NASCE DAL TOKEN.
 *
 * Pattern del tutor: Authentication come parametro del metodo e
 * sovrascrittura sulla request PRIMA di chiamare il service —
 * qualunque id mandi il client viene ignorato. La conversione
 * subject->id passa da SecurityUtils.utenteId (un solo punto, con
 * guardia 401 sull'identita' assente).
 *
 * Tutti gli endpoint sono SELF-SCOPED: operano sull'utente del token.
 * Il vecchio GET /{id} sparisce — "chi sei" non si chiede piu', lo
 * dice il token; per rileggere i propri dati c'e' GET /api/auth/me.
 * I service NON cambiano firma: ricevono l'id come prima, ma ora
 * glielo consegna il token.
 */
@RestController
@RequestMapping("/api/utenti")
@RequiredArgsConstructor
@Slf4j
public class UtenteController {

    private final IUtenteServices utenteS;

    @PutMapping("/profilo")
    public UtenteDTO updateProfilo(
            @Validated(ValidationGroups.Update.class) @RequestBody UtenteReq req,
            Authentication authentication) {
        req.setId(SecurityUtils.utenteId(authentication));   // identita' dal token
        log.debug("PUT /api/utenti/profilo id={}", req.getId());
        return utenteS.updateProfilo(req);
    }

    @PutMapping("/password")
    public UtenteDTO changePassword(@Validated @RequestBody CambioPasswordReq req,
                                    Authentication authentication) {
        req.setUtenteId(SecurityUtils.utenteId(authentication));
        log.debug("PUT /api/utenti/password id={}", req.getUtenteId());
        return utenteS.changePassword(req);
    }

    @PutMapping("/email")
    public UtenteDTO changeEmail(@Validated @RequestBody CambioEmailReq req,
                                 Authentication authentication) {
        req.setUtenteId(SecurityUtils.utenteId(authentication));
        log.debug("PUT /api/utenti/email id={}", req.getUtenteId());
        return utenteS.changeEmail(req);
    }

    //IMMAGINI — self-scoped: niente /{id} nel path

    @PostMapping("/immagine-profilo")
    public UtenteDTO uploadImmagineProfilo(@RequestParam("file") MultipartFile file,
                                           Authentication authentication) {
        Long id = SecurityUtils.utenteId(authentication);
        log.debug("POST /api/utenti/immagine-profilo id={}", id);
        return utenteS.updateImmagineProfilo(id, file);
    }

    @DeleteMapping("/immagine-profilo")
    public UtenteDTO deleteImmagineProfilo(Authentication authentication) {
        Long id = SecurityUtils.utenteId(authentication);
        log.debug("DELETE /api/utenti/immagine-profilo id={}", id);
        return utenteS.removeImmagineProfilo(id);
    }

}