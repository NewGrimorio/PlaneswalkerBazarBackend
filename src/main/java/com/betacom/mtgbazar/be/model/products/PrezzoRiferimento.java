package com.betacom.mtgbazar.be.model.products;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.betacom.mtgbazar.be.model.products.enums.Condizione;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;
import com.betacom.mtgbazar.be.model.products.enums.FontePrezzo;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Prezzo guida rilevato da una fonte esterna per (stampa, finitura).
 * Tabella append-only, storicizzata: mai UPDATE, solo nuovi rilevamenti.
 * Il prezzo più recente è MAX(rilevatoIl) per (stampa, fonte, finitura).
 */
@Entity
@Table(name = "prezzo_riferimento")
@Getter
@Setter
@NoArgsConstructor
public class PrezzoRiferimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stampa_id", nullable = false)
    private Stampa stampa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FontePrezzo fonte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Finitura finitura = Finitura.NONFOIL;

    @Column(name = "prezzo_trend", precision = 10, scale = 2)
    private BigDecimal prezzoTrend;

    @Column(name = "prezzo_medio", precision = 10, scale = 2)
    private BigDecimal prezzoMedio;

    @Column(name = "prezzo_min", precision = 10, scale = 2)
    private BigDecimal prezzoMin;

    @Column(nullable = false, length = 3)
    private String valuta = "EUR";

    @CreationTimestamp
    @Column(name = "detection_date", nullable = false, updatable = false)
    private LocalDateTime detectionDate; //Data Rivelamento
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Condizione condizione = Condizione.NA;

    @Column(nullable = false, length = 2)
    private String lingua = "NA";          // 'NA' = finish-level (Cardmarket/Scryfall)
    
}