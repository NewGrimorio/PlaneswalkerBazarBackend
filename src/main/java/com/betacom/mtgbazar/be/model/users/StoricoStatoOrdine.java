package com.betacom.mtgbazar.be.model.users;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
 * Audit trail dei cambi di stato dell'ordine: chi (eseguitoDa),
 * quando, da quale stato a quale stato, con nota facoltativa.
 * Append-only: una riga per ogni transizione, mai UPDATE.
 */
@Entity
@Table(name = "storico_stato_ordine")
@Getter
@Setter
@NoArgsConstructor
public class StoricoStatoOrdine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordine_id", nullable = false)
    private Ordine ordine;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato_da", length = 20)
    private StatOrdine statoDa;            // null per la creazione

    @Enumerated(EnumType.STRING)
    @Column(name = "stato_a", nullable = false, length = 20)
    private StatOrdine statoA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eseguito_da")
    private Utente eseguitoDa;

    @Column(length = 300)
    private String nota;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;
}