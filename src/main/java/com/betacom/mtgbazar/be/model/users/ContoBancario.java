package com.betacom.mtgbazar.be.model.users;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Conto bancario dell'utente per il ritiro del credito (bonifico in uscita).
 * SOLO soft delete: i movimenti di prelievo lo referenziano per sempre.
 */
@Entity
@Table(name = "conto_bancario")
@Getter
@Setter
@NoArgsConstructor
public class ContoBancario {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;
 
    @Column(nullable = false, length = 200)
    private String intestatario;
 
    @Column(nullable = false, length = 34)
    private String iban;
 
    @Column(length = 11)
    private String bic;
 
    @Column(nullable = false)
    private Boolean attivo = Boolean.TRUE;
 
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;
}