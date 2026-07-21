package com.betacom.mtgbazar.be.repositories.products;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.products.enums.Condizione;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;

import jakarta.persistence.LockModeType;

@Repository
public interface IMagazzinoSKURepository extends JpaRepository<MagazzinoSKU, Long> {

    /** Letture di catalogo, senza lock (derived query, niente properties). */
    List<MagazzinoSKU> findByProdottoIdAndAttivoTrue(Long prodottoId);

    /** Batch anti-N+1: tutti gli SKU di piu' prodotti in una query sola. */
    List<MagazzinoSKU> findByProdottoIdInAndAttivoTrue(Collection<Long> prodottoIds);

    /**
     * Dettaglio con il grafo prodotto/stampa/espansione (JOIN FETCH).
     * Query in META-INF: MagazzinoSKU.findByIdWithProdotto
     */
    Optional<MagazzinoSKU> findByIdWithProdotto(@Param("id") Long id);

    /**
     * Caricamento CON lock esclusivo per il checkout (SELECT ... FOR UPDATE).
     * Query in META-INF: MagazzinoSKU.findByIdInForUpdate
     *
     * L'ORDER BY s.id nella query e' la parte critica: blocca le righe in
     * ordine di id, cosi' due checkout con carrelli sovrapposti chiedono i
     * lock nella stessa sequenza e il secondo si accoda invece di andare
     * in deadlock. REGOLE D'USO:
     *  - chiamare SOLO dentro @Transactional
     *  - verificare la disponibilita' DOPO il lock, decrementare in memoria
     *  - ordine tra tabelle: SEMPRE prima magazzino_sku, poi portafoglio
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MagazzinoSKU> findByIdInForUpdate(@Param("ids") Collection<Long> ids);

    List<MagazzinoSKU> findByProdottoId(Long prodottoId);

    boolean existsByProdottoIdAndCondizioneAndLinguaAndFinitura(
            Long prodottoId, Condizione condizione, String lingua, Finitura finitura);

    /** Guard non-SINGLE: un prodotto commerciale ha al massimo uno SKU. */
    boolean existsByProdottoId(Long prodottoId);

    // ------------------------------------------------------------------
    // Dashboard "sotto scorta" — ESCLUSE le carte singole
    // ------------------------------------------------------------------

    /**
     * Contatore sotto scorta, escluso un tipo (SINGLE): la giacenza bassa
     * e' fisiologica per le singole, contarle sarebbe rumore.
     * Query in META-INF: MagazzinoSKU.countSottoScortaEsclusoTipo
     */
    long countSottoScortaEsclusoTipo(@Param("soglia") int soglia,
                                     @Param("tipoEscluso") TipoProdotto tipoEscluso);

    /**
     * Lista sotto scorta (col prodotto fetchato), escluso un tipo, dal
     * piu' urgente. Query in META-INF: MagazzinoSKU.sottoScortaEsclusoTipo
     */
    List<MagazzinoSKU> sottoScortaEsclusoTipo(@Param("soglia") int soglia,
                                              @Param("tipoEscluso") TipoProdotto tipoEscluso);
    
}