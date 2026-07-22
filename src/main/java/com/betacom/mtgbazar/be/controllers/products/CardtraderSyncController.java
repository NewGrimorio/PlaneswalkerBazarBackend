package com.betacom.mtgbazar.be.controllers.products;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.products.CardtraderSyncDTO;
import com.betacom.mtgbazar.be.services.interfaces.products.ICardtraderSyncServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Trigger dell'arricchimento Cardtrader (blueprint_id sulle stampe).
 *
 * Sotto /api/admin/**: la protezione ROLE_ADMIN e' gia' imposta dalla
 * filter chain del profilo prod (SecurityConfig) — nessuna annotazione
 * di sicurezza sul metodo, sarebbe ridondante.
 *
 * POST perche' muta il catalogo. Restituisce il riepilogo alla pagina
 * "Sincronizza" dell'admin.
 */
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
@Slf4j
public class CardtraderSyncController {

    private final ICardtraderSyncServices cardtraderSyncS;

    @PostMapping("/cardtrader")
    public CardtraderSyncDTO cardtrader() {
        log.debug("POST /api/admin/sync/cardtrader");
        return cardtraderSyncS.sincronizzaBlueprint();
    }
}