package com.betacom.mtgbazar.be.repositories.products;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;

@Repository
public interface IProdottoRepository extends JpaRepository<Prodotto, Long> {

    /** Lista di catalogo per categoria (il menu del sito). */
    List<Prodotto> findByTipoProdottoAndAttivoTrue(TipoProdotto tipoProdotto);

    /** Lista dei prodotti di un'espansione (pagina espansione). */
    List<Prodotto> findByEspansioneIdAndAttivoTrue(Long espansioneId);

    /**
     * Dettaglio prodotto per slug con il grafo completo caricato in una
     * query sola (stampa -> carta, espansione).
     * Query in META-INF: Prodotto.findBySlugWithDettagli
     */
    Optional<Prodotto> findBySlugWithDettagli(@Param("slug") String slug);

    /** Guardiano dell'unicita' dello slug (generazione e update). */
    boolean existsBySlug(String slug);

    /** Il prodotto SINGLE della stampa esiste gia'? (guardia del sync) */
    boolean existsByStampaId(Long stampaId);

    /**
     * Ricerca per nome, case-insensitive.
     * Query in META-INF: Prodotto.searchByNome
     */
    List<Prodotto> searchByNome(@Param("testo") String testo);
    
    /** Lista ADMIN per categoria: include i disattivati (il negozio usa la variante AndAttivoTrue). */
    List<Prodotto> findByTipoProdotto(TipoProdotto tipoProdotto);
    
    /** Ricerca ADMIN: include i disattivati. Query in META-INF: Prodotto.searchByNomeAdmin */
    List<Prodotto> searchByNomeAdmin(@Param("testo") String testo);
    
    /** Sfoglia ADMIN: prodotti di un'espansione per tipo, inclusi i disattivati. */
    List<Prodotto> findByEspansioneIdAndTipoProdottoOrderByNomeAsc(
            Long espansioneId, TipoProdotto tipoProdotto);
    
    Optional<Prodotto> findByStampaId(Long stampaId);
    
}