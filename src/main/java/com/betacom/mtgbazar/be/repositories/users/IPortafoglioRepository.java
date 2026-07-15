package com.betacom.mtgbazar.be.repositories.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.Portafoglio;

import jakarta.persistence.LockModeType;

@Repository
public interface IPortafoglioRepository extends JpaRepository<Portafoglio, Long> {

    /**
     * Lettura semplice, SENZA lock: per mostrare il saldo, lo storico, ecc.
     * Mai usare questa nei percorsi che MODIFICANO il saldo.
     */
    Optional<Portafoglio> findByUtenteId(Long utenteId);

    /**
     * Caricamento CON lock esclusivo di riga (SELECT ... FOR UPDATE).
     * Query in META-INF/jpa-named-queries.properties:
     * Portafoglio.findByUtenteIdForUpdate
     *
     * Da usare in OGNI percorso che modifica il saldo: ricarica, prelievo,
     * pagamento ordine, rimborso. REGOLE D'USO (pattern Fase 2):
     *  - chiamare SOLO dentro un metodo @Transactional
     *  - mutare il saldo in memoria: il dirty checking fa l'UPDATE al commit
     *  - il CHECK saldo >= 0 a DB resta l'ultima linea di difesa
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Portafoglio> findByUtenteIdForUpdate(@Param("utenteId") Long utenteId);
}