package com.betacom.mtgbazar.be.security.implementations;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.mtgbazar.be.exceptions.AuthTokenException;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.model.users.RefreshToken;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.repositories.users.IRefreshTokenRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.security.interfaces.IRefreshTokenServices;
import com.betacom.mtgbazar.be.services.IMessaggioServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Il cuore della sessione lunga. Tre principi:
 *  1) nel DB solo SHA-256: token random a 256 bit, non attaccabile a
 *     dizionario, quindi basta un hash veloce (bcrypt qui sprecherebbe
 *     CPU a ogni refresh);
 *  2) ROTAZIONE: ogni token vale UN refresh; il riuso di un token
 *     consumato brucia la famiglia (reuse detection = furto);
 *  3) ogni fallimento di validazione e' la STESSA AuthTokenException:
 *     chi presenta un token invalido non merita diagnosi dettagliate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenImpl implements IRefreshTokenServices {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final IRefreshTokenRepository refreshR;
    private final IUtenteRepository utenteR;
    private final IMessaggioServices msg;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    @Override
    @Transactional
    public String emetti(Long utenteId, String userAgent) {
        log.debug("emetti refresh: utente={}", utenteId);

        Utente u = utenteR.findById(utenteId)
                .filter(Utente::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("utente.non.trovato")));

        return salvaNuovo(u, UUID.randomUUID().toString(), userAgent);
    }

    @Override
    @Transactional
    public RotazioneEsito ruota(String tokenChiaro, String userAgent) {
        RefreshToken rt = carica(tokenChiaro);

        // REUSE DETECTION: un token gia' consumato che torna a bussare
        // significa che due mani hanno lo stesso token. Non sappiamo
        // quale sia il ladro: si brucia tutta la famiglia.
        if (Boolean.TRUE.equals(rt.getRevocato())) {
            int n = refreshR.revocaFamiglia(rt.getFamiglia());
            log.warn("RIUSO refresh token: famiglia {} revocata ({} token) — utente id={}",
                    rt.getFamiglia(), n, rt.getUtente().getId());
            throw invalido();
        }

        if (rt.getScadenza().isBefore(LocalDateTime.now())) {
            rt.setRevocato(Boolean.TRUE);   // dirty checking
            throw invalido();
        }

        Utente u = rt.getUtente();
        if (!Boolean.TRUE.equals(u.getAttivo())) {
            refreshR.revocaFamiglia(rt.getFamiglia());
            throw invalido();
        }

        // Rotazione: il vecchio muore, il nuovo eredita la famiglia
        rt.setRevocato(Boolean.TRUE);
        String nuovo = salvaNuovo(u, rt.getFamiglia(), userAgent);

        log.debug("refresh ruotato: utente={} famiglia={}", u.getId(), rt.getFamiglia());
        return new RotazioneEsito(nuovo, u.getId(), u.getUsername(), u.getRuolo());
    }

    @Override
    @Transactional
    public void revoca(String tokenChiaro) {
        if (tokenChiaro == null || tokenChiaro.isBlank()) return;   // logout idempotente

        refreshR.findByTokenHash(hash(tokenChiaro))
                .ifPresent(rt -> {
                    int n = refreshR.revocaFamiglia(rt.getFamiglia());
                    log.debug("logout: famiglia {} revocata ({} token)", rt.getFamiglia(), n);
                });
    }

    // ------------------------------------------------------------------
    // Interni
    // ------------------------------------------------------------------

    private String salvaNuovo(Utente u, String famiglia, String userAgent) {
        // 32 byte random -> Base64 URL-safe senza padding (~43 caratteri)
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String tokenChiaro = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken rt = new RefreshToken();
        rt.setUtente(u);
        rt.setTokenHash(hash(tokenChiaro));
        rt.setFamiglia(famiglia);
        rt.setScadenza(LocalDateTime.now().plusDays(refreshTokenExpirationDays));
        rt.setRevocato(Boolean.FALSE);
        rt.setUserAgent(userAgent != null && userAgent.length() > 255
                ? userAgent.substring(0, 255) : userAgent);
        refreshR.save(rt);

        return tokenChiaro;
    }

    private RefreshToken carica(String tokenChiaro) {
        if (tokenChiaro == null || tokenChiaro.isBlank()) throw invalido();
        return refreshR.findByTokenHash(hash(tokenChiaro))
                .orElseThrow(this::invalido);
    }

    private AuthTokenException invalido() {
        return new AuthTokenException(msg.get("auth.refresh.invalido"));
    }

    private String hash(String tokenChiaro) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(tokenChiaro.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 non disponibile", e);
        }
    }
}