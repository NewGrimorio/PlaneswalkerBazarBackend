package com.betacom.mtgbazar.be.mapping.users;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.betacom.mtgbazar.be.dto.users.CarrelloDTO;
import com.betacom.mtgbazar.be.dto.users.VoceCarrelloDTO;
import com.betacom.mtgbazar.be.model.users.Carrello;
import com.betacom.mtgbazar.be.model.users.VoceCarrello;

public class CarrelloMap {

    /*
     * Le voci arrivano dal service (niente collezione sull'entita').
     * Totale = somma dei subtotali; reduce con identita' ZERO:
     * carrello vuoto -> totale 0, mai null (pattern Fase 2).
     */
    public static CarrelloDTO buildCarrelloDTO(Carrello c, List<VoceCarrello> voci) {
        List<VoceCarrelloDTO> vociDTO = VoceCarrelloMap.buildVoceCarrelloDTOList(voci);

        BigDecimal totale = vociDTO.stream()
                .map(VoceCarrelloDTO::getSubtotale)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer numeroArticoli = vociDTO.stream()
                .map(VoceCarrelloDTO::getQuantita)
                .reduce(0, Integer::sum);

        return CarrelloDTO.builder()
                .id(c.getId())
                .voci(vociDTO)
                .totale(totale)
                .numeroArticoli(numeroArticoli)
                .build();
    }
    
}