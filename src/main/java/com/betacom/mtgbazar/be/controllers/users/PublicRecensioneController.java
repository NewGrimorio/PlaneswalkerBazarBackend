package com.betacom.mtgbazar.be.controllers.users;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.dto.users.RecensioneStatisticheDTO;
import com.betacom.mtgbazar.be.services.interfaces.users.IRecensioneServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Recensioni in LETTURA: fanno parte della pagina prodotto, che e'
 * pubblica, quindi vivono sotto /api/public. La SCRITTURA (acquisto
 * verificato: serve un utente) resta in RecensioneController, tier
 * autenticato. Il path dice la verita' su entrambe: e' il motivo
 * dello split.
 */
@RestController
@RequestMapping("/api/public/recensioni")
@RequiredArgsConstructor
@Slf4j
public class PublicRecensioneController {

    private final IRecensioneServices recensioneS;

    @GetMapping("/prodotto/{prodottoId}")
    public List<RecensioneDTO> listByProdotto(@PathVariable Long prodottoId) {
        log.debug("GET /api/public/recensioni/prodotto/{}", prodottoId);
        return recensioneS.listByProdotto(prodottoId);
    }

    @GetMapping("/prodotto/{prodottoId}/statistiche")
    public RecensioneStatisticheDTO statistiche(@PathVariable Long prodottoId) {
        log.debug("GET /api/public/recensioni/prodotto/{}/statistiche", prodottoId);
        return recensioneS.getStatistiche(prodottoId);
    }

}