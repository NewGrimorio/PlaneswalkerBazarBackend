package com.betacom.mtgbazar.be.request.products;

import java.time.LocalDate;

import com.betacom.mtgbazar.be.request.ValidationGroups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Creazione/modifica MANUALE di un set (i set arriveranno di norma
 * dal sync Scryfall: questa Req serve per casi particolari e correzioni).
 */
@Getter
@Setter
@ToString
public class EspansioneReq {

    @NotNull(groups = ValidationGroups.Update.class, message = "espansione.no.id")
    private Long id;

    @NotNull(groups = ValidationGroups.Create.class, message = "espansione.no.codice")
    @NotBlank(groups = ValidationGroups.Create.class, message = "espansione.no.codice")
    @Size(max = 10, message = "espansione.codice.maxlength")
    private String codice;

    @NotNull(groups = ValidationGroups.Create.class, message = "espansione.no.nome")
    @NotBlank(groups = ValidationGroups.Create.class, message = "espansione.no.nome")
    @Size(max = 200, message = "espansione.nome.maxlength")
    private String nome;

    @NotNull(groups = ValidationGroups.Create.class, message = "espansione.no.tipo")
    @NotBlank(groups = ValidationGroups.Create.class, message = "espansione.no.tipo")
    @Size(max = 30, message = "espansione.tipo.maxlength")
    private String tipoSet;               // valori Scryfall (expansion, box...)

    @Size(max = 10, message = "espansione.codice.maxlength")
    private String codiceSetPadre;

    private LocalDate dataUscita;

    @Size(max = 500, message = "espansione.url.maxlength")
    private String iconUrl;

    private Integer numeroCarte;
    
}