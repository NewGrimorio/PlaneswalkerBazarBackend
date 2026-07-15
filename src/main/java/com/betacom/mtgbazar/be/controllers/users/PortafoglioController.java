package com.betacom.mtgbazar.be.controllers.users;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.dto.users.PortafoglioDTO;
import com.betacom.mtgbazar.be.request.users.PrelievoReq;
import com.betacom.mtgbazar.be.request.users.RicaricaReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IPortafoglioServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Il credito del cliente: saldo, ricariche, ritiri, storico. */
@RestController
@RequestMapping("/api/portafoglio")
@RequiredArgsConstructor
@Slf4j
public class PortafoglioController {

    private final IPortafoglioServices portafoglioS;

    @GetMapping("/{utenteId}")
    public PortafoglioDTO getByUtente(@PathVariable Long utenteId) {
        log.debug("GET /api/portafoglio/{}", utenteId);
        return portafoglioS.getByUtente(utenteId);
    }

    @PostMapping("/ricarica")
    public MovimentoDTO ricarica(@Validated @RequestBody RicaricaReq req) {
        log.debug("POST /api/portafoglio/ricarica utente={}", req.getUtenteId());
        return portafoglioS.ricarica(req);
    }

    @PostMapping("/prelievo")
    public MovimentoDTO preleva(@Validated @RequestBody PrelievoReq req) {
        log.debug("POST /api/portafoglio/prelievo utente={}", req.getUtenteId());
        return portafoglioS.preleva(req);
    }

    @GetMapping("/{utenteId}/storico")
    public List<MovimentoDTO> storico(@PathVariable Long utenteId) {
        log.debug("GET /api/portafoglio/{}/storico", utenteId);
        return portafoglioS.storico(utenteId);
    }
    
}