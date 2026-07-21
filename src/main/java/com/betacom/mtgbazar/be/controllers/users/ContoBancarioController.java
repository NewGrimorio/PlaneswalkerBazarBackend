package com.betacom.mtgbazar.be.controllers.users;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.users.ContoBancarioDTO;
import com.betacom.mtgbazar.be.request.ValidationGroups;
import com.betacom.mtgbazar.be.request.users.ContoBancarioReq;
import com.betacom.mtgbazar.be.security.SecurityUtils;
import com.betacom.mtgbazar.be.services.interfaces.users.IContoBancarioServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/**
 * Conti per il ritiro credito — FASE C: l'utenteId nasce dal token.
 * Via i @RequestParam utenteId; l'ownership check nel service
 * (removeConto) resta a blindare "quale conto".
 */
@RestController
@RequestMapping("/api/conti")
@RequiredArgsConstructor
@Slf4j
public class ContoBancarioController {
 
    private final IContoBancarioServices contoS;
 
    @GetMapping
    public List<ContoBancarioDTO> list(Authentication authentication) {
        Long utenteId = SecurityUtils.utenteId(authentication);
        log.debug("GET /api/conti utente={}", utenteId);
        return contoS.listConti(utenteId);
    }
 
    @PostMapping
    public ContoBancarioDTO create(
            @Validated(ValidationGroups.Create.class) @RequestBody ContoBancarioReq req,
            Authentication authentication) {
        req.setUtenteId(SecurityUtils.utenteId(authentication));
        log.debug("POST /api/conti utente={}", req.getUtenteId());
        return contoS.createConto(req);
    }
 
    @DeleteMapping("/{id}")
    public void remove(@PathVariable Long id, Authentication authentication) {
        Long utenteId = SecurityUtils.utenteId(authentication);
        log.debug("DELETE /api/conti/{} utente={}", id, utenteId);
        contoS.removeConto(id, utenteId);
    }
    
}