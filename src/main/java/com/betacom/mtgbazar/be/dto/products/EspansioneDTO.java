package com.betacom.mtgbazar.be.dto.products;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspansioneDTO {
	
    private Long id;
    private String codice;
    private String nome;
    private String tipoSet;
    private String codiceSetPadre;
    private LocalDate dataUscita;
    private String iconUrl;
    private Integer numeroCarte;
    
}