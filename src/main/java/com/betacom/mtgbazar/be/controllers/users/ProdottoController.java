package com.betacom.mtgbazar.be.controllers.users;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.products.ProdottoDTO;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.services.interfaces.products.IProdottoServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Catalogo PUBBLICO: nessuna autenticazione, solo prodotti attivi.
 * Strada B: si restituiscono DTO puri, gli errori li traduce
 * il GlobalExceptionHandler (MtgException -> 400 col messaggio IT).
 */
@RestController
@RequestMapping("/api/prodotti")
@RequiredArgsConstructor
@Slf4j
public class ProdottoController {

    private final IProdottoServices prodottoS;

    /** GET /api/prodotti/tipo/SINGLE — la vetrina per categoria. */
    @GetMapping("/tipo/{tipo}")
    public List<ProdottoDTO> listByTipo(@PathVariable TipoProdotto tipo) {
        log.debug("GET /api/prodotti/tipo/{}", tipo);
        return prodottoS.listByTipo(tipo);
    }

    /** GET /api/prodotti/espansione/1 — la pagina del set. */
    @GetMapping("/espansione/{espansioneId}")
    public List<ProdottoDTO> listByEspansione(@PathVariable Long espansioneId) {
        log.debug("GET /api/prodotti/espansione/{}", espansioneId);
        return prodottoS.listByEspansione(espansioneId);
    }

    /** GET /api/prodotti/search?q=tazri — la barra di ricerca. */
    @GetMapping("/search")
    public List<ProdottoDTO> search(@RequestParam("q") String q) {
        log.debug("GET /api/prodotti/search?q={}", q);
        return prodottoS.searchByNome(q);
    }

    /** GET /api/prodotti/tazri-stalwart-survivor-mat-6 — la pagina prodotto. */
    @GetMapping("/{slug}")
    public ProdottoDTO getBySlug(@PathVariable String slug) {
        log.debug("GET /api/prodotti/{}", slug);
        return prodottoS.getBySlug(slug);
    }
    
}