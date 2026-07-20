package com.betacom.mtgbazar.be.security;

import org.springframework.security.core.Authentication;

import com.betacom.mtgbazar.be.exceptions.AuthTokenException;

/**
 * L'unico punto dove il subject del token diventa un id utente.
 *
 * Variante del pattern del tutor (req.setUserName(authentication.getName())):
 * da noi getName() e' il SUBJECT del JWT, cioe' l'id — immutabile,
 * perche' lo username si puo' cambiare e non puo' identificare un token.
 * Quindi qui si fa il Long.valueOf una volta sola, con la guardia sul
 * caso "identita' assente" (in dev una chiamata senza token ha
 * principal anonimo): AuthTokenException -> 401 pulito, non un 500 da
 * NumberFormatException.
 *
 * Non e' un bean e non e' un'astrazione: e' quella riga di conversione,
 * scritta in un posto solo. Nei controller si legge
 * req.setUtenteId(SecurityUtils.utenteId(authentication)).
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Long utenteId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null)
            throw new AuthTokenException("Accesso non autorizzato: effettua il login");
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            // principal anonimo (dev senza token) o subject non numerico
            throw new AuthTokenException("Accesso non autorizzato: effettua il login");
        }
    }
    
}