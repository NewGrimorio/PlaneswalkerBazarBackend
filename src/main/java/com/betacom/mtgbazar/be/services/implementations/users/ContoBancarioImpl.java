package com.betacom.mtgbazar.be.services.implementations.users;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.betacom.mtgbazar.be.dto.users.ContoBancarioDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.users.ContoBancarioMap;
import com.betacom.mtgbazar.be.model.users.ContoBancario;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.repositories.users.IContoBancarioRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.request.users.ContoBancarioReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IContoBancarioServices;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class ContoBancarioImpl implements IContoBancarioServices {
 
    private final IContoBancarioRepository contoR;
    private final IUtenteRepository utenteR;
    private final IMessaggioServices msg;
 
    @Override
    @Transactional(readOnly = true)
    public List<ContoBancarioDTO> listConti(Long utenteId) {
        log.debug("listConti: utente={}", utenteId);
        caricaUtente(utenteId);
        return ContoBancarioMap.buildContoBancarioDTOList(
                contoR.findByUtenteIdAndAttivoTrue(utenteId));
    }
 
    @Override
    @Transactional
    public ContoBancarioDTO createConto(ContoBancarioReq req) {
        log.debug("createConto: utente={}", req.getUtenteId());
        Utente u = caricaUtente(req.getUtenteId());
 
        ContoBancario c = new ContoBancario();
        c.setUtente(u);
        c.setIntestatario(req.getIntestatario().trim());
        // Normalizzazione IBAN: maiuscolo e senza spazi — gli utenti lo
        // incollano com'e' scritto sull'home banking ("IT60 X054 2811...")
        c.setIban(req.getIban().replaceAll("\\s", "").toUpperCase());
        if (req.getBic() != null)
            c.setBic(req.getBic().trim().toUpperCase());
        contoR.save(c);
 
        log.debug("creato conto id={}", c.getId());
        return ContoBancarioMap.buildContoBancarioDTO(c);
    }
 
    @Override
    @Transactional
    public void removeConto(Long id, Long utenteId) {
        log.debug("removeConto: id={} utente={}", id, utenteId);
        caricaUtente(utenteId);
 
        // Ownership check: il conto altrui "non esiste"
        ContoBancario c = contoR.findByIdAndUtenteId(id, utenteId)
                .filter(ContoBancario::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("conto.non.trovato")));
 
        c.setAttivo(Boolean.FALSE);   // SOLO soft delete: il ledger lo referenzia
    }
 
    private Utente caricaUtente(Long utenteId) {
        return utenteR.findById(utenteId)
                .filter(Utente::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("utente.non.trovato")));
    }
    
}