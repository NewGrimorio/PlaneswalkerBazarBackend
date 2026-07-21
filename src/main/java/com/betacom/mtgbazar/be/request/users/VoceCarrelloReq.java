package com.betacom.mtgbazar.be.request.users;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Add-to-cart / modifica quantita' (erede di ArtCarrelloReq Fase 2,
 * ma sul nuovo modello SKU). Se la variante e' gia' nel carrello il
 * service incrementa; quantita = 0 in modifica significa rimozione.
 */
@Getter
@Setter
@ToString
public class VoceCarrelloReq {

	/** FASE C: valorizzato dal controller dal token, mai dal client. */
    private Long utenteId;

    @NotNull(message = "carrello.no.sku")
    private Long skuId;

    @NotNull(message = "carrello.no.quantita")
    @Min(value = 1, message = "carrello.quantita.min")
    private Integer quantita;
    
}