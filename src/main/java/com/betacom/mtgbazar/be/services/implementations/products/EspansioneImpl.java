package com.betacom.mtgbazar.be.services.implementations.products;


import java.util.List;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.betacom.mtgbazar.be.dto.products.EspansioneDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.products.EspansioneMap;
import com.betacom.mtgbazar.be.model.products.Espansione;
import com.betacom.mtgbazar.be.repositories.products.IEspansioneRepository;
import com.betacom.mtgbazar.be.request.products.EspansioneReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.products.IEspansioneServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class EspansioneImpl implements IEspansioneServices {
 
    private final IEspansioneRepository espansioneR;
    private final IMessaggioServices msg;
 
    @Override
    @Transactional(readOnly = true)
    public List<EspansioneDTO> listEspansioni() {
        log.debug("listEspansioni");
        return EspansioneMap.buildEspansioneDTOList(
                espansioneR.findAllByOrderByDataUscitaDesc());
    }
 
    @Override
    @Transactional(readOnly = true)
    public EspansioneDTO getByCodice(String codice) {
        log.debug("getByCodice: {}", codice);
        return EspansioneMap.buildEspansioneDTO(caricaByCodice(codice));
    }
 
    @Override
    @Transactional
    public EspansioneDTO createEspansione(EspansioneReq req) {
        log.debug("createEspansione: {}", req.getCodice());
 
        // Codici Scryfall minuscoli ("mh3"): normalizzazione qui
        String codice = req.getCodice().trim().toLowerCase();
        if (espansioneR.findByCodice(codice).isPresent())
            throw new MtgException(msg.get("espansione.codice.duplicato"));
 
        Espansione e = new Espansione();
        e.setCodice(codice);
        e.setNome(req.getNome());
        e.setTipoSet(req.getTipoSet());
        e.setCodiceSetPadre(req.getCodiceSetPadre() == null
                ? null : req.getCodiceSetPadre().trim().toLowerCase());
        e.setDataUscita(req.getDataUscita());
        e.setIconUrl(req.getIconUrl());
        e.setNumeroCarte(req.getNumeroCarte());
        espansioneR.save(e);
 
        log.debug("creata espansione id={}", e.getId());
        return EspansioneMap.buildEspansioneDTO(e);
    }
 
    @Override
    @Transactional
    public EspansioneDTO updateEspansione(EspansioneReq req) {
        log.debug("updateEspansione: id={}", req.getId());
 
        Espansione e = espansioneR.findById(req.getId())
                .orElseThrow(() -> new MtgException(msg.get("espansione.non.trovata")));
 
        // null-safe: solo i campi presenti; il CODICE e' immutabile
        if (req.getNome() != null)        e.setNome(req.getNome());
        if (req.getTipoSet() != null)     e.setTipoSet(req.getTipoSet());
        if (req.getDataUscita() != null)  e.setDataUscita(req.getDataUscita());
        if (req.getIconUrl() != null)     e.setIconUrl(req.getIconUrl());
        if (req.getNumeroCarte() != null) e.setNumeroCarte(req.getNumeroCarte());
 
        return EspansioneMap.buildEspansioneDTO(e);
    }
 
    private Espansione caricaByCodice(String codice) {
        return espansioneR.findByCodice(codice == null ? null : codice.trim().toLowerCase())
                .orElseThrow(() -> new MtgException(msg.get("espansione.non.trovata")));
    }
    
}
