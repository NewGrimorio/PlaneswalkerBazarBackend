package com.betacom.mtgbazar.be.model.users;


import java.math.BigDecimal;
import java.time.LocalDateTime;
 
import org.hibernate.annotations.UpdateTimestamp;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
/**
 * Portafoglio 1-1 con l'utente. Il saldo è denormalizzato per performance,
 * ma deve SEMPRE essere riconciliabile con la somma dei movimenti
 * COMPLETATO del ledger (MovimentoPortafoglio).
 *
 * ATTENZIONE concorrenza: il saldo va modificato SOLO dentro una
 * transazione con @Lock(LockModeType.PESSIMISTIC_WRITE) sulla query di
 * caricamento del portafoglio. Il CHECK saldo >= 0 a livello DB è
 * l'ultima linea di difesa contro i saldi negativi.
 */
@Entity
@Table(name = "portafoglio")
@Getter
@Setter
@NoArgsConstructor
public class Portafoglio {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false, unique = true)
    private Utente utente;
 
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;
 
    @Column(nullable = false, length = 3)
    private String valuta = "EUR";
 
    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;
}