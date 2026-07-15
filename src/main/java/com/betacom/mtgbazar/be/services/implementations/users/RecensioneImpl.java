package com.betacom.mtgbazar.be.services.implementations.users;


import java.util.List;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.betacom.mtgbazar.be.dto.users.RecensioneDTO;
import com.betacom.mtgbazar.be.dto.users.RecensioneStatisticheDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.users.RecensioneMap;
import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.users.Ordine;
import com.betacom.mtgbazar.be.model.users.Recensione;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.model.users.enums.StatOrdine;
import com.betacom.mtgbazar.be.model.users.enums.StatoRecensione;
import com.betacom.mtgbazar.be.repositories.products.IProdottoRepository;
import com.betacom.mtgbazar.be.repositories.users.IOrdineRepository;
import com.betacom.mtgbazar.be.repositories.users.IRecensioneRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.repositories.users.IVoceOrdineRepository;
import com.betacom.mtgbazar.be.request.users.RecensioneReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IRecensioneServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class RecensioneImpl implements IRecensioneServices {
 
    private final IRecensioneRepository recensioneR;
    private final IOrdineRepository ordineR;
    private final IVoceOrdineRepository voceOrdineR;
    private final IProdottoRepository prodottoR;
    private final IUtenteRepository utenteR;
    private final IMessaggioServices msg;
 
    @Override
    @Transactional
    public RecensioneDTO saveRecensione(RecensioneReq req) {
        log.debug("saveRecensione: utente={} prodotto={} ordine={} voto={}",
                req.getUtenteId(), req.getProdottoId(), req.getOrdineId(), req.getVoto());
 
        Utente u = utenteR.findById(req.getUtenteId())
                .filter(Utente::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("utente.non.trovato")));
 
        Prodotto prodotto = prodottoR.findById(req.getProdottoId())
                .filter(Prodotto::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("prodotto.non.trovato")));
 
        // --- IL DIRITTO DI RECENSIRE (acquisto verificato) ---
        // 1) l'ordine e' dell'utente (ownership: quello altrui "non esiste")
        Ordine ordine = ordineR.findByIdAndUtenteId(req.getOrdineId(), u.getId())
                .orElseThrow(() -> new MtgException(msg.get("ordine.non.trovato")));
 
        // 2) e' stato CONSEGNATO
        if (ordine.getStato() != StatOrdine.CONSEGNATO)
            throw new MtgException(msg.get("recensione.non.consentita"));
 
        // 3) contiene il prodotto recensito
        boolean contieneProdotto = voceOrdineR.findByOrdineIdWithSku(ordine.getId()).stream()
                .anyMatch(v -> v.getSku().getProdotto().getId().equals(prodotto.getId()));
        if (!contieneProdotto)
            throw new MtgException(msg.get("recensione.non.consentita"));
 
        // Una per (utente, prodotto): se esiste si AGGIORNA
        Recensione r = recensioneR.findByUtenteIdAndProdottoId(u.getId(), prodotto.getId())
                .orElseGet(() -> {
                    Recensione nuova = new Recensione();
                    nuova.setUtente(u);
                    nuova.setProdotto(prodotto);
                    return nuova;
                });
        r.setOrdine(ordine);
        r.setVoto(req.getVoto());
        r.setTitolo(req.getTitolo());
        r.setTesto(req.getTesto());
        // Il contenuto e' cambiato: torna in stato di pubblicazione di
        // default (policy attuale: pubblicazione immediata)
        r.setStato(StatoRecensione.APPROVATA);
        recensioneR.save(r);
 
        log.debug("recensione {} salvata (voto {})", r.getId(), r.getVoto());
        return RecensioneMap.buildRecensioneDTO(r);
    }
 
    @Override
    @Transactional(readOnly = true)
    public List<RecensioneDTO> listByProdotto(Long prodottoId) {
        log.debug("listByProdotto: prodotto={}", prodottoId);
        return RecensioneMap.buildRecensioneDTOList(
                recensioneR.findByProdottoIdAndStatoOrderByCreationDateDesc(
                        prodottoId, StatoRecensione.APPROVATA));
    }
 
    @Override
    @Transactional(readOnly = true)
    public RecensioneStatisticheDTO getStatistiche(Long prodottoId) {
        log.debug("getStatistiche: prodotto={}", prodottoId);
        RecensioneStatisticheDTO grezzo = recensioneR.statisticheByProdotto(
                prodottoId, StatoRecensione.APPROVATA);
        // Normalizzazione (media null -> 0.0, arrotondamento a 1 decimale)
        return RecensioneMap.buildStatisticheDTO(grezzo.getMedia(), grezzo.getConteggio());
    }
 
    @Override
    @Transactional
    public RecensioneDTO modera(Long recensioneId, Boolean approvata) {
        log.debug("modera: recensione={} approvata={}", recensioneId, approvata);
        Recensione r = recensioneR.findById(recensioneId)
                .orElseThrow(() -> new MtgException(msg.get("recensione.non.trovata")));
        r.setStato(Boolean.TRUE.equals(approvata)
                ? StatoRecensione.APPROVATA : StatoRecensione.RIFIUTATA);
        return RecensioneMap.buildRecensioneDTO(r);
    }
    
}