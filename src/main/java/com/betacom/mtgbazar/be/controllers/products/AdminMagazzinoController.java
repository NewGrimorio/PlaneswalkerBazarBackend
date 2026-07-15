package com.betacom.mtgbazar.be.controllers.products;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.products.MagazzinoSKUDTO;
import com.betacom.mtgbazar.be.request.ValidationGroups;
import com.betacom.mtgbazar.be.request.products.MagazzinoSKUReq;
import com.betacom.mtgbazar.be.services.interfaces.products.IMagazzinoSKUServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/sku")
@RequiredArgsConstructor
@Slf4j
public class AdminMagazzinoController {
	
	 private final IMagazzinoSKUServices skuS;
	 
	    /** GET /api/admin/sku?prodottoId=1 — tutte le varianti, anche spente. */
	    @GetMapping
	    public List<MagazzinoSKUDTO> listByProdotto(@RequestParam Long prodottoId) {
	        log.debug("GET /api/admin/sku?prodottoId={}", prodottoId);
	        return skuS.listByProdotto(prodottoId);
	    }
	 
	    @PostMapping
	    public MagazzinoSKUDTO create(
	            @Validated(ValidationGroups.Create.class) @RequestBody MagazzinoSKUReq req) {
	        log.debug("POST /api/admin/sku prodotto={}", req.getProdottoId());
	        return skuS.createSku(req);
	    }
	 
	    @PutMapping
	    public MagazzinoSKUDTO update(
	            @Validated(ValidationGroups.Update.class) @RequestBody MagazzinoSKUReq req) {
	        log.debug("PUT /api/admin/sku id={}", req.getId());
	        return skuS.updateSku(req);
	    }
	
}
