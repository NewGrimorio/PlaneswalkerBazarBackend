package com.betacom.mtgbazar.be.controllers.users;

import java.util.List;
 
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.dto.users.RecensioneStatisticheDTO;
import com.betacom.mtgbazar.be.request.users.RecensioneReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IRecensioneServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/** Recensioni: scrittura (acquisto verificato) e lettura pubblica. */
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
 
    @GetMapping("/prodotto/{prodottoId}")
    public List<RecensioneDTO> listByProdotto(@PathVariable Long prodottoId) {
        log.debug("GET /api/recensioni/prodotto/{}", prodottoId);
        return recensioneS.listByProdotto(prodottoId);
    }
 
    @GetMapping("/prodotto/{prodottoId}/statistiche")
    public RecensioneStatisticheDTO statistiche(@PathVariable Long prodottoId) {
        log.debug("GET /api/recensioni/prodotto/{}/statistiche", prodottoId);
        return recensioneS.getStatistiche(prodottoId);
    }
    
}