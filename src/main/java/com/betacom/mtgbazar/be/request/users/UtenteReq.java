package com.betacom.mtgbazar.be.request.users;


import java.time.LocalDate;

import com.betacom.mtgbazar.be.request.ValidationGroups;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Create = registrazione cliente; Update = modifica dati anagrafici.
 * La password viaggia SOLO in Create: il cambio password ha la sua
 * request dedicata (CambioPasswordReq), con la verifica della vecchia.
 * NB: @ToString.Exclude sulla password, non deve mai finire nei log.
 */
@Getter
@Setter
@ToString
public class UtenteReq {

    @NotNull(groups = ValidationGroups.Update.class, message = "utente.no.id")
    private Long id;

    @NotNull(groups = ValidationGroups.Create.class, message = "utente.no.email")
    @NotBlank(groups = ValidationGroups.Create.class, message = "utente.no.email")
    @Email(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class},
           message = "utente.email.invalid")
    private String email;
    
    @NotBlank(groups = ValidationGroups.Create.class, message = "utente.no.username")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,30}$", message = "utente.username.invalid",
             groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String username;
    
    @NotNull(groups = ValidationGroups.Create.class, message = "utente.no.pwd")
    @NotBlank(groups = ValidationGroups.Create.class, message = "utente.no.pwd")
    @Size(min = 8, max = 72, groups = ValidationGroups.Create.class,
          message = "utente.pwd.length")
    @ToString.Exclude
    private String password;

    @NotNull(groups = ValidationGroups.Create.class, message = "utente.no.nome")
    @NotBlank(groups = ValidationGroups.Create.class, message = "utente.no.nome")
    @Size(max = 100, message = "utente.nome.maxlength")
    private String nome;

    @NotNull(groups = ValidationGroups.Create.class, message = "utente.no.cognome")
    @NotBlank(groups = ValidationGroups.Create.class, message = "utente.no.cognome")
    @Size(max = 100, message = "utente.cognome.maxlength")
    private String cognome;

    @Pattern(regexp = "^[0-9+ ]{6,30}$", message = "utente.telefono.invalid")
    private String telefono;

    @Past(message = "utente.nascita.invalid")
    private LocalDate dataNascita;

    @Pattern(regexp = "^[A-Za-z0-9]{16}$", message = "utente.cf.invalid")
    private String codiceFiscale;
    
}