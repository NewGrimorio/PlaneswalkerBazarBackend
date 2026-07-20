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
 * Refresh token persistito (V11). Qui vive SOLO l'hash SHA-256: il token
 * in chiaro esiste un'unica volta, nella risposta HTTP che imposta il
 * cookie. 'famiglia' lega i token della stessa catena di rotazione:
 * il riuso di un token gia' revocato brucia l'intera famiglia.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 36)
    private String famiglia;

    @Column(nullable = false)
    private LocalDateTime scadenza;

    @Column(nullable = false)
    private Boolean revocato = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "user_agent", length = 255)
    private String userAgent;
    
}