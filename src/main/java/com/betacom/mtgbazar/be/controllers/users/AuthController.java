package com.betacom.mtgbazar.be.controllers.users;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.users.LoginDTO;
import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.exceptions.AuthTokenException;
import com.betacom.mtgbazar.be.request.ValidationGroups;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.security.LoginReq;
import com.betacom.mtgbazar.be.security.UtentePrincipal;
import com.betacom.mtgbazar.be.security.interfaces.IJwtServices;
import com.betacom.mtgbazar.be.security.interfaces.IRefreshTokenServices;
import com.betacom.mtgbazar.be.security.interfaces.IRefreshTokenServices.RotazioneEsito;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * I flussi di IDENTITA' (Fase B: i token veri).
 *
 * login    -> AuthenticationManager (DaoAuthenticationProvider +
 *             CustomUserDetailsService): access token nel body,
 *             refresh token opaco nel cookie HttpOnly.
 * refresh  -> rotazione del refresh (reuse detection) + nuovo access.
 *             Stesso LoginDTO del login: e' anche il bootstrap post-F5.
 * logout   -> revoca della famiglia + cookie azzerato.
 * me       -> identita' dal Bearer token (sub = id). Autenticato.
 *
 * Il cookie ha path=/api/auth: il browser lo invia SOLO qui, mai con
 * le chiamate normali. secure e' pilotato da property: false in dev,
 * true in produzione (HTTPS).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";

    private final IUtenteServices utenteS;
    private final AuthenticationManager authenticationManager;
    private final IJwtServices jwtS;
    private final IRefreshTokenServices refreshS;
    private final IMessaggioServices msg;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    @Value("${app.jwt.cookie-secure}")
    private boolean cookieSecure;

    @PostMapping("/registrazione")
    public UtenteDTO registra(
            @Validated(ValidationGroups.Create.class) @RequestBody UtenteReq req) {
        log.debug("POST /api/auth/registrazione");
        return utenteS.registraUtente(req);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginDTO> login(
            @Validated @RequestBody LoginReq req,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        log.debug("POST /api/auth/login");

        // BadCredentials/Disabled -> GlobalExceptionHandler -> 401 "Credenziali errate"
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getIdentificativo(), req.getPassword()));

        UtentePrincipal p = (UtentePrincipal) authentication.getPrincipal();

        String accessToken = jwtS.generateAccessToken(p.getId(), p.getUsername(), p.getRuolo());
        String refreshToken = refreshS.emetti(p.getId(), userAgent);

        return rispostaConCookie(accessToken, refreshToken, utenteS.getById(p.getId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginDTO> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        log.debug("POST /api/auth/refresh");

        // Rotazione: valida + revoca il vecchio + emette il nuovo.
        // Ogni fallimento -> AuthTokenException -> 401 dal handler.
        RotazioneEsito esito = refreshS.ruota(refreshToken, userAgent);

        String accessToken = jwtS.generateAccessToken(
                esito.utenteId(), esito.username(), esito.ruolo());

        return rispostaConCookie(accessToken, esito.nuovoToken(),
                utenteS.getById(esito.utenteId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        log.debug("POST /api/auth/logout");

        refreshS.revoca(refreshToken);   // idempotente: senza cookie non fa nulla

        // Cookie azzerato: stesso nome/path, valore vuoto, vita zero
        ResponseCookie sparito = cookieRefresh("", Duration.ZERO);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sparito.toString())
                .build();
    }

    /**
     * Identita' dal token (profilo prod: principal = Jwt, sub = id).
     * In dev non ci sono token validati: risponde 401, ed e' corretto
     * cosi' — il bootstrap del frontend usa /refresh, che funziona
     * in entrambi i profili (cookie + DB, niente JWT da validare).
     */
    @GetMapping("/me")
    public UtenteDTO me(Authentication authentication) {
        log.debug("GET /api/auth/me");

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt))
            throw new AuthTokenException(msg.get("auth.non.autenticato"));

        return utenteS.getById(Long.valueOf(jwt.getSubject()));
    }

    // ------------------------------------------------------------------
    // Interni
    // ------------------------------------------------------------------

    private ResponseEntity<LoginDTO> rispostaConCookie(String accessToken,
                                                       String refreshToken,
                                                       UtenteDTO utente) {
        ResponseCookie cookie = cookieRefresh(refreshToken,
                Duration.ofDays(refreshTokenExpirationDays));

        LoginDTO body = LoginDTO.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .utente(utente)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    private ResponseCookie cookieRefresh(String valore, Duration durata) {
        return ResponseCookie.from(REFRESH_COOKIE, valore)
                .httpOnly(true)            // invisibile a JavaScript: l'XSS non lo ruba
                .secure(cookieSecure)      // false in dev, true in prod (HTTPS)
                .sameSite("Lax")           // protezione CSRF sul refresh
                .path(COOKIE_PATH)         // viaggia SOLO verso /api/auth
                .maxAge(durata)
                .build();
    }
    
}