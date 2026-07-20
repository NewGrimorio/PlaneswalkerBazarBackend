package com.betacom.mtgbazar.be.request.users;

import com.betacom.mtgbazar.be.request.ValidationGroups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Create = nuovo indirizzo in rubrica; Update = modifica.
 * L'utenteId serve per l'ownership check nel service; il flag
 * "predefinito" viaggia qui e il service aggiorna la FK su utente.
 *
 * FASE C: l'utenteId ha perso il @NotNull — il client non ha piu'
 * l'autorita' di dichiararlo. Lo valorizza il CONTROLLER dal token.
 */
@Getter
@Setter
@ToString
public class IndirizzoReq {

    @NotNull(groups = ValidationGroups.Update.class, message = "indirizzo.no.id")
    private Long id;

    /** Valorizzato dal controller (token), mai dal client. */
    private Long utenteId;

    @Size(max = 50, message = "indirizzo.etichetta.maxlength")
    private String etichetta;

    @NotNull(groups = ValidationGroups.Create.class, message = "indirizzo.no.destinatario")
    @NotBlank(groups = ValidationGroups.Create.class, message = "indirizzo.no.destinatario")
    @Size(max = 200, message = "indirizzo.destinatario.maxlength")
    private String destinatario;

    @NotNull(groups = ValidationGroups.Create.class, message = "indirizzo.no.via")
    @NotBlank(groups = ValidationGroups.Create.class, message = "indirizzo.no.via")
    @Size(max = 200, message = "indirizzo.via.maxlength")
    private String via;

    @NotNull(groups = ValidationGroups.Create.class, message = "indirizzo.no.civico")
    @NotBlank(groups = ValidationGroups.Create.class, message = "indirizzo.no.civico")
    @Size(max = 20, message = "indirizzo.civico.maxlength")
    private String civico;

    @NotNull(groups = ValidationGroups.Create.class, message = "indirizzo.no.cap")
    @NotBlank(groups = ValidationGroups.Create.class, message = "indirizzo.no.cap")
    @Pattern(regexp = "^[0-9A-Za-z -]{3,10}$", message = "indirizzo.cap.invalid")
    private String cap;

    @NotNull(groups = ValidationGroups.Create.class, message = "indirizzo.no.citta")
    @NotBlank(groups = ValidationGroups.Create.class, message = "indirizzo.no.citta")
    @Size(max = 100, message = "indirizzo.citta.maxlength")
    private String citta;

    @Size(max = 50, message = "indirizzo.provincia.maxlength")
    private String provincia;

    @Pattern(regexp = "^[A-Za-z]{2}$", message = "indirizzo.nazione.invalid")
    private String nazione;

    private Boolean predefinito;
}