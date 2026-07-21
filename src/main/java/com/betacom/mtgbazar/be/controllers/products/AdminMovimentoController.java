package com.betacom.mtgbazar.be.controllers.products;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.StatoMovimento;
import com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IPortafoglioServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sportello bonifici: coda IN_ATTESA, conferma/rifiuto, storico globale. */
@RestController
@RequestMapping("/api/admin/movimenti")
@RequiredArgsConstructor
@Slf4j
public class AdminMovimentoController {

	private final IPortafoglioServices portafoglioS;
	 
    /** GET /api/admin/movimenti/in-attesa — la coda da lavorare. */
    @GetMapping("/in-attesa")
    public List<MovimentoDTO> inAttesa() {
        log.debug("GET /api/admin/movimenti/in-attesa");
        return portafoglioS.movimentiInAttesa();
    }

    /**
     * GET /api/admin/movimenti/storico?stato=COMPLETATO&metodo=PAYPAL
     * Storico globale (di TUTTI i clienti) dei movimenti CONCLUSI —
     * gli IN_ATTESA restano fuori (sono nell'altra coda). Entrambi i
     * filtri sono facoltativi: assenti = nessun vincolo su quel campo.
     */
    @GetMapping("/storico")
    public List<MovimentoDTO> storico(
            @RequestParam(required = false) StatoMovimento stato,
            @RequestParam(required = false) MetodoMovimento metodo) {
        log.debug("GET /api/admin/movimenti/storico stato={} metodo={}", stato, metodo);
        return portafoglioS.storicoAdmin(stato, metodo);
    }
 
    @PostMapping("/conferma")
    public MovimentoDTO conferma(@Validated @RequestBody ConfermaMovimentoReq req) {
        log.debug("POST /api/admin/movimenti/conferma id={} approvato={}",
                req.getMovimentoId(), req.getApprovato());
        return portafoglioS.confermaMovimento(req);
    }
	
}