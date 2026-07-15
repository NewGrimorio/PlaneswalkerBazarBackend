package com.betacom.mtgbazar.be.controllers.products;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.services.interfaces.users.IRecensioneServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/** Moderazione recensioni. */
@RestController
@RequestMapping("/api/admin/recensioni")
@RequiredArgsConstructor
@Slf4j
public class AdminRecensioneController {
 
    private final IRecensioneServices recensioneS;
 
    /** POST /api/admin/recensioni/5/modera?approvata=false — nasconde/ripristina. */
    @PostMapping("/{id}/modera")
    public RecensioneDTO modera(@PathVariable Long id, @RequestParam Boolean approvata) {
        log.debug("POST /api/admin/recensioni/{}/modera approvata={}", id, approvata);
        return recensioneS.modera(id, approvata);
    }
    
}