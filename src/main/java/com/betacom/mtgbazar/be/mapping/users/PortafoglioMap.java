package com.betacom.mtgbazar.be.mapping.users;

import com.betacom.mtgbazar.be.dto.users.PortafoglioDTO;
import com.betacom.mtgbazar.be.model.users.Portafoglio;

public class PortafoglioMap {

    public static PortafoglioDTO buildPortafoglioDTO(Portafoglio p) {
        return PortafoglioDTO.builder()
                .id(p.getId())
                .saldo(p.getSaldo())
                .valuta(p.getValuta())
                .updateDate(p.getUpdateDate())
                .build();
    }
}