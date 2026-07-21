package com.betacom.mtgbazar.be.repositories.users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.dto.users.RecensioneStatisticheDTO;
import com.betacom.mtgbazar.be.model.users.Recensione;
import com.betacom.mtgbazar.be.model.users.enums.StatoRecensione;

@Repository
public interface IRecensioneRepository extends JpaRepository<Recensione, Long> {
 
    /** Le recensioni visibili nella pagina prodotto, dalla piu' recente. */
    List<Recensione> findByProdottoIdAndStatoOrderByCreationDateDesc(
            Long prodottoId, StatoRecensione stato);
 
    /** Una sola recensione per (utente, prodotto): lookup per modifica. */
    Optional<Recensione> findByUtenteIdAndProdottoId(Long utenteId, Long prodottoId);
 
    /**
     * Media e conteggio in una query sola, TIPIZZATA: la named query usa
     * la constructor expression JPQL (SELECT new ...DTO(AVG, COUNT)).
     * Query in META-INF: Recensione.statisticheByProdotto
     */
    RecensioneStatisticheDTO statisticheByProdotto(@Param("prodottoId") Long prodottoId,
                                                   @Param("stato") StatoRecensione stato);
 
    /** Pannello admin: recensioni in coda di moderazione. */
    List<Recensione> findByStatoOrderByCreationDateAsc(StatoRecensione stato);
    
}