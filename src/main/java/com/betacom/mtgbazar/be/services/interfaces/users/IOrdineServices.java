package com.betacom.mtgbazar.be.services.interfaces.users;


import java.util.List;
 
import com.betacom.mtgbazar.be.dto.users.OrdineDTO;
import com.betacom.mtgbazar.be.dto.users.StoricoStatoOrdineDTO;
import com.betacom.mtgbazar.be.model.users.enums.StatOrdine;
import com.betacom.mtgbazar.be.request.users.CheckoutReq;
 
/**
 * Ordini e state machine (8 stati, ereditata dalla Fase 2):
 *   CREATO -> SPEDITO -> CONSEGNATO / NON_CONSEGNATO
 *   CREATO -> ANNULLATO (cliente) / CANCELLATO (admin)
 *   CONSEGNATO -> RESO_RICHIESTO;  RESO_RICHIESTO / NON_CONSEGNATO -> RIMBORSATO
 * Ogni transizione scrive una riga di storico_stato_ordine.
 * REGOLA DI LOCK del progetto: prima magazzino_sku (id ordinati), poi portafoglio.
 */
public interface IOrdineServices {
 
    /**
     * LA transazione del sistema: lock SKU -> verifica scorte ->
     * lock portafoglio -> verifica saldo -> decrementi -> ordine con
     * snapshot (indirizzo + prezzi) -> movimento PAGAMENTO_ORDINE ->
     * svuotamento carrello. Tutto o niente.
     */
    OrdineDTO checkout(CheckoutReq req);
 
    /** Lista ordini dell'utente, dal piu' recente (senza voci). */
    List<OrdineDTO> listOrdini(Long utenteId);
 
    /** Dettaglio con voci e ownership check. */
    OrdineDTO getDettaglio(Long ordineId, Long utenteId);
 
    /** Timeline dei cambi di stato (ownership check). */
    List<StoricoStatoOrdineDTO> getTimeline(Long ordineId, Long utenteId);
 
    // --- Transizioni CLIENTE (ownership + state machine + storico) ---
 
    /** CREATO -> ANNULLATO: ripristina le scorte e rimborsa il portafoglio. */
    OrdineDTO annulla(Long ordineId, Long utenteId);
 
    /** SPEDITO -> CONSEGNATO. */
    OrdineDTO confermaConsegna(Long ordineId, Long utenteId);
 
    /** SPEDITO -> NON_CONSEGNATO. */
    OrdineDTO segnalaNonConsegnato(Long ordineId, Long utenteId);
 
    /** CONSEGNATO -> RESO_RICHIESTO. */
    OrdineDTO richiediReso(Long ordineId, Long utenteId);
 
    // --- Transizioni ADMIN (state machine + storico con eseguitoDa) ---
 
    /** CREATO -> SPEDITO. */
    OrdineDTO spedisci(Long ordineId, Long adminId);
 
    /** CREATO -> CANCELLATO: ripristina le scorte e rimborsa. */
    OrdineDTO cancella(Long ordineId, Long adminId);
 
    /**
     * RESO_RICHIESTO / NON_CONSEGNATO -> RIMBORSATO: rimborso sul
     * portafoglio; NESSUN ripristino scorte automatico (regola Fase 2:
     * il reso puo' tornare danneggiato, decide l'admin a mano).
     */
    OrdineDTO rimborsa(Long ordineId, Long adminId);
 
    /** ADMIN: coda di lavoro per stato, dalla piu' vecchia. */
    List<OrdineDTO> listByStato(StatOrdine stato);
    
}