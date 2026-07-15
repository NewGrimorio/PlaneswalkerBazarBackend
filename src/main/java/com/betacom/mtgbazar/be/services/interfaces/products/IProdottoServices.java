package com.betacom.mtgbazar.be.services.interfaces.products;

import java.util.List;

import com.betacom.mtgbazar.be.dto.products.ProdottoDTO;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.request.products.ProdottoReq;

/**
 * Catalogo prodotti: navigazione PUBBLICA (solo attivi) e CRUD ADMIN.
 * I prodotti SINGLE nascono dal sync Scryfall; da questo service si
 * creano sigillato, accessori e lotti.
 */
public interface IProdottoServices {

    // --- Navigazione pubblica (solo prodotti attivi) ---

    /** Vetrina per categoria (SINGLE, BOX, BUSTA, MAZZO, ACCESSORIO...). */
    List<ProdottoDTO> listByTipo(TipoProdotto tipo);

    /** I prodotti di un'espansione (pagina set). */
    List<ProdottoDTO> listByEspansione(Long espansioneId);

    /** Ricerca per nome, case-insensitive. */
    List<ProdottoDTO> searchByNome(String testo);

    /** La pagina prodotto: grafo completo (stampa, carta, varianti). */
    ProdottoDTO getBySlug(String slug);

    // --- ADMIN ---

    /** Crea sigillato/accessorio; slug generato dal nome se assente. */
    ProdottoDTO createProdotto(ProdottoReq req);

    /** Modifica null-safe; il TIPO e' immutabile (cambia la natura del prodotto). */
    ProdottoDTO updateProdotto(ProdottoReq req);
    
}