package com.betacom.mtgbazar.be.repositories.products;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.products.PrezzoRiferimento;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;

@Repository
public interface IPrezzoRiferimentoRepository extends JpaRepository<PrezzoRiferimento, Long> {

	/**
     * L'ULTIMA rilevazione per (stampa, finitura): e' il "prezzo di
     * mercato" che l'admin vedra' accanto al suo quando fissa i prezzi
     * SKU (tabella append-only: il piu' recente per detectionDate).
     */
    Optional<PrezzoRiferimento> findTopByStampaIdAndFinituraOrderByDetectionDateDesc(
            Long stampaId, Finitura finitura);
 
    /** La serie storica completa (per un futuro grafico andamento prezzi). */
    List<PrezzoRiferimento> findByStampaIdOrderByDetectionDateAsc(Long stampaId);
    
}