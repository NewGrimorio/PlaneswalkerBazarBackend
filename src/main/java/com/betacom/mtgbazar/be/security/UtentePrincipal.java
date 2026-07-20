package com.betacom.mtgbazar.be.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.model.users.enums.RuoloUtente;

import lombok.Getter;

/**
 * UserDetails custom: il User standard di Spring non porta l'id, e a noi
 * l'id serve come SUBJECT del JWT (immutabile — lo username da noi si
 * puo' cambiare, quindi non puo' identificare un token).
 *
 * Copia dei campi, nessun riferimento all'entity: il principal vive
 * oltre la transazione che l'ha caricato.
 *
 * isEnabled() = attivo: un utente disattivato viene rifiutato dal
 * DaoAuthenticationProvider con DisabledException — che il
 * GlobalExceptionHandler traduce nello STESSO "Credenziali errate"
 * (anti-enumeration: fuori non si distingue da una password sbagliata).
 */
@Getter
public class UtentePrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final RuoloUtente ruolo;
    private final boolean attivo;

    public UtentePrincipal(Utente u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.email = u.getEmail();
        this.passwordHash = u.getPasswordHash();
        this.ruolo = u.getRuolo();
        this.attivo = Boolean.TRUE.equals(u.getAttivo());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // "ROLE_ADMIN" / "ROLE_CLIENTE": il prefisso qui, cosi' hasRole("ADMIN") funziona
        return List.of(new SimpleGrantedAuthority("ROLE_" + ruolo.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return attivo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
}