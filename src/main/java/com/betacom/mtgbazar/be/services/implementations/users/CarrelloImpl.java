package com.betacom.mtgbazar.be.services.implementations.users;

import java.util.List;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.betacom.mtgbazar.be.dto.users.CarrelloDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.users.CarrelloMap;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.users.Carrello;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.model.users.VoceCarrello;
import com.betacom.mtgbazar.be.repositories.products.IMagazzinoSKURepository;
import com.betacom.mtgbazar.be.repositories.users.ICarrelloRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.repositories.users.IVoceCarrelloRepository;
import com.betacom.mtgbazar.be.request.users.VoceCarrelloReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.ICarrelloServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class CarrelloImpl implements ICarrelloServices {
 
    private final ICarrelloRepository carrelloR;
    private final IVoceCarrelloRepository voceR;
    private final IMagazzinoSKURepository skuR;
    private final IUtenteRepository utenteR;
    private final IMessaggioServices msg;
 
    @Override
    @Transactional
    public CarrelloDTO getCarrello(Long utenteId) {
        log.debug("getCarrello: utente={}", utenteId);
        Carrello c = caricaOCrea(utenteId);
        return buildDTO(c);
    }
 
    @Override
    @Transactional
    public CarrelloDTO addVoce(VoceCarrelloReq req) {
        log.debug("addVoce: utente={} sku={} quantita={}",
                req.getUtenteId(), req.getSkuId(), req.getQuantita());
 
        Carrello c = caricaOCrea(req.getUtenteId());
 
        // Lo SKU deve esistere, essere attivo e avere giacenza:
        // qui SENZA lock — il carrello e' una vetrina, non una prenotazione.
        // La verifica vincolante (con lock) avverra' al checkout.
        MagazzinoSKU sku = skuR.findById(req.getSkuId())
                .filter(MagazzinoSKU::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("sku.non.trovato")));
 
        VoceCarrello voce = voceR.findByCarrelloIdAndSkuId(c.getId(), sku.getId())
                .orElse(null);
        int giaInCarrello = (voce == null) ? 0 : voce.getQuantita();
 
        // Blocco morbido: mai piu' pezzi nel carrello di quanti a scaffale
        if (giaInCarrello + req.getQuantita() > sku.getQuantita())
            throw new MtgException(msg.get("sku.non.disponibile"));
 
        if (voce == null) {
            voce = new VoceCarrello();
            voce.setCarrello(c);
            voce.setSku(sku);
            voce.setQuantita(req.getQuantita());
            voceR.save(voce);
        } else {
            voce.setQuantita(giaInCarrello + req.getQuantita());   // incrementa
        }
 
        log.debug("voce {}: quantita ora {}", voce.getId(), voce.getQuantita());
        return buildDTO(c);
    }
 
    @Override
    @Transactional
    public CarrelloDTO updateVoce(VoceCarrelloReq req) {
        log.debug("updateVoce: utente={} sku={} quantita={}",
                req.getUtenteId(), req.getSkuId(), req.getQuantita());
 
        Carrello c = caricaOCrea(req.getUtenteId());
 
        VoceCarrello voce = voceR.findByCarrelloIdAndSkuId(c.getId(), req.getSkuId())
                .orElseThrow(() -> new MtgException(msg.get("carrello.voce.non.trovata")));
 
        // quantita' ESATTA (non incremento), sempre nel limite di giacenza
        if (req.getQuantita() > voce.getSku().getQuantita())
            throw new MtgException(msg.get("sku.non.disponibile"));
 
        voce.setQuantita(req.getQuantita());
        return buildDTO(c);
    }
 
    @Override
    @Transactional
    public CarrelloDTO removeVoce(Long utenteId, Long voceId) {
        log.debug("removeVoce: utente={} voce={}", utenteId, voceId);
 
        Carrello c = caricaOCrea(utenteId);
 
        // Ownership check: la voce deve appartenere al carrello DELL'UTENTE
        VoceCarrello voce = voceR.findById(voceId)
                .filter(v -> v.getCarrello().getId().equals(c.getId()))
                .orElseThrow(() -> new MtgException(msg.get("carrello.voce.non.trovata")));
 
        voceR.delete(voce);   // il carrello e' effimero: qui il delete e' VERO
        return buildDTO(c);
    }
 
    @Override
    @Transactional
    public void clearCarrello(Long utenteId) {
        log.debug("clearCarrello: utente={}", utenteId);
        Carrello c = caricaOCrea(utenteId);
        voceR.deleteByCarrelloId(c.getId());
    }
 
    // ------------------------------------------------------------------
 
    /** Un carrello per utente, creato pigramente al primo accesso. */
    private Carrello caricaOCrea(Long utenteId) {
        Utente u = utenteR.findById(utenteId)
                .filter(Utente::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("utente.non.trovato")));
 
        return carrelloR.findByUtenteId(utenteId)
                .orElseGet(() -> {
                    Carrello nuovo = new Carrello();
                    nuovo.setUtente(u);
                    log.debug("creato carrello per utente {}", utenteId);
                    return carrelloR.save(nuovo);
                });
    }
 
    /** Voci con SKU+prodotto gia' fetchati (anti-N+1) -> DTO con totali. */
    private CarrelloDTO buildDTO(Carrello c) {
        List<VoceCarrello> voci = voceR.findByCarrelloIdWithSku(c.getId());
        return CarrelloMap.buildCarrelloDTO(c, voci);
    }
    
}