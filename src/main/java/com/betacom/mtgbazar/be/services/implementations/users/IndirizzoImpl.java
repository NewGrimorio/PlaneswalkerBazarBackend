package com.betacom.mtgbazar.be.services.implementations.users;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.mtgbazar.be.dto.users.IndirizzoDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.users.IndirizzoMap;
import com.betacom.mtgbazar.be.model.users.Indirizzo;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.repositories.users.IIndirizzoRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.request.users.IndirizzoReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IIndirizzoServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndirizzoImpl implements IIndirizzoServices {

    private final IIndirizzoRepository indirizzoR;
    private final IUtenteRepository utenteR;
    private final IMessaggioServices msg;

    @Override
    @Transactional(readOnly = true)
    public List<IndirizzoDTO> listIndirizzi(Long utenteId) {
        log.debug("listIndirizzi: utente={}", utenteId);
        Utente u = caricaUtente(utenteId);
        return IndirizzoMap.buildIndirizzoDTOList(
                indirizzoR.findByUtenteIdAndAttivoTrueOrderByCreationDateAsc(utenteId),
                predefinitoId(u));
    }

    @Override
    @Transactional
    public IndirizzoDTO createIndirizzo(IndirizzoReq req) {
        log.debug("createIndirizzo: utente={}", req.getUtenteId());
        Utente u = caricaUtente(req.getUtenteId());

        Indirizzo i = new Indirizzo();
        i.setUtente(u);
        i.setEtichetta(req.getEtichetta());
        i.setDestinatario(req.getDestinatario());
        i.setVia(req.getVia());
        i.setCivico(req.getCivico());
        i.setCap(req.getCap());
        i.setCitta(req.getCitta());
        i.setProvincia(req.getProvincia());
        if (req.getNazione() != null)
            i.setNazione(req.getNazione().toUpperCase());   // default "IT" dall'entity
        indirizzoR.save(i);

        // Primo indirizzo o richiesta esplicita -> diventa il predefinito
        boolean primo = u.getIndirizzoPredefinito() == null;
        if (primo || Boolean.TRUE.equals(req.getPredefinito()))
            u.setIndirizzoPredefinito(i);

        log.debug("creato indirizzo id={} predefinito={}", i.getId(),
                u.getIndirizzoPredefinito() == i);
        return IndirizzoMap.buildIndirizzoDTO(i, predefinitoId(u));
    }

    @Override
    @Transactional
    public IndirizzoDTO updateIndirizzo(IndirizzoReq req) {
        log.debug("updateIndirizzo: id={} utente={}", req.getId(), req.getUtenteId());
        Utente u = caricaUtente(req.getUtenteId());
        Indirizzo i = caricaProprioAttivo(req.getId(), req.getUtenteId());

        // Aggiornamento null-safe: solo i campi presenti nella request
        if (req.getEtichetta() != null)    i.setEtichetta(req.getEtichetta());
        if (req.getDestinatario() != null) i.setDestinatario(req.getDestinatario());
        if (req.getVia() != null)          i.setVia(req.getVia());
        if (req.getCivico() != null)       i.setCivico(req.getCivico());
        if (req.getCap() != null)          i.setCap(req.getCap());
        if (req.getCitta() != null)        i.setCitta(req.getCitta());
        if (req.getProvincia() != null)    i.setProvincia(req.getProvincia());
        if (req.getNazione() != null)      i.setNazione(req.getNazione().toUpperCase());

        // predefinito = true PROMUOVE; per togliere il default se ne
        // promuove un altro (mai restare senza per scelta implicita)
        if (Boolean.TRUE.equals(req.getPredefinito()))
            u.setIndirizzoPredefinito(i);

        return IndirizzoMap.buildIndirizzoDTO(i, predefinitoId(u));
    }

    @Override
    @Transactional
    public void removeIndirizzo(Long id, Long utenteId) {
        log.debug("removeIndirizzo: id={} utente={}", id, utenteId);
        Utente u = caricaUtente(utenteId);
        Indirizzo i = caricaProprioAttivo(id, utenteId);

        // Regola della FK: se era il predefinito, PRIMA si azzera il
        // riferimento sull'utente, POI si disattiva l'indirizzo
        if (u.getIndirizzoPredefinito() != null
                && u.getIndirizzoPredefinito().getId().equals(i.getId()))
            u.setIndirizzoPredefinito(null);

        i.setAttivo(Boolean.FALSE);   // SOLO soft delete, mai cancellazione
    }

    @Override
    @Transactional
    public void setPredefinito(Long id, Long utenteId) {
        log.debug("setPredefinito: id={} utente={}", id, utenteId);
        Utente u = caricaUtente(utenteId);
        Indirizzo i = caricaProprioAttivo(id, utenteId);
        u.setIndirizzoPredefinito(i);
    }

    // ------------------------------------------------------------------

    private Utente caricaUtente(Long utenteId) {
        return utenteR.findById(utenteId)
                .filter(Utente::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("utente.non.trovato")));
    }

    /** Ownership check + soft delete: solo indirizzi PROPRI e ATTIVI. */
    private Indirizzo caricaProprioAttivo(Long id, Long utenteId) {
        return indirizzoR.findByIdAndUtenteId(id, utenteId)
                .filter(Indirizzo::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("indirizzo.non.trovato")));
    }

    private Long predefinitoId(Utente u) {
        return u.getIndirizzoPredefinito() == null
                ? null : u.getIndirizzoPredefinito().getId();
    }
    
}