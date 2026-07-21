package com.betacom.mtgbazar.be.services.interfaces.users;

import org.springframework.web.multipart.MultipartFile;

import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.security.CambioEmailReq;
import com.betacom.mtgbazar.be.request.users.security.CambioPasswordReq;
import com.betacom.mtgbazar.be.request.users.security.LoginReq;

/**
 * Gestione utenti. Errori di business come MtgException (runtime),
 * raccolti dal GlobalExceptionHandler -> 400.
 */
public interface IUtenteServices {

    /**
     * Registrazione cliente (gruppo Create): normalizza l'email,
     * hasha la password (BCrypt) e crea utente + portafoglio a saldo
     * zero NELLA STESSA transazione.
     */
    UtenteDTO registraUtente(UtenteReq req);

    /** Login: stesso errore per email inesistente e password errata. */
    UtenteDTO loginUtente(LoginReq req);

    /**
     * Aggiorna i SOLI dati anagrafici (gruppo Update): nome, cognome,
     * telefono, data di nascita, codice fiscale. Email e password
     * hanno i loro flussi dedicati e qui vengono ignorate.
     */
    UtenteDTO updateProfilo(UtenteReq req);

    /** Cambio password: richiede la password corrente. */
    UtenteDTO changePassword(CambioPasswordReq req);

    /** Cambio email: operazione sensibile, richiede la password. */
    UtenteDTO changeEmail(CambioEmailReq req);

    UtenteDTO getById(Long id);
    
    //IMMAGINI
    
    UtenteDTO updateImmagineProfilo(Long utenteId, MultipartFile file);

    UtenteDTO removeImmagineProfilo(Long utenteId);
    
}