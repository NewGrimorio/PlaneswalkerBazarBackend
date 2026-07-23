package com.betacom.mtgbazar.be.mapping.users;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.OrdineDTO;
import com.betacom.mtgbazar.be.model.users.Ordine;
import com.betacom.mtgbazar.be.model.users.VoceOrdine;

public class OrdineMap {

    /** Versione per le LISTE: senza voci (restano null nel DTO). */
    public static OrdineDTO buildOrdineDTO(Ordine o) {
        return builderComune(o).build();
    }

    /** Versione per il DETTAGLIO: con le voci caricate dal service. */
    public static OrdineDTO buildOrdineDTOWithVoci(Ordine o, List<VoceOrdine> voci) {
        return builderComune(o)
                .voci(VoceOrdineMap.buildVoceOrdineDTOList(voci))
                .build();
    }

    public static List<OrdineDTO> buildOrdineDTOList(Collection<Ordine> lO) {
        return lO.stream()
                .map(o -> buildOrdineDTO(o))
                .toList();
    }

    /* Campi comuni alle due versioni: l'indirizzo e' lo SNAPSHOT sped_*. */
    private static OrdineDTO.OrdineDTOBuilder builderComune(Ordine o) {
        return OrdineDTO.builder()
                .id(o.getId())
                .stato(o.getStato().name())
                .totale(o.getTotale())
                .speseSpedizione(o.getSpeseSpedizione())
                .tipoSpedizione(o.getTipoSpedizione().name())
                .spedDestinatario(o.getSpedDestinatario())
                .spedVia(o.getSpedVia())
                .spedCivico(o.getSpedCivico())
                .spedCap(o.getSpedCap())
                .spedCitta(o.getSpedCitta())
                .spedProvincia(o.getSpedProvincia())
                .spedNazione(o.getSpedNazione())
                .creationDate(o.getCreationDate())
                .updateDate(o.getUpdateDate());
    }
    
}