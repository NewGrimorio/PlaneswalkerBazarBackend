package com.betacom.mtgbazar.be.mapping.users;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.betacom.mtgbazar.be.dto.users.CarrelloDTO;
import com.betacom.mtgbazar.be.dto.users.OpzioneSpedizioneDTO;
import com.betacom.mtgbazar.be.dto.users.VoceCarrelloDTO;
import com.betacom.mtgbazar.be.model.users.Carrello;
import com.betacom.mtgbazar.be.model.users.VoceCarrello;
import com.betacom.mtgbazar.be.model.users.enums.TipoSpedizione;

public class CarrelloMap {

	public static CarrelloDTO buildCarrelloDTO(Carrello c, List<VoceCarrello> voci) {
        List<VoceCarrelloDTO> vociDTO = VoceCarrelloMap.buildVoceCarrelloDTOList(voci);

        BigDecimal totale = vociDTO.stream()
                .map(VoceCarrelloDTO::getSubtotale)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer numeroArticoli = vociDTO.stream()
                .map(VoceCarrelloDTO::getQuantita)
                .reduce(0, Integer::sum);

        boolean offerta = TipoSpedizione.sopraSoglia(totale);
        BigDecimal manca = offerta ? BigDecimal.ZERO
                : TipoSpedizione.SOGLIA_GRATUITA.subtract(totale);

        return CarrelloDTO.builder()
                .id(c.getId())
                .voci(vociDTO)
                .totale(totale)
                .numeroArticoli(numeroArticoli)
                .spedizioneOfferta(offerta)
                .mancaPerSpedizioneGratuita(manca)
                .opzioniSpedizione(opzioni(totale, numeroArticoli, offerta))
                .build();
    }

    /**
     * Carrello vuoto -> nessuna opzione (non ha senso proporre spedizioni
     * per nulla). Sopra soglia -> una sola voce: express offerta, perche'
     * mostrare una scelta che il server ignorerebbe sarebbe una bugia.
     */
    private static List<OpzioneSpedizioneDTO> opzioni(
            BigDecimal totaleMerce, Integer numeroArticoli, boolean offerta) {

        if (numeroArticoli == null || numeroArticoli == 0) return List.of();

        List<TipoSpedizione> proposte = offerta
                ? List.of(TipoSpedizione.EXPRESS)
                : List.of(TipoSpedizione.STANDARD, TipoSpedizione.EXPRESS);

        return proposte.stream().map(t -> {
            BigDecimal costo = TipoSpedizione.applica(t, totaleMerce).costo();
            return OpzioneSpedizioneDTO.builder()
                    .tipo(t.name())
                    .etichetta(t.etichetta())
                    .tempi(t.tempi())
                    .costo(costo)
                    .totaleConSpedizione(totaleMerce.add(costo))
                    .build();
        }).toList();
    }
    
    
}