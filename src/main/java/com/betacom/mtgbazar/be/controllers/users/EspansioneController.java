package com.betacom.mtgbazar.be.controllers.users;

import java.util.List;
 
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.products.EspansioneDTO;
import com.betacom.mtgbazar.be.services.interfaces.products.IEspansioneServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/** Espansioni PUBBLICHE: il menu dei set del negozio, sotto /api/public. */
@RestController
@RequestMapping("/api/public/espansioni")
@RequiredArgsConstructor
@Slf4j
public class EspansioneController {
 
    private final IEspansioneServices espansioneS;
 
    @GetMapping
    public List<EspansioneDTO> list() {
        log.debug("GET /api/public/espansioni");
        return espansioneS.listEspansioni();
    }
 
    @GetMapping("/{codice}")
    public EspansioneDTO getByCodice(@PathVariable String codice) {
        log.debug("GET /api/public/espansioni/{}", codice);
        return espansioneS.getByCodice(codice);
    }
    
}