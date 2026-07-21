package com.betacom.mtgbazar.be.services.implementations;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.betacom.mtgbazar.be.dto.DashboardDTO;
import com.betacom.mtgbazar.be.dto.products.MagazzinoSKUDTO;
import com.betacom.mtgbazar.be.mapping.products.MagazzinoSKUMap;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.model.users.enums.StatOrdine;
import com.betacom.mtgbazar.be.model.users.enums.StatoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.StatoRecensione;
import com.betacom.mtgbazar.be.repositories.products.IMagazzinoSKURepository;
import com.betacom.mtgbazar.be.repositories.users.IMovimentoPortafoglioRepository;
import com.betacom.mtgbazar.be.repositories.users.IOrdineRepository;
import com.betacom.mtgbazar.be.repositories.users.IRecensioneRepository;
import com.betacom.mtgbazar.be.services.interfaces.IDashboardServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/**
 * Ogni numero e' una COUNT sul DB, non una lista contata in memoria.
 *
 * "Sotto scorta" ESCLUDE le carte singole (TipoProdotto.SINGLE): la
 * giacenza bassa e' fisiologica per i SINGLE (una-due copie per stampa),
 * quindi segnalarle sarebbe rumore che nasconde i riordini veri. Il
 * contatore e la lista condividono soglia E tipo escluso: sempre coerenti.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardImpl implements IDashboardServices {
 
    /** Soglia "sotto scorta": a questa giacenza (o meno) l'SKU e' da riordinare. */
    private static final int SOGLIA_SCORTA = 3;
 
    /** Tipo escluso dal "sotto scorta": le singole non si riordinano a scaffale. */
    private static final TipoProdotto TIPO_ESCLUSO_SCORTA = TipoProdotto.SINGLE;
 
    private final IOrdineRepository ordineR;
    private final IMovimentoPortafoglioRepository movimentoR;
    private final IMagazzinoSKURepository skuR;
    private final IRecensioneRepository recensioneR;
 
    @Override
    @Transactional(readOnly = true)
    public DashboardDTO getStats() {
        log.debug("getStats: aggregazione contatori dashboard");
 
        return DashboardDTO.builder()
                .ordiniDaSpedire(ordineR.countByStato(StatOrdine.CREATO))
                .bonificiInAttesa(movimentoR.countByStato(StatoMovimento.IN_ATTESA))
                .skuSottoScorta(skuR.countSottoScortaEsclusoTipo(SOGLIA_SCORTA, TIPO_ESCLUSO_SCORTA))
                .recensioniPubblicate(recensioneR.countByStato(StatoRecensione.APPROVATA))
                .build();
    }
 
    @Override
    @Transactional(readOnly = true)
    public List<MagazzinoSKUDTO> sottoScorta() {
        log.debug("sottoScorta: soglia={} escluso={}", SOGLIA_SCORTA, TIPO_ESCLUSO_SCORTA);
        return MagazzinoSKUMap.buildMagazzinoSKUDTOConProdottoList(
                skuR.sottoScortaEsclusoTipo(SOGLIA_SCORTA, TIPO_ESCLUSO_SCORTA));
    }
}