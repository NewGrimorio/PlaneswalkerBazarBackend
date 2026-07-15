package com.betacom.mtgbazar.be.model.users;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.betacom.mtgbazar.be.model.users.enums.StatOrdine;

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
 * Ordine: SNAPSHOTTA tutto al checkout (indirizzo di spedizione,
 * prezzi nelle righe). Le modifiche successive a indirizzi/prezzi
 * non alterano mai lo storico ordini.
 * State machine sugli 8 valori di StatOrdine, con transizioni validate
 * nel service (pattern caricaEValidaStato / caricaEValidaStatoOwner).
 */
@Entity
@Table(name = "ordine")
@Getter
@Setter
@NoArgsConstructor
public class Ordine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatOrdine stato = StatOrdine.CREATO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totale;

    @Column(name = "spese_spedizione", nullable = false, precision = 10, scale = 2)
    private BigDecimal speseSpedizione = BigDecimal.ZERO;

    // --- Snapshot indirizzo di consegna ---

    @Column(name = "sped_destinatario", nullable = false, length = 200)
    private String spedDestinatario;

    @Column(name = "sped_via", nullable = false, length = 200)
    private String spedVia;

    @Column(name = "sped_civico", nullable = false, length = 20)
    private String spedCivico;

    @Column(name = "sped_cap", nullable = false, length = 10)
    private String spedCap;

    @Column(name = "sped_citta", nullable = false, length = 100)
    private String spedCitta;

    @Column(name = "sped_provincia", length = 50)
    private String spedProvincia;

    @Column(name = "sped_nazione", nullable = false, length = 2)
    private String spedNazione = "IT";

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;
}