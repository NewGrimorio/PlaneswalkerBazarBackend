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
 * Indirizzo di consegna (multipli per utente).
 * SOLO soft delete (attivo = false): gli indirizzi non si cancellano mai
 * fisicamente. L'ordine ne salva comunque uno snapshot, quindi lo storico
 * ordini non dipende da questa tabella.
 */
@Entity
@Table(name = "indirizzo")
@Getter
@Setter
@NoArgsConstructor
public class Indirizzo {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;
 
    @Column(length = 50)
    private String etichetta;              // "Casa", "Ufficio"...
 
    @Column(nullable = false, length = 200)
    private String destinatario;           // nome sul pacco
 
    @Column(nullable = false, length = 200)
    private String via;
 
    @Column(nullable = false, length = 20)
    private String civico;
 
    @Column(nullable = false, length = 10)
    private String cap;
 
    @Column(nullable = false, length = 100)
    private String citta;
 
    @Column(length = 50)
    private String provincia;
 
    @Column(nullable = false, length = 2)
    private String nazione = "IT";         // ISO 3166-1 alpha-2
 
    @Column(nullable = false)
    private Boolean attivo = Boolean.TRUE;
 
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;
}
 
