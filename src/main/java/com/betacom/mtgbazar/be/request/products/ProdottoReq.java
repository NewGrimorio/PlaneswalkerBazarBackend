package com.betacom.mtgbazar.be.request.products;

import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.request.ValidationGroups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Prodotti creati a mano dall'admin: sigillato, accessori, lotti.
 * I prodotti SINGLE nascono dal sync Scryfall (stampaId valorizzato
 * dal servizio di sincronizzazione, non da questo form).
 * Lo slug, se assente, lo genera il service dal nome.
 */
@Getter
@Setter
@ToString
public class ProdottoReq {

    @NotNull(groups = ValidationGroups.Update.class, message = "prodotto.no.id")
    private Long id;

    @NotNull(groups = ValidationGroups.Create.class, message = "prodotto.no.tipo")
    private TipoProdotto tipoProdotto;

    @NotNull(groups = ValidationGroups.Create.class, message = "prodotto.no.nome")
    @NotBlank(groups = ValidationGroups.Create.class, message = "prodotto.no.nome")
    @Size(max = 300, message = "prodotto.nome.maxlength")
    private String nome;

    @Size(max = 300, message = "prodotto.slug.maxlength")
    private String slug;                  // opzionale: generato dal nome

    private String descrizione;

    private Long espansioneId;            // per booster/box/sigillato

    @Size(max = 500, message = "prodotto.url.maxlength")
    private String imageUrl;

    private Boolean attivo;               // per attivare/disattivare in update
    
}