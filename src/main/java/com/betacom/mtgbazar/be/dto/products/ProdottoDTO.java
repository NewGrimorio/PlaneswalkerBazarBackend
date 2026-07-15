package com.betacom.mtgbazar.be.dto.products;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Prodotto di catalogo. Versione LISTA: solo i campi piani.
 * Versione DETTAGLIO: anche stampa (per i SINGLE), carta e skus.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdottoDTO {
    private Long id;
    private String tipoProdotto;
    private String nome;
    private String slug;
    private String descrizione;
    private String imageUrl;
    private Boolean attivo;

    private Long espansioneId;
    private String espansioneNome;

    // --- solo nel dettaglio ---
    private StampaDTO stampa;           // per i SINGLE
    private CartaDTO carta;             // la carta oracle della stampa
    private List<MagazzinoSKUDTO> skus; // le varianti acquistabili
    
}