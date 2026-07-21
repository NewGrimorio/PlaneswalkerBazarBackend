package com.betacom.mtgbazar.be.mapping.users;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.dto.users.RecensioneStatisticheDTO;
import com.betacom.mtgbazar.be.model.users.Recensione;

public class RecensioneMap {

    /*
     * Vista PUBBLICA. PRE-REQUISITO: utente fetchato (nome visualizzabile).
     * prodottoId e' letto dal PROXY (solo getId(): niente lazy load);
     * prodottoNome resta null qui (il prodotto e' gia' il contesto).
     * acquistoVerificato = true per costruzione.
     */
    public static RecensioneDTO buildRecensioneDTO(Recensione r) {
        return builderComune(r).build();
    }

    /*
     * Vista ADMIN (moderazione). PRE-REQUISITO: utente E prodotto
     * fetchati (query Recensione.findByStatoWithProdotto), perche' qui
     * si legge prodotto.getNome(): l'operatore deve vedere di quale
     * prodotto e' la recensione.
     */
    public static RecensioneDTO buildRecensioneDTOAdmin(Recensione r) {
        return builderComune(r)
                .prodottoNome(r.getProdotto() == null ? null : r.getProdotto().getNome())
                .build();
    }

    public static List<RecensioneDTO> buildRecensioneDTOList(Collection<Recensione> lR) {
        return lR.stream().map(r -> buildRecensioneDTO(r)).toList();
    }

    public static List<RecensioneDTO> buildRecensioneDTOAdminList(Collection<Recensione> lR) {
        return lR.stream().map(r -> buildRecensioneDTOAdmin(r)).toList();
    }

    private static RecensioneDTO.RecensioneDTOBuilder builderComune(Recensione r) {
        return RecensioneDTO.builder()
                .id(r.getId())
                .voto(r.getVoto())
                .titolo(r.getTitolo())
                .testo(r.getTesto())
                .stato(r.getStato().name())
                .autore(UtenteMap.nomeVisualizzabile(r.getUtente()))
                .acquistoVerificato(Boolean.TRUE)
                .prodottoId(r.getProdotto() == null ? null : r.getProdotto().getId())  // proxy.getId(): safe
                .creationDate(r.getCreationDate())
                .updateDate(r.getUpdateDate());
    }

    /*
     * Statistiche pagina prodotto: media (1 decimale, null->0.0) e conteggio.
     */
    public static RecensioneStatisticheDTO buildStatisticheDTO(Double media, Long conteggio) {
        return RecensioneStatisticheDTO.builder()
                .media(media == null ? 0.0 : Math.round(media * 10.0) / 10.0)
                .conteggio(conteggio == null ? 0L : conteggio)
                .build();
    }
    
}