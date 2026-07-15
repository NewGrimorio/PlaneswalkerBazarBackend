package com.betacom.mtgbazar.be.mapping.users;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.dto.users.RecensioneStatisticheDTO;
import com.betacom.mtgbazar.be.model.users.Recensione;

public class RecensioneMap {

    /*
     * PRE-REQUISITO: utente fetchato (serve per il nome visualizzabile).
     * acquistoVerificato = true per costruzione: nel nostro modello non
     * esistono recensioni senza ordine consegnato.
     */
    public static RecensioneDTO buildRecensioneDTO(Recensione r) {
        return RecensioneDTO.builder()
                .id(r.getId())
                .voto(r.getVoto())
                .titolo(r.getTitolo())
                .testo(r.getTesto())
                .stato(r.getStato().name())
                .autore(UtenteMap.nomeVisualizzabile(r.getUtente()))
                .acquistoVerificato(Boolean.TRUE)
                .creationDate(r.getCreationDate())
                .updateDate(r.getUpdateDate())
                .build();
    }

    public static List<RecensioneDTO> buildRecensioneDTOList(Collection<Recensione> lR) {
        return lR.stream()
                .map(r -> buildRecensioneDTO(r))
                .toList();
    }

    /*
     * Costruito dai due valori della named query
     * Recensione.statisticheByProdotto: [AVG(voto), COUNT(*)].
     * media null (nessuna recensione) -> 0.0 e conteggio 0.
     */
    public static RecensioneStatisticheDTO buildStatisticheDTO(Double media, Long conteggio) {
        return RecensioneStatisticheDTO.builder()
                .media(media == null ? 0.0 : Math.round(media * 10.0) / 10.0)
                .conteggio(conteggio == null ? 0L : conteggio)
                .build();
    }
}