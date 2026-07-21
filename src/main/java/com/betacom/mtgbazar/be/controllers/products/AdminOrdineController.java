package com.betacom.mtgbazar.be.controllers.products;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.users.OrdineDTO;
import com.betacom.mtgbazar.be.model.users.enums.StatOrdine;
import com.betacom.mtgbazar.be.security.SecurityUtils;
import com.betacom.mtgbazar.be.services.interfaces.users.IOrdineServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestione ordini lato NEGOZIO — FASE C: l'adminId dell'AUDIT nasce
 * dal token, non da un @RequestParam. Prima un admin poteva scrivere
 * nel log l'id di un COLLEGA (audit autocertificato); ora "eseguitoDa"
 * dice la verita' su chi ha davvero agito.
 * Il filtro per stato resta un @RequestParam: e' un criterio di
 * ricerca, non un'identita'.
 */
@RestController
@RequestMapping("/api/admin/ordini")
@RequiredArgsConstructor
@Slf4j
public class AdminOrdineController {
	private final IOrdineServices ordineS;
	 
    /** GET /api/admin/ordini?stato=CREATO — la coda "da spedire". */
    @GetMapping
    public List<OrdineDTO> listByStato(@RequestParam StatOrdine stato) {
        log.debug("GET /api/admin/ordini?stato={}", stato);
        return ordineS.listByStato(stato);
    }
 
    @PostMapping("/{id}/spedisci")
    public OrdineDTO spedisci(@PathVariable Long id, Authentication authentication) {
        Long adminId = SecurityUtils.utenteId(authentication);
        log.debug("POST /api/admin/ordini/{}/spedisci admin={}", id, adminId);
        return ordineS.spedisci(id, adminId);
    }
 
    @PostMapping("/{id}/cancella")
    public OrdineDTO cancella(@PathVariable Long id, Authentication authentication) {
        Long adminId = SecurityUtils.utenteId(authentication);
        log.debug("POST /api/admin/ordini/{}/cancella admin={}", id, adminId);
        return ordineS.cancella(id, adminId);
    }
 
    @PostMapping("/{id}/rimborsa")
    public OrdineDTO rimborsa(@PathVariable Long id, Authentication authentication) {
        Long adminId = SecurityUtils.utenteId(authentication);
        log.debug("POST /api/admin/ordini/{}/rimborsa admin={}", id, adminId);
        return ordineS.rimborsa(id, adminId);
    }
    
}
