package com.betacom.mtgbazar.be.services.implementations.users;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.dto.users.PortafoglioDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.users.MovimentoMap;
import com.betacom.mtgbazar.be.mapping.users.PortafoglioMap;
import com.betacom.mtgbazar.be.model.users.ContoBancario;
import com.betacom.mtgbazar.be.model.users.MovimentoPortafoglio;
import com.betacom.mtgbazar.be.model.users.Portafoglio;
import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.StatoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.TipoMovimento;
import com.betacom.mtgbazar.be.repositories.users.IContoBancarioRepository;
import com.betacom.mtgbazar.be.repositories.users.IMovimentoPortafoglioRepository;
import com.betacom.mtgbazar.be.repositories.users.IPortafoglioRepository;
import com.betacom.mtgbazar.be.request.users.ConfermaMovimentoReq;
import com.betacom.mtgbazar.be.request.users.PrelievoReq;
import com.betacom.mtgbazar.be.request.users.RicaricaReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IPortafoglioServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortafoglioImpl implements IPortafoglioServices {

    /** Commissione PayPal: 5% + 0,35 EUR (schermata "Credito"). */
    private static final BigDecimal PAYPAL_PERCENTUALE = new BigDecimal("0.05");
    private static final BigDecimal PAYPAL_FISSO = new BigDecimal("0.35");

    private final IPortafoglioRepository portafoglioRepository;
    private final IMovimentoPortafoglioRepository movimentoRepository;
    private final IContoBancarioRepository contoBancarioRepository;
    private final IMessaggioServices msg;

    @Override
    @Transactional(readOnly = true)
    public PortafoglioDTO getByUtente(Long utenteId) {
        Portafoglio p = portafoglioRepository.findByUtenteId(utenteId)
                .orElseThrow(() -> new MtgException(msg.get("portafoglio.non.trovato")));
        return PortafoglioMap.buildPortafoglioDTO(p);
    }

    @Override
    @Transactional
    public MovimentoDTO ricarica(RicaricaReq req) {
        log.debug("ricarica: utente={} importo={} metodo={}",
                req.getUtenteId(), req.getImporto(), req.getMetodo());

        // INTERNO e' riservato al sistema (pagamenti/rimborsi ordine)
        if (req.getMetodo() == MetodoMovimento.INTERNO)
            throw new MtgException(msg.get("portafoglio.metodo.non.valido"));

        MovimentoPortafoglio m = new MovimentoPortafoglio();
        m.setTipo(TipoMovimento.RICARICA);
        m.setMetodo(req.getMetodo());
        m.setImporto(req.getImporto());
        m.setRiferimentoEsterno(req.getRiferimentoEsterno());

        if (req.getMetodo() == MetodoMovimento.PAYPAL) {
            // Commissione calcolata QUI, mai dal client
            BigDecimal commissione = req.getImporto()
                    .multiply(PAYPAL_PERCENTUALE)
                    .add(PAYPAL_FISSO)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal netto = req.getImporto().subtract(commissione);
            if (netto.compareTo(BigDecimal.ZERO) <= 0)
                throw new MtgException(msg.get("portafoglio.importo.min"));

            // Accredito immediato: LOCK sul saldo
            Portafoglio p = portafoglioRepository
                    .findByUtenteIdForUpdate(req.getUtenteId())
                    .orElseThrow(() -> new MtgException(msg.get("portafoglio.non.trovato")));
            p.setSaldo(p.getSaldo().add(netto));   // dirty checking al commit

            m.setPortafoglio(p);
            m.setCommissione(commissione);
            m.setStato(StatoMovimento.COMPLETATO);
            m.setCompletionDate(LocalDateTime.now());
            m.setDescrizione("Ricarica PayPal");
        } else {
            // BONIFICO: nessun tocco al saldo finche' l'admin non conferma.
            // Lettura senza lock: qui non modifichiamo il saldo.
            Portafoglio p = portafoglioRepository.findByUtenteId(req.getUtenteId())
                    .orElseThrow(() -> new MtgException(msg.get("portafoglio.non.trovato")));
            m.setPortafoglio(p);
            m.setCommissione(BigDecimal.ZERO);
            m.setStato(StatoMovimento.IN_ATTESA);
            m.setDescrizione("Ricarica con bonifico bancario");
        }

        movimentoRepository.save(m);
        return MovimentoMap.buildMovimentoDTO(m);
    }

    @Override
    @Transactional
    public MovimentoDTO preleva(PrelievoReq req) {
        log.debug("preleva: utente={} importo={} conto={}",
                req.getUtenteId(), req.getImporto(), req.getContoBancarioId());

        // Ownership check sul conto di destinazione
        ContoBancario conto = contoBancarioRepository
                .findByIdAndUtenteId(req.getContoBancarioId(), req.getUtenteId())
                .filter(ContoBancario::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("conto.non.trovato")));

        // LOCK sul saldo: verifica e decurtazione sono atomiche.
        // Doppio submit -> il secondo trova il saldo gia' decurtato.
        Portafoglio p = portafoglioRepository
                .findByUtenteIdForUpdate(req.getUtenteId())
                .orElseThrow(() -> new MtgException(msg.get("portafoglio.non.trovato")));

        if (p.getSaldo().compareTo(req.getImporto()) < 0)
            throw new MtgException(msg.get("saldo.insufficiente"));

        p.setSaldo(p.getSaldo().subtract(req.getImporto()));

        MovimentoPortafoglio m = new MovimentoPortafoglio();
        m.setPortafoglio(p);
        m.setTipo(TipoMovimento.PRELIEVO);
        m.setMetodo(MetodoMovimento.BONIFICO);
        m.setStato(StatoMovimento.IN_ATTESA);   // l'admin esegue il bonifico
        m.setImporto(req.getImporto());
        m.setCommissione(BigDecimal.ZERO);
        m.setContoBancario(conto);
        m.setDescrizione("Ritiro credito su conto " + conto.getIntestatario());
        movimentoRepository.save(m);

        return MovimentoMap.buildMovimentoDTO(m);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimentoDTO> storico(Long utenteId) {
        Portafoglio p = portafoglioRepository.findByUtenteId(utenteId)
                .orElseThrow(() -> new MtgException(msg.get("portafoglio.non.trovato")));
        return MovimentoMap.buildMovimentoDTOList(
                movimentoRepository.findByPortafoglioIdOrderByCreationDateDesc(p.getId()));
    }
    

    @Override
    @Transactional(readOnly = true)
    public List<MovimentoDTO> movimentiInAttesa() {
        return MovimentoMap.buildMovimentoDTOList(
                movimentoRepository.findByStatoOrderByCreationDateAsc(StatoMovimento.IN_ATTESA));
    }

    @Override
    @Transactional
    public MovimentoDTO confermaMovimento(ConfermaMovimentoReq req) {
        log.debug("confermaMovimento: id={} approvato={}",
                req.getMovimentoId(), req.getApprovato());

        MovimentoPortafoglio m = movimentoRepository.findById(req.getMovimentoId())
                .orElseThrow(() -> new MtgException(msg.get("movimento.non.trovato")));

        // Il ledger e' append-only: solo IN_ATTESA e' lavorabile
        if (m.getStato() != StatoMovimento.IN_ATTESA)
            throw new MtgException(msg.get("movimento.non.lavorabile"));

        boolean approvato = Boolean.TRUE.equals(req.getApprovato());

        if (m.getTipo() == TipoMovimento.RICARICA) {
            if (approvato) {
                // Bonifico arrivato: SOLO ORA il saldo si muove (lock)
                Portafoglio p = portafoglioRepository
                        .findByUtenteIdForUpdate(m.getPortafoglio().getUtente().getId())
                        .orElseThrow(() -> new MtgException(msg.get("portafoglio.non.trovato")));
                p.setSaldo(p.getSaldo().add(m.getImporto().subtract(m.getCommissione())));
                m.setStato(StatoMovimento.COMPLETATO);
            } else {
                m.setStato(StatoMovimento.RIFIUTATO);   // mai accreditato: nulla da stornare
            }
        } else if (m.getTipo() == TipoMovimento.PRELIEVO) {
            if (approvato) {
                m.setStato(StatoMovimento.COMPLETATO);  // gia' decurtato al momento della richiesta
            } else {
                // Prelievo rifiutato: ri-accredito di quanto decurtato (lock)
                Portafoglio p = portafoglioRepository
                        .findByUtenteIdForUpdate(m.getPortafoglio().getUtente().getId())
                        .orElseThrow(() -> new MtgException(msg.get("portafoglio.non.trovato")));
                p.setSaldo(p.getSaldo().add(m.getImporto()));
                m.setStato(StatoMovimento.RIFIUTATO);
            }
        } else {
            // PAGAMENTO_ORDINE / RIMBORSO / RETTIFICA non passano da qui
            throw new MtgException(msg.get("movimento.non.lavorabile"));
        }

        m.setCompletionDate(LocalDateTime.now());
        if (req.getNota() != null && !req.getNota().isBlank())
            m.setDescrizione(m.getDescrizione() + " — " + req.getNota());

        return MovimentoMap.buildMovimentoDTO(m);
    }
    
}