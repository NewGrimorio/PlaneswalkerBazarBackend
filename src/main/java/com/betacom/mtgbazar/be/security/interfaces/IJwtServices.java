package com.betacom.mtgbazar.be.security.interfaces;

import com.betacom.mtgbazar.be.model.users.enums.RuoloUtente;

/**
 * Generazione dell'ACCESS token (JWT firmato HS512, 15 minuti).
 * La VALIDAZIONE non passa da qui: la fa il Resource Server di Spring
 * (NimbusJwtDecoder in JwtConfiguration) sulla stessa chiave.
 *
 * Il refresh token NON e' un JWT: e' opaco e persistito
 * (IRefreshTokenServices) — deviazione dichiarata dal progetto del
 * tutor, motivata da revoca, logout e rotation.
 */
public interface IJwtServices {

    /**
     * subject = id utente (immutabile: lo username da noi si cambia);
     * claim "username" informativo, claim "roles" gia' prefissato
     * ROLE_ per il converter del Resource Server.
     */
    String generateAccessToken(Long utenteId, String username, RuoloUtente ruolo);
}