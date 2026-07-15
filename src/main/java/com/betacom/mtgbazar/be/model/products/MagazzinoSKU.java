package com.betacom.mtgbazar.be.model.products;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import com.betacom.mtgbazar.be.model.products.enums.Condizione;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;

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
 * SKU (Stock Keeping Unit): l'unità effettivamente vendibile e contabile
 * a magazzino — variante + prezzo + giacenza.
 * Per i SINGLE la variante è (condizione, lingua, finitura);
 * per i non-single condizione = Condizione.NA (sentinella, mai null).
 *
 * ATTENZIONE concorrenza: quantita va decrementata SOLO dentro una
 * transazione con @Lock(LockModeType.PESSIMISTIC_WRITE) sulla query di
 * caricamento, ordinando gli id prima dell'acquisizione (pattern Fase 2).
 */
@Entity
@Table(name = "magazzino_sku", uniqueConstraints = @UniqueConstraint(
        name = "uq_magazzino_sku_var",
        columnNames = { "prodotto_id", "condizione", "lingua", "finitura" }))
@Getter
@Setter
@NoArgsConstructor
public class MagazzinoSKU {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prodotto_id", nullable = false)
    private Prodotto prodotto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Condizione condizione = Condizione.NA;

    @Column(nullable = false, length = 2)
    private String lingua = "en";          // ISO 639-1

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Finitura finitura = Finitura.NONFOIL;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzo;

    @Column(nullable = false)
    private Integer quantita = 0;

    @Column(nullable = false)
    private Boolean attivo = Boolean.TRUE;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;
    
}