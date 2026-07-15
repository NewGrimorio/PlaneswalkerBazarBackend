package com.betacom.mtgbazar.be.repositories.users;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.VoceOrdine;

@Repository
public interface IVoceOrdineRepository extends JpaRepository<VoceOrdine, Long> {

    /**
     * Le voci di un ordine con SKU/prodotto caricati (per reso e ripristino
     * scorte); per la VISUALIZZAZIONE bastano gli snapshot nelle voci.
     * Query in META-INF: VoceOrdine.findByOrdineIdWithSku
     */
    List<VoceOrdine> findByOrdineIdWithSku(@Param("ordineId") Long ordineId);

    /** Batch anti-N+1 per le liste ordini (voci di piu' ordini insieme). */
    List<VoceOrdine> findByOrdineIdIn(List<Long> ordineIds);
}