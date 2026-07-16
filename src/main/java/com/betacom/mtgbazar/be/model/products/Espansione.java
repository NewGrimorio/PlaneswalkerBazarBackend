package com.betacom.mtgbazar.be.model.products;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Set/espansione, modellata sui dati Scryfall.
 * tipoSet usa i valori testuali Scryfall (expansion, core, masters,
 * commander, box, promo, token...). Secret Lair (SLD) ha tipoSet = "box".
 */
@Entity
@Table(name = "espansione")
@Getter
@Setter
@NoArgsConstructor
public class Espansione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codice;                 // es. "MH3", "SLD"

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(name = "tipo_set", nullable = false, length = 30)
    private String tipoSet;

    @Column(name = "codice_set_padre", length = 10)
    private String codiceSetPadre;         // set figli: token, promo...

    @Column(name = "data_uscita")
    private LocalDate dataUscita;

    @Column(name = "scryfall_id", unique = true)
    private UUID scryfallId;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "numero_carte")
    private Integer numeroCarte;
    
    @Column(name = "data_ultima_sincronizzazione")
    private LocalDateTime dataUltimaSincronizzazione;
}