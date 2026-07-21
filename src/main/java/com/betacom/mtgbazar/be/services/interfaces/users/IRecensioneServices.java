package com.betacom.mtgbazar.be.services.interfaces.users;

import java.util.List;
 
import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.dto.users.RecensioneStatisticheDTO;
import com.betacom.mtgbazar.be.model.users.enums.StatoRecensione;
import com.betacom.mtgbazar.be.request.users.RecensioneReq;
 
/**
 * Recensioni "acquisto verificato": il diritto nasce da un ordine
 * CONSEGNATO dell'utente che contiene il prodotto. Una sola recensione
 * per coppia (utente, prodotto): il salvataggio ripetuto AGGIORNA.
 */
public interface IRecensioneServices {
 
    /**
     * Crea o aggiorna la recensione dell'utente sul prodotto.
     * Verifiche: ordine dell'utente, stato CONSEGNATO, prodotto
     * presente tra le voci. La modifica riporta lo stato ad
     * APPROVATA (il contenuto e' cambiato: si ri-modera da capo).
     */
    RecensioneDTO saveRecensione(RecensioneReq req);
 
    /** Le recensioni APPROVATE della pagina prodotto, dalla piu' recente. */
    List<RecensioneDTO> listByProdotto(Long prodottoId);
 
    /** Media (1 decimale) e conteggio per la testata della pagina prodotto. */
    RecensioneStatisticheDTO getStatistiche(Long prodottoId);
 
    /** ADMIN: nasconde (false) o ripristina (true) una recensione. */
    RecensioneDTO modera(Long recensioneId, Boolean approvata);
    
    List<RecensioneDTO> listByStatoAdmin(StatoRecensione stato);
    
}