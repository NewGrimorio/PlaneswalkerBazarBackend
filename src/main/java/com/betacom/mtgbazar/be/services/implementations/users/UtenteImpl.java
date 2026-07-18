package com.betacom.mtgbazar.be.services.implementations.users;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.users.UtenteMap;
import com.betacom.mtgbazar.be.model.users.Portafoglio;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.model.users.enums.RuoloUtente;
import com.betacom.mtgbazar.be.repositories.users.IPortafoglioRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.security.CambioEmailReq;
import com.betacom.mtgbazar.be.request.users.security.CambioPasswordReq;
import com.betacom.mtgbazar.be.request.users.security.LoginReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtenteImpl implements IUtenteServices {

    private final IUtenteRepository utenteR;
    private final IPortafoglioRepository portafoglioR;
    private final PasswordEncoder passwordEncoder;
    private final IMessaggioServices msg;

    /**
     * UNICO punto di normalizzazione dell'email di tutto il progetto:
     * chiunque tocchi un'email (salvataggio o ricerca) passa da qui.
     */
    private String normalizzaEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /**
     * Gemello di normalizzaEmail per lo username: il DB conserva SOLO
     * minuscole, cosi' il vincolo UNIQUE coincide con l'unicita' che
     * percepiscono gli umani ("Antonio" e "antonio" sono la stessa persona).
     */
    private String normalizzaUsername(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }

    private String normalizzaCF(String cf) {
        return cf == null ? null : cf.trim().toUpperCase();
    }

    /** Anticipa il CHECK chk_maggiorenne del DB con un errore pulito. */
    private void verificaMaggiorenne(LocalDate dataNascita) {
        if (dataNascita != null
                && dataNascita.isAfter(LocalDate.now().minusYears(18)))
            throw new MtgException(msg.get("utente.nascita.invalid"));
    }

    @Override
    @Transactional
    public UtenteDTO registraUtente(UtenteReq req) {
        log.debug("registraUtente: {}", req.getEmail());

        String email = normalizzaEmail(req.getEmail());
        if (utenteR.existsByEmail(email))
            throw new MtgException(msg.get("utente.email.duplicata"));

        String username = normalizzaUsername(req.getUsername());
        if (utenteR.existsByUsername(username))
            throw new MtgException(msg.get("utente.username.duplicato"));

        String cf = normalizzaCF(req.getCodiceFiscale());
        if (cf != null && utenteR.existsByCodiceFiscale(cf))
            throw new MtgException(msg.get("utente.cf.duplicato"));

        verificaMaggiorenne(req.getDataNascita());

        Utente u = new Utente();
        u.setEmail(email);
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRuolo(RuoloUtente.CLIENTE);
        u.setNome(req.getNome());
        u.setCognome(req.getCognome());
        u.setTelefono(req.getTelefono());
        u.setDataNascita(req.getDataNascita());
        u.setCodiceFiscale(cf);
        utenteR.save(u);

        // Il portafoglio nasce con l'utente, a saldo zero, nella stessa
        // transazione: o nascono entrambi o nessuno dei due.
        Portafoglio p = new Portafoglio();
        p.setUtente(u);
        p.setSaldo(BigDecimal.ZERO);
        portafoglioR.save(p);

        log.debug("registrato utente id={} con portafoglio id={}", u.getId(), p.getId());
        return UtenteMap.buildUtenteDTO(u);
    }

    @Override
    @Transactional(readOnly = true)
    public UtenteDTO loginUtente(LoginReq req) {
        log.debug("loginUtente: {}", req.getEmail());

        Utente u = utenteR.findByEmail(normalizzaEmail(req.getEmail()))
                .filter(Utente::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("utente.credenziali.errate")));

        // Stesso messaggio per "email inesistente" e "password sbagliata":
        // non riveliamo quali email sono registrate (user enumeration).
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash()))
            throw new MtgException(msg.get("utente.credenziali.errate"));

        return UtenteMap.buildUtenteDTO(u);
    }

    @Override
    @Transactional
    public UtenteDTO updateProfilo(UtenteReq req) {
        log.debug("updateProfilo: id={}", req.getId());

        Utente u = caricaAttivo(req.getId());

        // Username: identita' pubblica, modificabile (a differenza dell'email
        // che e' credenziale). Niente anti-enumeration: "gia' in uso" e'
        // l'errore giusto per un dato pubblico per definizione.
        if (req.getUsername() != null) {
            String username = normalizzaUsername(req.getUsername());
            if (!username.equals(u.getUsername())
                    && utenteR.existsByUsername(username))
                throw new MtgException(msg.get("utente.username.duplicato"));
            u.setUsername(username);
        }

        String cf = normalizzaCF(req.getCodiceFiscale());
        if (cf != null && !cf.equals(u.getCodiceFiscale())
                && utenteR.existsByCodiceFiscale(cf))
            throw new MtgException(msg.get("utente.cf.duplicato"));

        verificaMaggiorenne(req.getDataNascita());

        // SOLO anagrafica: email e password hanno i loro flussi dedicati
        if (req.getNome() != null)        u.setNome(req.getNome());
        if (req.getCognome() != null)     u.setCognome(req.getCognome());
        if (req.getTelefono() != null)    u.setTelefono(req.getTelefono());
        if (req.getDataNascita() != null) u.setDataNascita(req.getDataNascita());
        if (cf != null)                   u.setCodiceFiscale(cf);

        // dirty checking: nessun save() necessario
        return UtenteMap.buildUtenteDTO(u);
    }

    @Override
    @Transactional
    public UtenteDTO changePassword(CambioPasswordReq req) {
        log.debug("changePassword: id={}", req.getUtenteId());

        Utente u = caricaAttivo(req.getUtenteId());

        if (!passwordEncoder.matches(req.getVecchiaPassword(), u.getPasswordHash()))
            throw new MtgException(msg.get("utente.credenziali.errate"));

        u.setPasswordHash(passwordEncoder.encode(req.getNuovaPassword()));
        return UtenteMap.buildUtenteDTO(u);
    }

    @Override
    @Transactional
    public UtenteDTO changeEmail(CambioEmailReq req) {
        log.debug("changeEmail: id={}", req.getUtenteId());

        Utente u = caricaAttivo(req.getUtenteId());

        // Operazione sensibile: si riconferma la password
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash()))
            throw new MtgException(msg.get("utente.credenziali.errate"));

        String email = normalizzaEmail(req.getNuovaEmail());
        if (utenteR.existsByEmail(email))
            throw new MtgException(msg.get("utente.email.duplicata"));

        u.setEmail(email);
        return UtenteMap.buildUtenteDTO(u);
    }

    @Override
    @Transactional(readOnly = true)
    public UtenteDTO getById(Long id) {
        return UtenteMap.buildUtenteDTO(caricaAttivo(id));
    }

    /** Caricamento standard: esiste ed e' attivo, altrimenti 400 pulito. */
    private Utente caricaAttivo(Long id) {
        return utenteR.findById(id)
                .filter(Utente::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("utente.non.trovato")));
    }

}