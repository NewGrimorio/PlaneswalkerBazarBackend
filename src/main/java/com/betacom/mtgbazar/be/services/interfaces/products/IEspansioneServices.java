package com.betacom.mtgbazar.be.services.interfaces.products;

import java.util.List;
 
import com.betacom.mtgbazar.be.dto.products.EspansioneDTO;
import com.betacom.mtgbazar.be.request.products.EspansioneReq;
 
/**
 * Set/espansioni. Di norma arrivano dal sync Scryfall: il CRUD
 * manuale serve per correzioni e casi particolari.
 */
public interface IEspansioneServices {
 
    /** Tutte le espansioni, dalla piu' recente (menu del negozio). */
    List<EspansioneDTO> listEspansioni();
 
    /** Dettaglio per codice set (es. "mh3"). */
    EspansioneDTO getByCodice(String codice);
 
    /** ADMIN: creazione manuale (codice normalizzato minuscolo). */
    EspansioneDTO createEspansione(EspansioneReq req);
 
    /** ADMIN: modifica (il codice non si cambia: e' l'identita' del set). */
    EspansioneDTO updateEspansione(EspansioneReq req);
    
}