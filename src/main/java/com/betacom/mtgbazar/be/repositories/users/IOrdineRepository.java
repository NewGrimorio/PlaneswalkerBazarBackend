package com.betacom.mtgbazar.be.repositories.users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.Ordine;
import com.betacom.mtgbazar.be.model.users.enums.StatOrdine;
 
@Repository
public interface IOrdineRepository extends JpaRepository<Ordine, Long> {
 
    /** Lista ordini dell'utente, dal piu' recente. */
    List<Ordine> findByUtenteIdOrderByCreationDateDesc(Long utenteId);
 
    /**
     * OWNERSHIP CHECK: base del pattern caricaEValidaStatoOwner.
     * Il dettaglio/azione su un ordine passa SEMPRE da qui per i clienti.
     */
    Optional<Ordine> findByIdAndUtenteId(Long id, Long utenteId);
 
    /** Pannello admin: coda di lavoro per stato, dalla piu' vecchia. */
    List<Ordine> findByStatoOrderByCreationDateAsc(StatOrdine stato);
}
