package com.betacom.mtgbazar.be.mapping.users;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.StoricoStatoOrdineDTO;
import com.betacom.mtgbazar.be.model.users.StoricoStatoOrdine;

public class StoricoStatoOrdineMap {

    /*
     * PRE-REQUISITO: eseguitoDa fetchato (o null) — il nome
     * visualizzabile passa da UtenteMap.nomeVisualizzabile,
     * che gestisce anche il caso "Sistema".
     */
    public static StoricoStatoOrdineDTO buildStoricoStatoOrdineDTO(StoricoStatoOrdine s) {
        return StoricoStatoOrdineDTO.builder()
                .statoDa(s.getStatoDa() == null ? null : s.getStatoDa().name())
                .statoA(s.getStatoA().name())
                .eseguitoDa(UtenteMap.nomeVisualizzabile(s.getEseguitoDa()))
                .nota(s.getNota())
                .creationDate(s.getCreationDate())
                .build();
    }

    public static List<StoricoStatoOrdineDTO> buildStoricoStatoOrdineDTOList(
            Collection<StoricoStatoOrdine> lS) {
        return lS.stream()
                .map(s -> buildStoricoStatoOrdineDTO(s))
                .toList();
    }
    
}