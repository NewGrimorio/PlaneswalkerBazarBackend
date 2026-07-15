package com.betacom.mtgbazar.be.mapping.users;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.model.users.MovimentoPortafoglio;

public class MovimentoMap {

    /*
     * ordineId/contoBancarioId: leggere SOLO l'id di una relazione LAZY
     * non scatena il caricamento del proxy — e' l'unico attraversamento
     * sicuro senza fetch. Non leggere altri campi di ordine/conto qui.
     */
    public static MovimentoDTO buildMovimentoDTO(MovimentoPortafoglio m) {
        return MovimentoDTO.builder()
                .id(m.getId())
                .tipo(m.getTipo().name())
                .metodo(m.getMetodo().name())
                .stato(m.getStato().name())
                .importo(m.getImporto())
                .commissione(m.getCommissione())
                .riferimentoEsterno(m.getRiferimentoEsterno())
                .descrizione(m.getDescrizione())
                .ordineId(m.getOrdine() == null ? null : m.getOrdine().getId())
                .contoBancarioId(m.getContoBancario() == null ? null : m.getContoBancario().getId())
                .creationDate(m.getCreationDate())
                .completionDate(m.getCompletionDate())
                .build();
    }

    public static List<MovimentoDTO> buildMovimentoDTOList(Collection<MovimentoPortafoglio> lM) {
        return lM.stream()
                .map(m -> buildMovimentoDTO(m))
                .toList();
    }
}