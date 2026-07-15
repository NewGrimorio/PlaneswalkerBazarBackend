package com.betacom.mtgbazar.be.repositories.users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.VoceCarrello;

@Repository
public interface IVoceCarrelloRepository extends JpaRepository<VoceCarrello, Long> {

    /**
     * Le voci del carrello con SKU e prodotto gia' caricati (una query,
     * anti-N+1): serve per mostrare il carrello coi prezzi live.
     * Query in META-INF: VoceCarrello.findByCarrelloIdWithSku
     */
    List<VoceCarrello> findByCarrelloIdWithSku(@Param("carrelloId") Long carrelloId);

    /** Per l'add-to-cart: se la variante c'e' gia', si incrementa. */
    Optional<VoceCarrello> findByCarrelloIdAndSkuId(Long carrelloId, Long skuId);

    /** Svuotamento dopo il checkout. */
    @Modifying
    void deleteByCarrelloId(Long carrelloId);
}