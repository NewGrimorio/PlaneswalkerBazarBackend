package com.betacom.mtgbazar.be.mapping.users;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.ContoBancarioDTO;
import com.betacom.mtgbazar.be.model.users.ContoBancario;

public class ContoBancarioMap {

    public static ContoBancarioDTO buildContoBancarioDTO(ContoBancario c) {
        return ContoBancarioDTO.builder()
                .id(c.getId())
                .intestatario(c.getIntestatario())
                .ibanMascherato(mascheraIban(c.getIban()))
                .bic(c.getBic())
                .build();
    }

    public static List<ContoBancarioDTO> buildContoBancarioDTOList(Collection<ContoBancario> lC) {
        return lC.stream()
                .map(c -> buildContoBancarioDTO(c))
                .toList();
    }

    /*
     * "IT60X054281110..." -> "IT60 **** 3456": primi 4 + ultimi 4.
     * L'IBAN completo non esce MAI dal backend in visualizzazione.
     */
    private static String mascheraIban(String iban) {
        if (iban == null || iban.length() < 8) return "****";
        return iban.substring(0, 4) + " **** " + iban.substring(iban.length() - 4);
    }
}