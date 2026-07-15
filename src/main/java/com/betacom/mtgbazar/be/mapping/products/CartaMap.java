package com.betacom.mtgbazar.be.mapping.products;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.products.CartaDTO;
import com.betacom.mtgbazar.be.model.products.Carta;

public class CartaMap {

    public static CartaDTO buildCartaDTO(Carta c) {
        return CartaDTO.builder()
                .id(c.getId())
                .nome(c.getNome())
                .costoMana(c.getCostoMana())
                .valoreMana(c.getValoreMana())
                .tipoRiga(c.getTipoRiga())
                .testoOracle(c.getTestoOracle())
                .forza(c.getForza())
                .costituzione(c.getCostituzione())
                .colori(c.getColori())
                .identitaColore(c.getIdentitaColore())
                .paroleChiave(c.getParoleChiave())
                .legal(c.getLegal())
                .build();
    }

    public static List<CartaDTO> buildCartaDTOList(Collection<Carta> lC) {
        return lC.stream()
                .map(c -> buildCartaDTO(c))
                .toList();
    }
    
}