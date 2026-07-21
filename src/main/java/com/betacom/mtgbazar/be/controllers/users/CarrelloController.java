package com.betacom.mtgbazar.be.controllers.users;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.users.CarrelloDTO;
import com.betacom.mtgbazar.be.request.users.VoceCarrelloReq;
import com.betacom.mtgbazar.be.security.SecurityUtils;
import com.betacom.mtgbazar.be.services.interfaces.users.ICarrelloServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/**
 * Carrello — FASE C: l'utenteId nasce dal token, non dal client.
 * Sparisce il segmento /{utenteId} dagli URL: GET /api/carrello e'
 * "il MIO carrello", punto. L'ownership check nel service resta
 * (difesa su "quale voce", complementare a "chi sono").
 */
@RestController
@RequestMapping("/api/carrello")
@RequiredArgsConstructor
@Slf4j
public class CarrelloController {
 
    private final ICarrelloServices carrelloS;
 
    @GetMapping
    public CarrelloDTO get(Authentication authentication) {
        Long utenteId = SecurityUtils.utenteId(authentication);
        log.debug("GET /api/carrello utente={}", utenteId);
        return carrelloS.getCarrello(utenteId);
    }
 
    @PostMapping("/voci")
    public CarrelloDTO addVoce(@Validated @RequestBody VoceCarrelloReq req,
                               Authentication authentication) {
        req.setUtenteId(SecurityUtils.utenteId(authentication));
        log.debug("POST /api/carrello/voci utente={} sku={}", req.getUtenteId(), req.getSkuId());
        return carrelloS.addVoce(req);
    }
 
    @PutMapping("/voci")
    public CarrelloDTO updateVoce(@Validated @RequestBody VoceCarrelloReq req,
                                  Authentication authentication) {
        req.setUtenteId(SecurityUtils.utenteId(authentication));
        log.debug("PUT /api/carrello/voci utente={} sku={}", req.getUtenteId(), req.getSkuId());
        return carrelloS.updateVoce(req);
    }
 
    @DeleteMapping("/voci/{voceId}")
    public CarrelloDTO removeVoce(@PathVariable Long voceId, Authentication authentication) {
        Long utenteId = SecurityUtils.utenteId(authentication);
        log.debug("DELETE /api/carrello/voci/{} utente={}", voceId, utenteId);
        return carrelloS.removeVoce(utenteId, voceId);
    }
 
    @DeleteMapping
    public void clear(Authentication authentication) {
        Long utenteId = SecurityUtils.utenteId(authentication);
        log.debug("DELETE /api/carrello utente={}", utenteId);
        carrelloS.clearCarrello(utenteId);
    }
    
}