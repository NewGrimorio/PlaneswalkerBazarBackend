package com.betacom.mtgbazar.be.mapping.products;


import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.products.ProdottoDTO;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.products.Prodotto;

public class ProdottoMap {

    /**
     * Versione per le LISTE: campi piani + riferimento all'espansione.
     * PRE-REQUISITO: espansione fetchata o null (per gli ACCESSORI);
     * qui si leggono solo id e nome.
     */
    public static ProdottoDTO buildProdottoDTO(Prodotto p) {
        return builderComune(p).build();
    }

    /**
     * Versione per il DETTAGLIO: con stampa/carta (per i SINGLE) e
     * le varianti acquistabili. PRE-REQUISITO: grafo completo fetchato
     * (query Prodotto.findBySlugWithDettagli) + skus dal repository.
     */
    public static ProdottoDTO buildProdottoDTOWithDettagli(Prodotto p, List<MagazzinoSKU> skus) {
        ProdottoDTO.ProdottoDTOBuilder builder = builderComune(p)
                .skus(MagazzinoSKUMap.buildMagazzinoSKUDTOList(skus));
        if (p.getStampa() != null) {
            builder.stampa(StampaMap.buildStampaDTO(p.getStampa()))
                   .carta(CartaMap.buildCartaDTO(p.getStampa().getCarta()));
        }
        return builder.build();
    }

    public static List<ProdottoDTO> buildProdottoDTOList(Collection<Prodotto> lP) {
        return lP.stream()
                .map(p -> buildProdottoDTO(p))
                .toList();
    }

    private static ProdottoDTO.ProdottoDTOBuilder builderComune(Prodotto p) {
        return ProdottoDTO.builder()
                .id(p.getId())
                .tipoProdotto(p.getTipoProdotto().name())
                .nome(p.getNome())
                .slug(p.getSlug())
                .descrizione(p.getDescrizione())
                .imageUrl(p.getImageUrl())
                .attivo(p.getAttivo())
                .espansioneId(p.getEspansione() == null ? null : p.getEspansione().getId())
                .espansioneNome(p.getEspansione() == null ? null : p.getEspansione().getNome());
    }
    
}
