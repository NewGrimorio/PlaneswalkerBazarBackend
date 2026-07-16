package com.betacom.mtgbazar.be.controllers.products;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.betacom.mtgbazar.be.dto.products.EspansioneDTO;
import com.betacom.mtgbazar.be.dto.products.ImmagineDTO;
import com.betacom.mtgbazar.be.dto.products.ProdottoDTO;
import com.betacom.mtgbazar.be.request.ValidationGroups;
import com.betacom.mtgbazar.be.request.products.EspansioneReq;
import com.betacom.mtgbazar.be.request.products.ProdottoReq;
import com.betacom.mtgbazar.be.services.interfaces.products.IEspansioneServices;
import com.betacom.mtgbazar.be.services.interfaces.products.IImmagineServices;
import com.betacom.mtgbazar.be.services.interfaces.products.IProdottoServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/**
 * CRUD manuale del catalogo: prodotti (sigillato/accessori — i SINGLE
 * nascono dal sync) e correzioni alle espansioni.
 */
@RestController
@RequestMapping("/api/admin/catalogo")
@RequiredArgsConstructor
@Slf4j
public class AdminCatalogoController {

	private final IProdottoServices prodottoS;
    private final IEspansioneServices espansioneS;
    private final IImmagineServices imgS;
 
    @PostMapping("/prodotti")
    public ProdottoDTO createProdotto(
            @Validated(ValidationGroups.Create.class) @RequestBody ProdottoReq req) {
        log.debug("POST /api/admin/catalogo/prodotti {}", req.getNome());
        return prodottoS.createProdotto(req);
    }
 
    @PutMapping("/prodotti")
    public ProdottoDTO updateProdotto(
            @Validated(ValidationGroups.Update.class) @RequestBody ProdottoReq req) {
        log.debug("PUT /api/admin/catalogo/prodotti id={}", req.getId());
        return prodottoS.updateProdotto(req);
    }
 
    @PostMapping("/espansioni")
    public EspansioneDTO createEspansione(
            @Validated(ValidationGroups.Create.class) @RequestBody EspansioneReq req) {
        log.debug("POST /api/admin/catalogo/espansioni {}", req.getCodice());
        return espansioneS.createEspansione(req);
    }
 
    @PutMapping("/espansioni")
    public EspansioneDTO updateEspansione(
            @Validated(ValidationGroups.Update.class) @RequestBody EspansioneReq req) {
        log.debug("PUT /api/admin/catalogo/espansioni id={}", req.getId());
        return espansioneS.updateEspansione(req);
    }
    
    @PostMapping("/prodotti/immagine")
    public ImmagineDTO uploadImmagine(@RequestParam("file") MultipartFile file) {
        log.debug("POST /api/admin/catalogo/prodotti/immagine");
        return imgS.salvaImmagine(file, "prodotti");
    }
    
}
