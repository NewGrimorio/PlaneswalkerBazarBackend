package com.betacom.mtgbazar.be.services.interfaces.users;

import java.util.List;
 
import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.dto.users.PortafoglioDTO;
import com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq;
import com.betacom.mtgbazar.be.request.users.PrelievoReq;
import com.betacom.mtgbazar.be.request.users.RicaricaReq;
import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.StatoMovimento;
 
/**
 * Operazioni sul portafoglio. Gli errori di business viaggiano come
 * MtgException (runtime): nessun throws nelle firme, li raccoglie
 * il GlobalExceptionHandler -> 400.
 */
public interface IPortafoglioServices {
 
    /** La schermata "Credito": saldo attuale (lettura, senza lock). */
    PortafoglioDTO getByUtente(Long utenteId);
 
    /**
     * Ricarica. PAYPAL: accredito immediato del NETTO (importo meno
     * commissione 5% + 0,35 calcolata QUI), movimento COMPLETATO.
     * BONIFICO: movimento IN_ATTESA, il saldo si muove solo alla
     * conferma admin. INTERNO non e' ammesso dal client.
     */
    MovimentoDTO ricarica(RicaricaReq req);
 
    /**
     * "Ritira credito": ownership del conto, saldo sufficiente (lock),
     * decurtazione IMMEDIATA e movimento IN_ATTESA — cosi' un doppio
     * click non puo' prelevare due volte lo stesso denaro.
     */
    MovimentoDTO preleva(PrelievoReq req);
 
    /** "Tutte le transazioni": storico dal ledger, dal piu' recente. */
    List<MovimentoDTO> storico(Long utenteId);
 
    /** ADMIN: coda dei movimenti IN_ATTESA da lavorare. */
    List<MovimentoDTO> movimentiInAttesa();
 
    /**
     * ADMIN: approva/rifiuta un movimento IN_ATTESA.
     * Ricarica approvata -> accredito (lock); rifiutata -> nulla.
     * Prelievo approvato -> chiusura; rifiutato -> ri-accredito (lock).
     */
    MovimentoDTO confermaMovimento(ConfermaMovimentoReq req);
    
    List<MovimentoDTO> storicoAdmin(StatoMovimento stato, MetodoMovimento metodo);
    
}