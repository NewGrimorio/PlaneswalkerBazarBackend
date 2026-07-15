package com.betacom.mtgbazar.be.model.products;
 
import java.util.UUID;

import com.betacom.mtgbazar.be.model.products.enums.Rarita;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
/**
 * Livello STAMPA: una pubblicazione della carta in un set
 * (numero di collezione, rarità, artista, finiture disponibili).
 * Qui vivono tutti gli ID di integrazione esterna:
 * Scryfall, Gatherer (multiverseId), Cardmarket, Cardtrader.
 */
@Entity
@Table(name = "stampa", uniqueConstraints = @UniqueConstraint(
        name = "uq_stampa_numero",
        columnNames = { "espansione_id", "numero_collezione" }))
@Getter
@Setter
@NoArgsConstructor
public class Stampa {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carta_id", nullable = false)
    private Carta carta;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espansione_id", nullable = false)
    private Espansione espansione;
 
    @Column(name = "numero_collezione", nullable = false, length = 20)
    private String numeroCollezione;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Rarita rarita;
 
    @Column(length = 200)
    private String artista;
 
    @Column(nullable = false)
    private Boolean promo = Boolean.FALSE;
 
    @Column(name = "has_non_foil", nullable = false)
    private Boolean hasNonFoil = Boolean.TRUE;
 
    @Column(name = "has_foil", nullable = false)
    private Boolean hasFoil = Boolean.FALSE;
 
    @Column(name = "has_etched_foil", nullable = false)
    private Boolean hasEtchedFoil = Boolean.FALSE;
 
    @Column(name = "effetti_cornice", length = 300)
    private String effettiCornice;         // CSV: "showcase,extendedart"
    
    @Column(name = "tipi_promo", length = 300)
    private String tipiPromo;              // CSV Scryfall promo_types: "surgefoil,galaxyfoil"
 
    @Column(name = "scryfall_id", unique = true)
    private UUID scryfallId;
 
    @Column(name = "multiverse_id")
    private Integer multiverseId;          // Gatherer
 
    @Column(name = "cardmarket_id")
    private Integer cardmarketId;          // Cardmarket idProduct
 
    @Column(name = "cardtrader_blueprint_id")
    private Integer cardtraderBlueprintId; // Cardtrader blueprint
 
    @Column(name = "image_url", length = 500)
    private String imageUrl;
}