package com.betacom.mtgbazar.be.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * L'IBAN esce MASCHERATO (es. "IT60 **** **** 3456"): e' un dato
 * sensibile e nelle liste non serve mai per intero. Il mascheramento
 * lo fa il service alla costruzione del DTO.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContoBancarioDTO {
    private Long id;
    private String intestatario;
    private String ibanMascherato;
    private String bic;
}