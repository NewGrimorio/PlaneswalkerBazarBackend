package com.betacom.mtgbazar.be.mapping.products;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.products.EspansioneDTO;
import com.betacom.mtgbazar.be.model.products.Espansione;


public class EspansioneMap {

    public static EspansioneDTO buildEspansioneDTO(Espansione e) {
        return EspansioneDTO.builder()
                .id(e.getId())
                .codice(e.getCodice())
                .nome(e.getNome())
                .tipoSet(e.getTipoSet())
                .codiceSetPadre(e.getCodiceSetPadre())
                .dataUscita(e.getDataUscita())
                .iconUrl(e.getIconUrl())
                .numeroCarte(e.getNumeroCarte())
                .dataUltimaSincronizzazione(e.getDataUltimaSincronizzazione())
                .build();
    }

    public static List<EspansioneDTO> buildEspansioneDTOList(Collection<Espansione> lE) {
        return lE.stream()
                .map(e -> buildEspansioneDTO(e))
                .toList();
    }
    
}