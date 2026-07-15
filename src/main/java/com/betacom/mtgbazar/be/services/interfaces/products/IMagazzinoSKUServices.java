package com.betacom.mtgbazar.be.services.interfaces.products;

import java.util.List;

import com.betacom.mtgbazar.be.dto.products.MagazzinoSKUDTO;
import com.betacom.mtgbazar.be.request.products.MagazzinoSKUReq;
 
/**
 * Pannello magazzino ADMIN: le varianti vendibili di un prodotto,
 * prezzi e giacenze. La variante (condizione+lingua+finitura) e'
 * IMMUTABILE: variante nuova = SKU nuovo.
 */
public interface IMagazzinoSKUServices {
 
    /** ADMIN: tutte le varianti del prodotto, anche disattivate. */
    List<MagazzinoSKUDTO> listByProdotto(Long prodottoId);
 
    /** ADMIN: nuova variante (default: NA / en / NONFOIL). */
    MagazzinoSKUDTO createSku(MagazzinoSKUReq req);
 
    /** ADMIN: aggiorna prezzo/quantita'/attivo — MAI la variante. */
    MagazzinoSKUDTO updateSku(MagazzinoSKUReq req);
}