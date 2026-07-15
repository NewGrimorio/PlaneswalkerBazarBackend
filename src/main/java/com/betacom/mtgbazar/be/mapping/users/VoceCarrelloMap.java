package com.betacom.mtgbazar.be.mapping.users;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.VoceCarrelloDTO;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.users.VoceCarrello;

public class VoceCarrelloMap {

    /*
     * A differenza della Fase 2, NESSUNA mappa prezzi esterna: il prezzo
     * vive sullo SKU referenziato dalla voce, ed e' il prezzo LIVE del
     * negozio (lo snapshot esiste solo nell'ordine).
     * PRE-REQUISITO: sku e sku.prodotto gia' fetchati dal service
     * (query VoceCarrello.findByCarrelloIdWithSku), altrimenti LAZY exception.
     */
    public static VoceCarrelloDTO buildVoceCarrelloDTO(VoceCarrello v) {
        MagazzinoSKU s = v.getSku();
        BigDecimal prezzo = s.getPrezzo();
        return VoceCarrelloDTO.builder()
                .id(v.getId())
                .skuId(s.getId())
                .nomeProdotto(s.getProdotto().getNome())
                .tipoProdotto(s.getProdotto().getTipoProdotto().name())
                .condizione(s.getCondizione().name())
                .lingua(s.getLingua())
                .finitura(s.getFinitura().name())
                .imageUrl(s.getProdotto().getImageUrl())
                .prezzoUnitario(prezzo)
                .quantita(v.getQuantita())
                .subtotale(prezzo.multiply(BigDecimal.valueOf(v.getQuantita())))
                .disponibile(Boolean.TRUE.equals(s.getAttivo())
                        && s.getQuantita() >= v.getQuantita())
                .build();
    }

    public static List<VoceCarrelloDTO> buildVoceCarrelloDTOList(Collection<VoceCarrello> lV) {
        return lV.stream()
                .map(v -> buildVoceCarrelloDTO(v))
                .toList();
    }
}