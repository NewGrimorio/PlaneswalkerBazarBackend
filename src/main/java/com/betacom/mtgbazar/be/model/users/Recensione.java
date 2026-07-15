package com.betacom.mtgbazar.be.model.users;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.users.enums.StatoRecensione;

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
 * Recensione di un prodotto ("acquisto verificato").
 * REGOLE nel service, non esprimibili come CHECK:
 *   - l'ordine referenziato deve appartenere all'utente
 *   - deve essere in stato CONSEGNATO
 *   - deve contenere almeno uno SKU del prodotto recensito
 * Una sola recensione per (utente, prodotto): il riacquisto non ne
 * crea una seconda, l'utente puo' modificare la propria (update_date).
 */
@Entity
@Table(name = "recensione", uniqueConstraints = @UniqueConstraint(
        name = "uq_recensione_utente_prodotto",
        columnNames = { "utente_id", "prodotto_id" }))
@Getter
@Setter
@NoArgsConstructor
public class Recensione {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prodotto_id", nullable = false)
    private Prodotto prodotto;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordine_id", nullable = false)
    private Ordine ordine;                 // l'ordine consegnato che la giustifica
 
    @Column(nullable = false)
    private Short voto;                    // 1..5, CHECK a livello DB
 
    @Column(length = 150)
    private String titolo;
 
    @Column(columnDefinition = "TEXT")
    private String testo;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatoRecensione stato = StatoRecensione.APPROVATA;
 
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;
 
    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;
}