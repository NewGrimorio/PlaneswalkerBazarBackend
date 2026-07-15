package com.betacom.mtgbazar.be.repositories.users;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.StoricoStatoOrdine;

@Repository
public interface IStoricoStatoOrdineRepository extends JpaRepository<StoricoStatoOrdine, Long> {

    /** Timeline dell'ordine, dalla creazione all'ultimo cambio. */
    List<StoricoStatoOrdine> findByOrdineIdOrderByCreationDateAsc(Long ordineId);
}