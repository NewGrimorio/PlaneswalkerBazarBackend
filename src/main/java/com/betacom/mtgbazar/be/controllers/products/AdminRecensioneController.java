package com.betacom.mtgbazar.be.controllers.products;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.model.users.enums.StatoRecensione;
import com.betacom.mtgbazar.be.services.interfaces.users.IRecensioneServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/** Moderazione recensioni: coda per stato + nascondi/ripristina. */
@RestController
@RequestMapping("/api/admin/recensioni")
@RequiredArgsConstructor
@Slf4j
public class AdminRecensioneController {
 
    private final IRecensioneServices recensioneS;

    /**
     * GET /api/admin/recensioni?stato=APPROVATA — la coda di moderazione.
     * A differenza della lettura pubblica (solo APPROVATE di UN prodotto),
     * qui si vedono le recensioni di OGNI prodotto nello stato scelto, col
     * nome del prodotto, per poterle nascondere o ripristinare.
     */
    @GetMapping
    public List<RecensioneDTO> listByStato(@RequestParam StatoRecensione stato) {
        log.debug("GET /api/admin/recensioni?stato={}", stato);
        return recensioneS.listByStatoAdmin(stato);
    }
 
    /** POST /api/admin/recensioni/5/modera?approvata=false — nasconde/ripristina. */
    @PostMapping("/{id}/modera")
    public RecensioneDTO modera(@PathVariable Long id, @RequestParam Boolean approvata) {
        log.debug("POST /api/admin/recensioni/{}/modera approvata={}", id, approvata);
        return recensioneS.modera(id, approvata);
    }
    
}