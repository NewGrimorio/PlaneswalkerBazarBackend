package com.betacom.mtgbazar.be.model.users;

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
 * Carrello persistente, uno per utente (creato pigramente al primo
 * "aggiungi al carrello"). I prezzi NON si salvano qui: nel carrello
 * sono sempre live dallo SKU; lo snapshot avviene solo al checkout.
 */
@Entity
@Table(name = "carrello")
@Getter
@Setter
@NoArgsConstructor
public class Carrello {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false, unique = true)
    private Utente utente;
 
    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;
}