package com.betacom.mtgbazar.be.services.implementations.products;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.mtgbazar.be.model.products.PrezzoRiferimento;
import com.betacom.mtgbazar.be.model.products.enums.Condizione;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;
import com.betacom.mtgbazar.be.model.products.enums.FontePrezzo;
import com.betacom.mtgbazar.be.repositories.products.IPrezzoRiferimentoRepository;
import com.betacom.mtgbazar.be.repositories.products.IStampaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Confine transazionale della persistenza degli snapshot di tendenza.
 * Riceve spec "piatte" e le materializza in PrezzoRiferimento, usando
 * getReferenceById per la FK stampa (nessun SELECT sull'entità).
 * Append-only: sempre righe nuove, mai UPDATE.
 */
@Component
@RequiredArgsConstructor
public class TendenzaSnapshotTx {

    private final IStampaRepository stampaR;
    private final IPrezzoRiferimentoRepository prezzoR;

    /** Spec immutabile di una rilevazione da salvare. */
    public record SnapshotSpec(
            Long stampaId, FontePrezzo fonte, Finitura finitura,
            Condizione condizione, String lingua,
            BigDecimal prezzoMin, BigDecimal prezzoMedio, BigDecimal prezzoTrend) { }

    @Transactional
    public void salva(List<SnapshotSpec> specs) {
        for (SnapshotSpec s : specs) {
            PrezzoRiferimento pr = new PrezzoRiferimento();
            pr.setStampa(stampaR.getReferenceById(s.stampaId()));
            pr.setFonte(s.fonte());
            pr.setFinitura(s.finitura());
            pr.setCondizione(s.condizione());
            pr.setLingua(s.lingua());
            pr.setPrezzoMin(s.prezzoMin());
            pr.setPrezzoMedio(s.prezzoMedio());
            pr.setPrezzoTrend(s.prezzoTrend());
            prezzoR.save(pr);
        }
    }
}