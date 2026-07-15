package com.betacom.mtgbazar.be.model.users;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Voce del carrello: riferimento allo SKU + quantità.
 * Vincolo uq(carrello, sku): la stessa variante compare una sola volta,
 * l'aggiunta ripetuta incrementa la quantità (logica nel service).
 */
@Entity
@Table(name = "voce_carrello", uniqueConstraints = @UniqueConstraint(
        name = "uq_voce_carrello",
        columnNames = { "carrello_id", "sku_id" }))
@Getter
@Setter
@NoArgsConstructor
public class VoceCarrello {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrello_id", nullable = false)
    private Carrello carrello;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private MagazzinoSKU sku;

    @Column(nullable = false)
    private Integer quantita;
}