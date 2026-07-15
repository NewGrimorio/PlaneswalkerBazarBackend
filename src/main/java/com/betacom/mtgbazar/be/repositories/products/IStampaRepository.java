package com.betacom.mtgbazar.be.repositories.products;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.products.Stampa;

@Repository
public interface IStampaRepository extends JpaRepository<Stampa, Long> {

    /** Per il sync Scryfall: la stampa e' unica per scryfallId. */
    Optional<Stampa> findByScryfallId(UUID scryfallId);

    /** Chiave naturale alternativa: set + numero di collezione. */
    Optional<Stampa> findByEspansioneIdAndNumeroCollezione(Long espansioneId, String numeroCollezione);

    /**
     * Tutte le stampe di una carta oracle con l'espansione gia' caricata:
     * e' la sezione "altre versioni" della pagina carta.
     * Query in META-INF: Stampa.findByCartaIdWithEspansione
     */
    List<Stampa> findByCartaIdWithEspansione(@Param("cartaId") Long cartaId);
}