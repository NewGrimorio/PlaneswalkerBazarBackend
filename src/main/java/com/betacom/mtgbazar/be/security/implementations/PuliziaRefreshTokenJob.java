package com.betacom.mtgbazar.be.security.implementations;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.mtgbazar.be.repositories.users.IRefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manutenzione della tabella refresh_token, che e' append-mostly:
 * ogni login crea una riga, ogni rotazione (F5, refresh a meta'
 * sessione) ne aggiunge un'altra. Senza pulizia cresce all'infinito.
 *
 * Questo job chiude il "job futuro" promesso dall'indice idx_rt_scadenza
 * della V11: una DELETE notturna dei token scaduti da piu' del margine
 * di grazia. NB: cancella i SCADUTI, non i revocati-ma-non-scaduti —
 * quelli restano finche' non scadono, cosi' la reuse-detection continua
 * a poterli riconoscere come "gia' visti" entro la loro finestra di vita.
 * L'indice su scadenza rende la WHERE una scansione mirata, non un
 * full-table-scan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PuliziaRefreshTokenJob {

    private final IRefreshTokenRepository refreshR;

    /** Giorni di grazia DOPO la scadenza prima di cancellare (property, default 7). */
    @Value("${app.jwt.cleanup-grace-days:7}")
    private long graceDays;

    /**
     * Ogni notte alle 03:30 (ora di poco traffico). Il cron e'
     * esternalizzato: in test lo si puo' spegnere o rendere raro senza
     * ricompilare. Zona oraria esplicita: niente sorprese con l'ora legale.
     */
    @Scheduled(cron = "${app.jwt.cleanup-cron:0 30 3 * * *}", zone = "Europe/Rome")
    @Transactional
    public void pulisci() {
        LocalDateTime soglia = LocalDateTime.now().minusDays(graceDays);
        int cancellati = refreshR.deleteScadutiPrimaDi(soglia);
        if (cancellati > 0)
            log.info("pulizia refresh token: {} righe scadute prima di {} rimosse",
                    cancellati, soglia);
        else
            log.debug("pulizia refresh token: nessuna riga da rimuovere");
    }
    
}