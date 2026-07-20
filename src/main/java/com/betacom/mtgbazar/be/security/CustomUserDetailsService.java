package com.betacom.mtgbazar.be.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Chiamato dal DaoAuthenticationProvider durante authenticate():
 * questa classe dice CHI e' l'utente, il confronto password lo fa
 * Spring col PasswordEncoder.
 *
 * STESSA logica di risoluzione di UtenteImpl.loginUtente: normalizza
 * (trim+lowercase) e discrimina con la '@' — lo username non puo'
 * contenerla per pattern, l'email deve. Una query, percorso
 * deterministico.
 *
 * La UsernameNotFoundException NON arriva mai al client: il provider
 * la nasconde dietro BadCredentialsException (anti-enumeration
 * garantita da Spring, non dalla nostra disciplina).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final IUtenteRepository utenteR;

    @Override
    public UserDetails loadUserByUsername(String identificativo) throws UsernameNotFoundException {
        log.debug("loadUserByUsername: {}", identificativo);

        String id = identificativo == null ? null
                : identificativo.trim().toLowerCase();

        Utente u = (id != null && id.contains("@")
                        ? utenteR.findByEmail(id)
                        : utenteR.findByUsername(id))
                .orElseThrow(() -> new UsernameNotFoundException("utente non trovato"));

        // NB: 'attivo' NON si filtra qui: lo dichiara UtentePrincipal.isEnabled()
        // e lo boccia il provider (DisabledException -> stesso 401 del handler).
        return new UtentePrincipal(u);
    }
    
}