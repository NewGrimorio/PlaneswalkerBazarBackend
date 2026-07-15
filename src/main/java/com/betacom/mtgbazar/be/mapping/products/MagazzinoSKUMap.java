package com.betacom.mtgbazar.be.mapping.products;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.products.MagazzinoSKUDTO;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;

public class MagazzinoSKUMap {

    public static MagazzinoSKUDTO buildMagazzinoSKUDTO(MagazzinoSKU s) {
        return MagazzinoSKUDTO.builder()
                .id(s.getId())
                .prodottoId(s.getProdotto().getId())   // solo id: LAZY-safe
                .condizione(s.getCondizione().name())
                .lingua(s.getLingua())
                .finitura(s.getFinitura().name())
                .prezzo(s.getPrezzo())
                .quantita(s.getQuantita())
                .attivo(s.getAttivo())
                .disponibile(Boolean.TRUE.equals(s.getAttivo()) && s.getQuantita() > 0)
                .build();
    }

    public static List<MagazzinoSKUDTO> buildMagazzinoSKUDTOList(Collection<MagazzinoSKU> lS) {
        return lS.stream()
                .map(s -> buildMagazzinoSKUDTO(s))
                .toList();
    }
    
}