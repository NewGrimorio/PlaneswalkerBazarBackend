package com.betacom.mtgbazar.be.controllers.users;

import java.util.List;
 
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.users.OrdineDTO;
import com.betacom.mtgbazar.be.dto.users.StoricoStatoOrdineDTO;
import com.betacom.mtgbazar.be.request.users.CheckoutReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IOrdineServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/**
 * Ordini lato CLIENTE. Le transizioni sono azioni esplicite in
 * kebab-case (conferma-consegna, non confermaConsegna: i path
 * camelCase in Fase 2 costavano 404 misteriosi).
 */
@RestController
@RequestMapping("/api/ordini")
@RequiredArgsConstructor
@Slf4j
public class OrdineController {
 
    private final IOrdineServices ordineS;
 
    @PostMapping("/checkout")
    public OrdineDTO checkout(@Validated @RequestBody CheckoutReq req) {
        log.debug("POST /api/ordini/checkout utente={}", req.getUtenteId());
        return ordineS.checkout(req);
    }
 
    @GetMapping
    public List<OrdineDTO> list(@RequestParam Long utenteId) {
        log.debug("GET /api/ordini?utenteId={}", utenteId);
        return ordineS.listOrdini(utenteId);
    }
 
    @GetMapping("/{id}")
    public OrdineDTO dettaglio(@PathVariable Long id, @RequestParam Long utenteId) {
        log.debug("GET /api/ordini/{} utente={}", id, utenteId);
        return ordineS.getDettaglio(id, utenteId);
    }
 
    @GetMapping("/{id}/timeline")
    public List<StoricoStatoOrdineDTO> timeline(@PathVariable Long id, @RequestParam Long utenteId) {
        log.debug("GET /api/ordini/{}/timeline utente={}", id, utenteId);
        return ordineS.getTimeline(id, utenteId);
    }
 
    @PostMapping("/{id}/annulla")
    public OrdineDTO annulla(@PathVariable Long id, @RequestParam Long utenteId) {
        log.debug("POST /api/ordini/{}/annulla utente={}", id, utenteId);
        return ordineS.annulla(id, utenteId);
    }
 
    @PostMapping("/{id}/conferma-consegna")
    public OrdineDTO confermaConsegna(@PathVariable Long id, @RequestParam Long utenteId) {
        log.debug("POST /api/ordini/{}/conferma-consegna utente={}", id, utenteId);
        return ordineS.confermaConsegna(id, utenteId);
    }
 
    @PostMapping("/{id}/segnala-non-consegnato")
    public OrdineDTO segnalaNonConsegnato(@PathVariable Long id, @RequestParam Long utenteId) {
        log.debug("POST /api/ordini/{}/segnala-non-consegnato utente={}", id, utenteId);
        return ordineS.segnalaNonConsegnato(id, utenteId);
    }
 
    @PostMapping("/{id}/richiedi-reso")
    public OrdineDTO richiediReso(@PathVariable Long id, @RequestParam Long utenteId) {
        log.debug("POST /api/ordini/{}/richiedi-reso utente={}", id, utenteId);
        return ordineS.richiediReso(id, utenteId);
    }
    
}