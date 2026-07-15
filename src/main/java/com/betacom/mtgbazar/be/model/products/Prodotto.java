package com.betacom.mtgbazar.be.model.products;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Entità commerciale di catalogo, mostrata sul sito.
 *
 * Invariante (vincolo chk_single_stampa a livello DB):
 *   SINGLE      -> stampa obbligatoria
 *   altri tipi  -> stampa sempre null (espansione facoltativa)
 *   ACCESSORIO  -> di norma nessun riferimento
 */

@Entity
@Table(name = "prodotto")
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class Prodotto {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoProdotto tipoProdotto;
 
    @Column(nullable = false, length = 300)
    private String nome;
    
    //Identificatore di un prodotto dentro un url
    //www.planeswalkerbazar.it/prodotto/commander-masters-booster-box
    //commander-masters-booster-box è uno slug
    @Column(nullable = false, unique = true, length = 300)
    private String slug;
 
    @Column(columnDefinition = "TEXT")
    private String descrizione;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espansione_id")
    private Espansione espansione;
 
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stampa_id", unique = true)
    private Stampa stampa;                 // solo per tipo = SINGLE
 
    @Column(name = "image_url", length = 500)
    private String imageUrl;
 
    @Column(nullable = false)
    private Boolean attivo = Boolean.TRUE;
 
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;
 
    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;
	
}
