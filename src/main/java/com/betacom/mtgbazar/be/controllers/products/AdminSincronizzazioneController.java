package com.betacom.mtgbazar.be.controllers.products;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.products.SincronizzazioneDTO;
import com.betacom.mtgbazar.be.services.interfaces.products.ISincronizzazioneServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Il telecomando del catalogo: importa un set da Scryfall. */
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
@Slf4j
public class AdminSincronizzazioneController {
	
	private final ISincronizzazioneServices syncS;
	 
    /** POST /api/admin/sync/mh3 — sincrono: risponde a fine import. */
    @PostMapping("/{codiceSet}")
    public SincronizzazioneDTO sincronizza(@PathVariable String codiceSet) {
        log.debug("POST /api/admin/sync/{}", codiceSet);
        return syncS.sincronizzaSet(codiceSet);
    }
    
}
