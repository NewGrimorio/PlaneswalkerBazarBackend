package com.betacom.mtgbazar.be.security.implementations;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.betacom.mtgbazar.be.model.users.enums.RuoloUtente;
import com.betacom.mtgbazar.be.security.interfaces.IJwtServices;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Generazione JWT con jjwt. La chiave arriva da app.jwt.secret, che a
 * sua volta arriva SOLO dalla variabile d'ambiente jwt_secret
 * (Run Configuration in dev, secret manager in prod).
 *
 * FAIL-FAST sulla lunghezza: HS512 richiede >= 512 bit (64 byte) e
 * jjwt lo verifica solo AL MOMENTO DELLA FIRMA — cioe' al primo login.
 * La guardia qui sotto sposta l'errore all'avvio, dove appartiene:
 * meglio un'app che non parte di un'app che esplode al primo utente.
 * (Lezione imparata: openssl manda il Base64 a capo a 64 colonne;
 *  copiare solo la prima riga = 48 byte = 384 bit.)
 *
 * Generazione corretta, UNA riga: openssl rand -base64 64 | tr -d '\n'
 */
@Slf4j
@Service
public class JwtImpl implements IJwtServices {

    private final SecretKey key;

    @Value("${app.jwt.access-token-expiration-seconds}")
    private long accessTokenExpirationSeconds;

    public JwtImpl(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "app.jwt.secret non e' Base64 valido: controlla la variabile "
                  + "d'ambiente jwt_secret (attesa una riga unica, ~88 caratteri).", e);
        }

        if (keyBytes.length < 64) {
            throw new IllegalStateException(
                    "app.jwt.secret decodifica a " + (keyBytes.length * 8)
                  + " bit: HS512 ne richiede almeno 512 (64 byte). Probabile copia "
                  + "parziale dell'output di openssl (va a capo a 64 colonne). "
                  + "Rigenera con: openssl rand -base64 64 | tr -d '\\n'");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateAccessToken(Long utenteId, String username, RuoloUtente ruolo) {
        Instant now = Instant.now();

        String t = Jwts.builder()
                .subject(String.valueOf(utenteId))            // id: identita' IMMUTABILE
                .claim("username", username)                  // informativo (puo' cambiare)
                .claim("roles", List.of("ROLE_" + ruolo.name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpirationSeconds)))
                .signWith(key, Jwts.SIG.HS512)
                .compact();

        // MAI loggare il token: e' una credenziale a tutti gli effetti
        log.debug("access token emesso per utente id={}", utenteId);
        return t;
    }
    
}