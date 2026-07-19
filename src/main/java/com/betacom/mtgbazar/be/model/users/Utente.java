package com.betacom.mtgbazar.be.model.users;


import java.time.LocalDate;
import java.time.LocalDateTime;
 
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.betacom.mtgbazar.be.model.users.enums.RuoloUtente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Utente della piattaforma (ADMIN o CLIENTE).
 * L'email va normalizzata (trim + lowercase) nel service PRIMA di ogni
 * salvataggio e di ogni ricerca: è l'identificativo di login.
 * La password è salvata come hash BCrypt (spring-security-crypto).
 *
 * indirizzoPredefinito: FK verso Indirizzo — garantisce strutturalmente
 * "al massimo un predefinito per utente" (portabile PG/H2, niente indici
 * parziali). Se si disattiva quell'indirizzo, azzerare prima questo campo.
 */
@Entity
@Table(name = "utente")
@Getter
@Setter
@NoArgsConstructor
public class Utente {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    
    @Column(nullable = false, unique = true, length = 30)
    private String username;
 
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RuoloUtente ruolo = RuoloUtente.CLIENTE;
 
    @Column(length = 100)
    private String nome;
 
    @Column(length = 100)
    private String cognome;
 
    @Column(length = 30)
    private String telefono;
 
    @Column(name = "data_nascita")
    private LocalDate dataNascita;
 
    @Column(name = "codice_fiscale", unique = true, length = 16)
    private String codiceFiscale;
 
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indirizzo_predefinito_id")
    private Indirizzo indirizzoPredefinito;
 
    @CreationTimestamp
    @Column(name = "data_registrazione", nullable = false, updatable = false)
    private LocalDateTime dataRegistrazione;
 
    @Column(nullable = false)
    private Boolean attivo = Boolean.TRUE;
 
    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;
    
    @Column(name = "immagine_profilo", length = 500)
    private String immagineProfilo;
    
}
 