package com.betacom.mtgbazar.be.repositories.products;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.betacom.mtgbazar.be.model.products.Carta;

public interface ICartaRepository extends JpaRepository<Carta, Long> {
 
    /** Per il sync Scryfall: la carta oracle e' unica per oracleId. */
    Optional<Carta> findByOracleId(UUID oracleId);
 
    /**
     * Ricerca carte per nome, case-insensitive.
     * Query in META-INF: Carta.searchByNome
     */
    List<Carta> searchByNome(@Param("testo") String testo);
    
}