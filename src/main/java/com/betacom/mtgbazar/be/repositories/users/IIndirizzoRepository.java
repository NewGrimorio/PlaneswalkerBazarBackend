package com.betacom.mtgbazar.be.repositories.users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.Indirizzo;

@Repository
public interface IIndirizzoRepository extends JpaRepository<Indirizzo, Long> {

    /** Gli indirizzi attivi dell'utente (rubrica nel profilo). */
    List<Indirizzo> findByUtenteIdAndAttivoTrueOrderByCreationDateAsc(Long utenteId);

    /**
     * OWNERSHIP CHECK (pattern Fase 2): carica l'indirizzo SOLO se
     * appartiene all'utente. Da usare in ogni modifica/disattivazione:
     * l'id arriva dal client e non e' mai da fidarsi da solo.
     */
    Optional<Indirizzo> findByIdAndUtenteId(Long id, Long utenteId);
}