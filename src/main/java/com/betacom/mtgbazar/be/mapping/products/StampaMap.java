package com.betacom.mtgbazar.be.mapping.products;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.products.StampaDTO;
import com.betacom.mtgbazar.be.model.products.Stampa;

public class StampaMap {

    /*
     * PRE-REQUISITO: carta ed espansione gia' fetchate dal service
     * (query Stampa.findByCartaIdWithEspansione o caricamento nel
     * grafo del prodotto) — qui si attraversano per nome e codice.
     */
    public static StampaDTO buildStampaDTO(Stampa s) {
        return StampaDTO.builder()
                .id(s.getId())
                .cartaId(s.getCarta().getId())
                .cartaNome(s.getCarta().getNome())
                .espansioneId(s.getEspansione().getId())
                .espansioneCodice(s.getEspansione().getCodice())
                .espansioneNome(s.getEspansione().getNome())
                .numeroCollezione(s.getNumeroCollezione())
                .rarita(s.getRarita().name())
                .artista(s.getArtista())
                .promo(s.getPromo())
                .hasNonFoil(s.getHasNonFoil())
                .hasFoil(s.getHasFoil())
                .hasEtchedFoil(s.getHasEtchedFoil())
                .effettiCornice(s.getEffettiCornice())
                .tipiPromo(s.getTipiPromo())
                .imageUrl(s.getImageUrl())
                .build();
    }

    public static List<StampaDTO> buildStampaDTOList(Collection<Stampa> lS) {
        return lS.stream()
                .map(s -> buildStampaDTO(s))
                .toList();
    }
    
}