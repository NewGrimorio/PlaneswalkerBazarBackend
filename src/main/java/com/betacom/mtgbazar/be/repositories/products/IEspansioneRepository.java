package com.betacom.mtgbazar.be.repositories.products;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.products.Espansione;

@Repository
public interface IEspansioneRepository extends JpaRepository<Espansione, Long> {

    /** Lookup per codice set ("MH3", "SLD"): chiave naturale del dominio. */
    Optional<Espansione> findByCodice(String codice);

    /** Per il sync Scryfall: upsert basato sull'id esterno. */
    Optional<Espansione> findByScryfallId(java.util.UUID scryfallId);

    /** Elenco per la navigazione, dalle piu' recenti. */
    List<Espansione> findAllByOrderByDataUscitaDesc();

    /** Elenco filtrato per tipo set (es. solo le expansion "vere"). */
    List<Espansione> findByTipoSetOrderByDataUscitaDesc(String tipoSet);
    
}