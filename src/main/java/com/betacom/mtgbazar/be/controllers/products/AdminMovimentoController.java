package com.betacom.mtgbazar.be.controllers.products;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IPortafoglioServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sportello bonifici: la coda IN_ATTESA e la conferma/rifiuto. */
@RestController
@RequestMapping("/api/admin/movimenti")
@RequiredArgsConstructor
@Slf4j
public class AdminMovimentoController {

	private final IPortafoglioServices portafoglioS;
	 
    /** GET /api/admin/movimenti/in-attesa — kebab-case, come sempre. */
    @GetMapping("/in-attesa")
    public List<MovimentoDTO> inAttesa() {
        log.debug("GET /api/admin/movimenti/in-attesa");
        return portafoglioS.movimentiInAttesa();
    }
 
    @PostMapping("/conferma")
    public MovimentoDTO conferma(@Validated @RequestBody ConfermaMovimentoReq req) {
        log.debug("POST /api/admin/movimenti/conferma id={} approvato={}",
                req.getMovimentoId(), req.getApprovato());
        return portafoglioS.confermaMovimento(req);
    }
	
}
