package com.betacom.mtgbazar.be.model.products;

import java.math.BigDecimal;
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
 * Livello ORACLE: la carta come concetto di gioco, unica per oracleId.
 * "Lightning Bolt" esiste una sola volta qui, anche se stampato 40 volte.
 *
 * colori / identitaColore: sottoinsieme ordinato di "WUBRG" (es. "WU", "").
 * legalita / facce: JSON serializzato (Jackson) in colonne TEXT.
 */

@Entity
@Table(name = "carta")
@Getter
@Setter
@NoArgsConstructor
public class Carta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oracle_id", nullable = false, unique = true)
    private UUID oracleId;

    @Column(nullable = false, length = 300)
    private String nome;

    @Column(name = "costo_mana", length = 80)
    private String costoMana;              // es. "{1}{U}{U}"

    @Column(name = "valore_mana", precision = 4, scale = 1)
    private BigDecimal valoreMana;         // mezzi punti per gli Un-set

    @Column(name = "tipo_riga", length = 200)
    private String tipoRiga;               // "Creature — Human Wizard"

    @Column(name = "testo_oracle", columnDefinition = "TEXT")
    private String testoOracle;

    @Column(length = 10)
    private String forza;                  // String: esistono "*", "1+*"

    @Column(length = 10)
    private String costituzione;

    @Column(nullable = false, length = 5)
    private String colori = "";

    @Column(name = "identita_colore", nullable = false, length = 5)
    private String identitaColore = "";

    @Column(name = "parole_chiave", length = 500)
    private String paroleChiave;           // CSV: "Flying,Trample"

    @Column(columnDefinition = "TEXT")
    private String legal;               // JSON {"standard":"legal",...}

    @Column(columnDefinition = "TEXT")
    private String cardFaces;              // JSON card_faces (bifronte/MDFC)
}