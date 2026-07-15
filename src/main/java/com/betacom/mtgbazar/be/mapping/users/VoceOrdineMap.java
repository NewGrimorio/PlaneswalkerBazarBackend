package com.betacom.mtgbazar.be.mapping.users;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.VoceOrdineDTO;
import com.betacom.mtgbazar.be.model.users.VoceOrdine;

public class VoceOrdineMap {

    /*
     * NESSUN accesso allo SKU vivo (pattern ArtOrdineMap Fase 2):
     * descrizione e prezzo sono gli SNAPSHOT salvati al checkout.
     * Il documento storico si legge cosi' com'e', mai ricalcolato.
     * skuId (solo id, LAZY-safe) resta per tracciabilita'/reso.
     */
    public static VoceOrdineDTO buildVoceOrdineDTO(VoceOrdine v) {
        return VoceOrdineDTO.builder()
                .id(v.getId())
                .skuId(v.getSku() == null ? null : v.getSku().getId())
                .descrizione(v.getDescrizione())
                .prezzoUnitario(v.getPrezzoUnitario())
                .quantita(v.getQuantita())
                .subtotale(v.getPrezzoUnitario()
                        .multiply(BigDecimal.valueOf(v.getQuantita())))
                .build();
    }

    public static List<VoceOrdineDTO> buildVoceOrdineDTOList(Collection<VoceOrdine> lV) {
        return lV.stream()
                .map(v -> buildVoceOrdineDTO(v))
                .toList();
    }
}