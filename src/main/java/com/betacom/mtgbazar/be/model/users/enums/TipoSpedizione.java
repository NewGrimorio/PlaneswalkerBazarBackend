package com.betacom.mtgbazar.be.model.users.enums;

import java.math.BigDecimal;

/**
 * Metodi di spedizione offerti, con la loro tariffa.
 *
 * REGOLA COMMERCIALE: sopra i 70 euro di merce la spedizione e' OFFERTA
 * ed e' EXPRESS — il metodo scelto dal cliente viene ignorato, perche'
 * non ha senso spedire piu' lentamente qualcosa che paghiamo noi.
 *
 * Il costo e' un dato sul valore dell'enum; la soglia invece e' una
 * costante unica, perche' e' una promozione trasversale e non una
 * proprieta' del singolo metodo.
 *
 * NB: il costo applicato viene SNAPSHOTTATO su ordine.spese_spedizione
 * al checkout: ritoccare le tariffe non riscrive gli ordini passati.
 */
public enum TipoSpedizione {

    STANDARD("Standard", "4-7 giorni lavorativi", new BigDecimal("4.90")),
    EXPRESS ("Express",  "1-2 giorni lavorativi", new BigDecimal("7.90"));

    /** Imponibile MERCE (spedizione esclusa) oltre il quale si spedisce gratis. */
    public static final BigDecimal SOGLIA_GRATUITA = new BigDecimal("70.00");

    private final String etichetta;
    private final String tempi;
    private final BigDecimal costo;

    TipoSpedizione(String etichetta, String tempi, BigDecimal costo) {
        this.etichetta = etichetta;
        this.tempi = tempi;
        this.costo = costo;
    }

    public String etichetta() { return etichetta; }
    public String tempi()     { return tempi; }
    public BigDecimal costo() { return costo; }

    /** True se l'imponibile merce fa scattare la promozione. */
    public static boolean sopraSoglia(BigDecimal totaleMerce) {
        return totaleMerce != null && totaleMerce.compareTo(SOGLIA_GRATUITA) >= 0;
    }

    /**
     * UNICO punto in cui la regola vive: dato il metodo scelto e
     * l'imponibile, restituisce il metodo APPLICATO e il costo.
     * Lo usano sia il checkout (per addebitare e snapshottare) sia il
     * carrello (per l'anteprima): i due numeri non possono divergere.
     */
    public static Esito applica(TipoSpedizione scelto, BigDecimal totaleMerce) {
        if (sopraSoglia(totaleMerce))
            return new Esito(EXPRESS, BigDecimal.ZERO);      // offerta, e veloce
        TipoSpedizione t = (scelto == null) ? STANDARD : scelto;
        return new Esito(t, t.costo);
    }

    /** Metodo effettivamente applicato e costo addebitato. */
    public record Esito(TipoSpedizione tipo, BigDecimal costo) {
    	
    }
}