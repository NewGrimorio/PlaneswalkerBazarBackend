package com.betacom.mtgbazar.be.mapping.users;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.betacom.mtgbazar.be.dto.users.IndirizzoDTO;
import com.betacom.mtgbazar.be.model.users.Indirizzo;

public class IndirizzoMap {

    /*
     * predefinitoId = l'id di utente.indirizzoPredefinito (puo' essere
     * null): il flag non e' una colonna dell'indirizzo, si CALCOLA qui
     * confrontando gli id. Il service lo legge una volta e lo passa.
     */
    public static IndirizzoDTO buildIndirizzoDTO(Indirizzo i, Long predefinitoId) {
        return IndirizzoDTO.builder()
                .id(i.getId())
                .etichetta(i.getEtichetta())
                .destinatario(i.getDestinatario())
                .via(i.getVia())
                .civico(i.getCivico())
                .cap(i.getCap())
                .citta(i.getCitta())
                .provincia(i.getProvincia())
                .nazione(i.getNazione())
                .predefinito(Objects.equals(i.getId(), predefinitoId))
                .build();
    }

    public static List<IndirizzoDTO> buildIndirizzoDTOList(Collection<Indirizzo> lI, Long predefinitoId) {
        return lI.stream()
                .map(i -> buildIndirizzoDTO(i, predefinitoId))
                .toList();
    }
    
}