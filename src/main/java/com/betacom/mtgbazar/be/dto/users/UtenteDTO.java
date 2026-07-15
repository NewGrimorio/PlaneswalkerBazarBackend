package com.betacom.mtgbazar.be.dto.users;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Vista dell'utente verso l'esterno.
 * MAI l'hash della password, MAI l'entity JPA cruda fuori dal service.
 * Il ruolo viaggia come String: il frontend non deve conoscere gli enum.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtenteDTO {
    private Long id;
    private String email;
    private String ruolo;
    private String nome;
    private String cognome;
    private String telefono;
    private LocalDate dataNascita;
    private String codiceFiscale;
    private LocalDateTime dataRegistrazione;
}
