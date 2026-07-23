package com.betacom.mtgbazar.be.controllers.products;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.products.TendenzaPrezzoCartaDTO;
import com.betacom.mtgbazar.be.services.interfaces.products.ITendenzaPrezzoServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tendenze prezzo in tempo reale di una carta (stampa) e dei suoi SKU.
 * Sotto /api/admin/**: protezione ROLE_ADMIN già imposta dalla filter
 * chain del profilo prod, nessuna annotazione di sicurezza sul metodo.
 *
 * GET (non muta lo stato dal punto di vista del client) anche se dietro
 * le quinte storicizza gli snapshot: l'effetto collaterale è append-only
 * e idempotente per il chiamante, quindi GET è semanticamente corretto.
 */
@RestController
@RequestMapping("/api/admin/magazzino")
@RequiredArgsConstructor
@Slf4j
public class AdminTendenzaPrezzoController {
	
	private final ITendenzaPrezzoServices tendenzaS;

    @GetMapping("/stampa/{stampaId}/tendenze")
    public TendenzaPrezzoCartaDTO tendenze(@PathVariable Long stampaId) {
        log.debug("GET /api/admin/magazzino/stampa/{}/tendenze", stampaId);
        return tendenzaS.tendenzeCarta(stampaId);
    }
	
}
