package com.betacom.mtgbazar.be.controllers.users;

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
import com.betacom.mtgbazar.be.services.interfaces.users.ICarrelloServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/** Carrello: ogni operazione restituisce il carrello aggiornato. */
@RestController
@RequestMapping("/api/carrello")
@RequiredArgsConstructor
@Slf4j
public class CarrelloController {
 
    private final ICarrelloServices carrelloS;
 
    @GetMapping("/{utenteId}")
    public CarrelloDTO get(@PathVariable Long utenteId) {
        log.debug("GET /api/carrello/{}", utenteId);
        return carrelloS.getCarrello(utenteId);
    }
 
    @PostMapping("/voci")
    public CarrelloDTO addVoce(@Validated @RequestBody VoceCarrelloReq req) {
        log.debug("POST /api/carrello/voci utente={} sku={}", req.getUtenteId(), req.getSkuId());
        return carrelloS.addVoce(req);
    }
 
    @PutMapping("/voci")
    public CarrelloDTO updateVoce(@Validated @RequestBody VoceCarrelloReq req) {
        log.debug("PUT /api/carrello/voci utente={} sku={}", req.getUtenteId(), req.getSkuId());
        return carrelloS.updateVoce(req);
    }
 
    @DeleteMapping("/{utenteId}/voci/{voceId}")
    public CarrelloDTO removeVoce(@PathVariable Long utenteId, @PathVariable Long voceId) {
        log.debug("DELETE /api/carrello/{}/voci/{}", utenteId, voceId);
        return carrelloS.removeVoce(utenteId, voceId);
    }
 
    @DeleteMapping("/{utenteId}")
    public void clear(@PathVariable Long utenteId) {
        log.debug("DELETE /api/carrello/{}", utenteId);
        carrelloS.clearCarrello(utenteId);
    }
    
}