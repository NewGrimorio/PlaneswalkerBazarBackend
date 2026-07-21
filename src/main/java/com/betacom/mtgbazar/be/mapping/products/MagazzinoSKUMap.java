package com.betacom.mtgbazar.be.mapping.products;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.products.MagazzinoSKUDTO;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;

public class MagazzinoSKUMap {

    /** Vista standard: solo prodottoId (LAZY-safe, niente nome). */
    public static MagazzinoSKUDTO buildMagazzinoSKUDTO(MagazzinoSKU s) {
        return builderComune(s).build();
    }

    /**
     * Vista admin RESTOCK: aggiunge il nome del prodotto.
     * PRE-REQUISITO: prodotto accessibile (dentro la transazione readOnly
     * la lazy load su getNome() e' ammessa; la lista sotto scorta e'
     * piccola, quindi l'eventuale N+1 e' trascurabile).
     */
    public static MagazzinoSKUDTO buildMagazzinoSKUDTOConProdotto(MagazzinoSKU s) {
        return builderComune(s)
                .prodottoNome(s.getProdotto().getNome())
                .build();
    }

    public static List<MagazzinoSKUDTO> buildMagazzinoSKUDTOList(Collection<MagazzinoSKU> lS) {
        return lS.stream().map(s -> buildMagazzinoSKUDTO(s)).toList();
    }

    public static List<MagazzinoSKUDTO> buildMagazzinoSKUDTOConProdottoList(Collection<MagazzinoSKU> lS) {
        return lS.stream().map(s -> buildMagazzinoSKUDTOConProdotto(s)).toList();
    }

    private static MagazzinoSKUDTO.MagazzinoSKUDTOBuilder builderComune(MagazzinoSKU s) {
        return MagazzinoSKUDTO.builder()
                .id(s.getId())
                .prodottoId(s.getProdotto().getId())   // solo id: LAZY-safe
                .condizione(s.getCondizione().name())
                .lingua(s.getLingua())
                .finitura(s.getFinitura().name())
                .prezzo(s.getPrezzo())
                .quantita(s.getQuantita())
                .attivo(s.getAttivo())
                .disponibile(Boolean.TRUE.equals(s.getAttivo()) && s.getQuantita() > 0);
    }
    
}