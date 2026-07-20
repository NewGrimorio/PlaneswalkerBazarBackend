package com.betacom.mtgbazar.be.controllers.users;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.request.ValidationGroups;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.security.CambioEmailReq;
import com.betacom.mtgbazar.be.request.users.security.CambioPasswordReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestione del PROFILO utente: dopo la Fase A questo controller vive
 * interamente nel tier AUTENTICATO. Login e registrazione (gli unici
 * due endpoint aperti che aveva) sono migrati in AuthController
 * (/api/auth): niente eccezioni puntuali nella SecurityConfig.
 *
 * I gruppi di validazione restano il cuore: Update per il profilo,
 * request dedicate per password ed email — stessa disciplina di prima.
 */
@RestController
@RequestMapping("/api/utenti")
@RequiredArgsConstructor
@Slf4j
public class UtenteController {
 
    private final IUtenteServices utenteS;
 
    @GetMapping("/{id}")
    public UtenteDTO getById(@PathVariable Long id) {
        log.debug("GET /api/utenti/{}", id);
        return utenteS.getById(id);
    }
 
    @PutMapping("/profilo")
    public UtenteDTO updateProfilo(
            @Validated(ValidationGroups.Update.class) @RequestBody UtenteReq req) {
        log.debug("PUT /api/utenti/profilo id={}", req.getId());
        return utenteS.updateProfilo(req);
    }
 
    @PutMapping("/password")
    public UtenteDTO changePassword(@Validated @RequestBody CambioPasswordReq req) {
        log.debug("PUT /api/utenti/password id={}", req.getUtenteId());
        return utenteS.changePassword(req);
    }
 
    @PutMapping("/email")
    public UtenteDTO changeEmail(@Validated @RequestBody CambioEmailReq req) {
        log.debug("PUT /api/utenti/email id={}", req.getUtenteId());
        return utenteS.changeEmail(req);
    }
    
    //IMMAGINI
    
    @PostMapping("/{id}/immagine-profilo")
    public UtenteDTO uploadImmagineProfilo(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile file) {
        log.debug("POST /api/utenti/{}/immagine-profilo", id);
        return utenteS.updateImmagineProfilo(id, file);
    }

    @DeleteMapping("/{id}/immagine-profilo")
    public UtenteDTO deleteImmagineProfilo(@PathVariable Long id) {
        log.debug("DELETE /api/utenti/{}/immagine-profilo", id);
        return utenteS.removeImmagineProfilo(id);
    }
    
}