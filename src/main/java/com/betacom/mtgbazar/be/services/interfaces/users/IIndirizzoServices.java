package com.betacom.mtgbazar.be.services.interfaces.users;
 
import java.util.List;
 
import com.betacom.mtgbazar.be.dto.users.IndirizzoDTO;
import com.betacom.mtgbazar.be.request.users.IndirizzoReq;
 
/**
 * Rubrica indirizzi dell'utente. Il "predefinito" NON e' una colonna
 * dell'indirizzo: e' la FK utente.indirizzo_predefinito_id — garantisce
 * strutturalmente "al massimo uno" e va tenuta coerente qui.
 */
public interface IIndirizzoServices {
 
    /** Gli indirizzi attivi dell'utente, col flag predefinito calcolato. */
    List<IndirizzoDTO> listIndirizzi(Long utenteId);
 
    /**
     * Nuovo indirizzo. Diventa il predefinito se req.predefinito = true
     * OPPURE se e' il primo indirizzo dell'utente (UX: chi ne ha uno solo
     * non deve fare due passaggi).
     */
    IndirizzoDTO createIndirizzo(IndirizzoReq req);
 
    /** Modifica con ownership check; predefinito = true lo promuove. */
    IndirizzoDTO updateIndirizzo(IndirizzoReq req);
 
    /**
     * Soft delete con ownership check. Se era il predefinito, la FK
     * sull'utente viene azzerata PRIMA della disattivazione.
     */
    void removeIndirizzo(Long id, Long utenteId);
 
    /** Promuove un indirizzo (attivo e proprio) a predefinito. */
    void setPredefinito(Long id, Long utenteId);
    
}