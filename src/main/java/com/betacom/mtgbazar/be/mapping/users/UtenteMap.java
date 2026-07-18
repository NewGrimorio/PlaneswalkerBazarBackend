package com.betacom.mtgbazar.be.mapping.users;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.model.users.Utente;

public class UtenteMap {

    public static UtenteDTO buildUtenteDTO(Utente u) {
        return UtenteDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .username(u.getUsername())
                .ruolo(u.getRuolo().name())
                .nome(u.getNome())
                .cognome(u.getCognome())
                .telefono(u.getTelefono())
                .dataNascita(u.getDataNascita())
                .codiceFiscale(u.getCodiceFiscale())
                .dataRegistrazione(u.getDataRegistrazione())
                .build();
    }

    public static List<UtenteDTO> buildUtenteDTOList(Collection<Utente> lU) {
        return lU.stream()
                .map(u -> buildUtenteDTO(u))
                .toList();
    }

    /**
     * Identita' pubblica dell'utente: lo username, nato esattamente per
     * questo. Usato da recensioni e timeline ordini.
     * MAI email o id di altri utenti verso l'esterno.
     */
    public static String nomeVisualizzabile(Utente u) {
        if (u == null) return "Sistema";
        return u.getUsername();
    }
}