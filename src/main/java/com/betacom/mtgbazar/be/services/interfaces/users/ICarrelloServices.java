package com.betacom.mtgbazar.be.services.interfaces.users;


import com.betacom.mtgbazar.be.dto.users.CarrelloDTO;
import com.betacom.mtgbazar.be.request.users.VoceCarrelloReq;

 
/**
 * Carrello persistente, uno per utente, creato pigramente al primo
 * accesso. I prezzi nel carrello sono sempre LIVE dagli SKU (lo
 * snapshot avviene solo al checkout). Ogni metodo restituisce il
 * carrello completo aggiornato: il frontend non tiene stato.
 */
public interface ICarrelloServices {
 
    /** Il carrello coi prezzi live e i flag disponibilita'. */
    CarrelloDTO getCarrello(Long utenteId);
 
    /**
     * Add-to-cart: se la variante e' gia' presente INCREMENTA la
     * quantita'. Blocca se (in carrello + richiesta) supera la giacenza.
     */
    CarrelloDTO addVoce(VoceCarrelloReq req);
 
    /** Imposta la quantita' ESATTA di una voce esistente. */
    CarrelloDTO updateVoce(VoceCarrelloReq req);
 
    /** Rimuove una voce (ownership check tramite il carrello). */
    CarrelloDTO removeVoce(Long utenteId, Long voceId);
 
    /** Svuota il carrello (usato anche dal checkout a fine transazione). */
    void clearCarrello(Long utenteId);
    
}
 