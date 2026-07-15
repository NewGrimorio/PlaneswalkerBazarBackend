package com.betacom.mtgbazar.be.request.products;

import java.math.BigDecimal;

import com.betacom.mtgbazar.be.model.products.enums.Condizione;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;
import com.betacom.mtgbazar.be.request.ValidationGroups;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Gestione magazzino admin (erede di MagazzinoReq Fase 2):
 * crea la variante vendibile di un prodotto o ne aggiorna
 * prezzo/giacenza. Condizione/lingua/finitura identificano la
 * variante e NON si cambiano in update: variante nuova = SKU nuovo.
 */
@Getter
@Setter
@ToString
public class MagazzinoSKUReq {

    @NotNull(groups = ValidationGroups.Update.class, message = "sku.no.id")
    private Long id;

    @NotNull(groups = ValidationGroups.Create.class, message = "sku.no.prodotto")
    private Long prodottoId;

    private Condizione condizione;        // default NA nel service (non-single)

    @Pattern(regexp = "^[a-zA-Z]{2}$", message = "sku.lingua.invalid")
    private String lingua;                // default "en" nel service

    private Finitura finitura;            // default NONFOIL nel service

    @NotNull(groups = ValidationGroups.Create.class, message = "sku.no.prezzo")
    @DecimalMin(value = "0.00", message = "sku.prezzo.ngt")
    @Digits(integer = 8, fraction = 2, message = "sku.prezzo.invalid")
    private BigDecimal prezzo;

    @NotNull(groups = ValidationGroups.Create.class, message = "sku.no.quantita")
    @PositiveOrZero(message = "sku.quantita.ngt")
    private Integer quantita;

    private Boolean attivo;
    
}
