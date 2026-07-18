package com.betacom.mtgbazar.be.repositories.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.Utente;

@Repository
public interface IUtenteRepository extends JpaRepository<Utente, Long> {

    /**
     * Lookup di login. Ricordare: l'email va normalizzata (trim+lowercase)
     * nel service PRIMA di chiamare questo metodo, in salvataggio E ricerca.
     */
    Optional<Utente> findByEmail(String email);
    
    Optional<Utente> findByUsername(String username);
    
    /** Per la validazione in registrazione: errore pulito prima del vincolo DB. */
    boolean existsByEmail(String email);

    boolean existsByCodiceFiscale(String codiceFiscale);
    
    boolean existsByUsername(String username);
    
    
}