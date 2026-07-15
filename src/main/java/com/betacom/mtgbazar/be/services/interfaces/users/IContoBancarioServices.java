package com.betacom.mtgbazar.be.services.interfaces.users;

import java.util.List;

import com.betacom.mtgbazar.be.dto.users.ContoBancarioDTO;
import com.betacom.mtgbazar.be.request.users.ContoBancarioReq;
 
/**
 * Conti bancari per il ritiro credito. NIENTE update per design:
 * un IBAN sbagliato si disattiva e se ne inserisce uno nuovo — i
 * movimenti di prelievo gia' eseguiti referenziano quello vecchio
 * e devono continuare a dire la verita' (regola del ledger).
 */
public interface IContoBancarioServices {
 
    /** I conti attivi dell'utente, con IBAN mascherato. */
    List<ContoBancarioDTO> listConti(Long utenteId);
 
    ContoBancarioDTO createConto(ContoBancarioReq req);
 
    /** Soft delete con ownership check. */
    void removeConto(Long id, Long utenteId);
    
}