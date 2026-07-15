package com.betacom.mtgbazar.be.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Media e conteggio per la testata della pagina prodotto. */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecensioneStatisticheDTO {
    private Double media;
    private Long conteggio;
}