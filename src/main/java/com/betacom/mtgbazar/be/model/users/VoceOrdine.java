package com.betacom.mtgbazar.be.model.users;

import java.math.BigDecimal;

import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;

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
 * Voce d'ordine con SNAPSHOT di descrizione e prezzo unitario:
 * il riferimento allo SKU serve per tracciabilità/reso, ma i dati
 * mostrati all'utente vengono sempre dallo snapshot, mai dallo SKU vivo.
 */
@Entity
@Table(name = "voce_ordine")
@Getter
@Setter
@NoArgsConstructor
public class VoceOrdine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordine_id", nullable = false)
    private Ordine ordine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private MagazzinoSKU sku;

    @Column(nullable = false, length = 400)
    private String descrizione;            // snapshot leggibile

    @Column(name = "prezzo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoUnitario;     // snapshot prezzo

    @Column(nullable = false)
    private Integer quantita;
}