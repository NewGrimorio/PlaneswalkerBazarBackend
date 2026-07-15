package com.betacom.mtgbazar.be.dto.users;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** La schermata "Credito": saldo attuale e valuta. */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortafoglioDTO {
    private Long id;
    private BigDecimal saldo;
    private String valuta;
    private LocalDateTime updateDate;
}