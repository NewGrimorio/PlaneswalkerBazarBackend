package com.betacom.mtgbazar.be.controllers.users;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.request.ValidationGroups;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.security.LoginReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * I flussi di IDENTITA', raccolti sotto /api/auth: gli unici endpoint
 * non autenticati fuori dalla vetrina /api/public. Cosi' UtenteController
 * resta interamente nel tier autenticato e la SecurityConfig li apre
 * con un'unica lista esplicita.
 *
 * FASE A: solo le mappature sono migrate qui da UtenteController;
 * la logica resta in IUtenteServices e il contratto col frontend
 * (UtenteDTO di risposta) non cambia.
 *
 * FASE B: il login emettera' access token (body) + refresh token
 * (cookie HttpOnly) e nasceranno /refresh, /logout e /me.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final IUtenteServices utenteS;

    @PostMapping("/registrazione")
    public UtenteDTO registra(
            @Validated(ValidationGroups.Create.class) @RequestBody UtenteReq req) {
        log.debug("POST /api/auth/registrazione");
        return utenteS.registraUtente(req);
    }

    @PostMapping("/login")
    public UtenteDTO login(@Validated @RequestBody LoginReq req) {
        log.debug("POST /api/auth/login");
        return utenteS.loginUtente(req);
    }

}