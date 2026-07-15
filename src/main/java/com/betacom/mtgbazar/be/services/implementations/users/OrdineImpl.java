package com.betacom.mtgbazar.be.services.implementations.users;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.betacom.mtgbazar.be.dto.users.OrdineDTO;
import com.betacom.mtgbazar.be.dto.users.StoricoStatoOrdineDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.users.OrdineMap;
import com.betacom.mtgbazar.be.mapping.users.StoricoStatoOrdineMap;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.users.Carrello;
import com.betacom.mtgbazar.be.model.users.Indirizzo;
import com.betacom.mtgbazar.be.model.users.MovimentoPortafoglio;
import com.betacom.mtgbazar.be.model.users.Ordine;
import com.betacom.mtgbazar.be.model.users.Portafoglio;
import com.betacom.mtgbazar.be.model.users.StoricoStatoOrdine;
import com.betacom.mtgbazar.be.model.users.Utente;
import com.betacom.mtgbazar.be.model.users.VoceCarrello;
import com.betacom.mtgbazar.be.model.users.VoceOrdine;
import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.StatOrdine;
import com.betacom.mtgbazar.be.model.users.enums.StatoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.TipoMovimento;
import com.betacom.mtgbazar.be.repositories.products.IMagazzinoSKURepository;
import com.betacom.mtgbazar.be.repositories.users.ICarrelloRepository;
import com.betacom.mtgbazar.be.repositories.users.IIndirizzoRepository;
import com.betacom.mtgbazar.be.repositories.users.IMovimentoPortafoglioRepository;
import com.betacom.mtgbazar.be.repositories.users.IOrdineRepository;
import com.betacom.mtgbazar.be.repositories.users.IPortafoglioRepository;
import com.betacom.mtgbazar.be.repositories.users.IStoricoStatoOrdineRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.repositories.users.IVoceCarrelloRepository;
import com.betacom.mtgbazar.be.repositories.users.IVoceOrdineRepository;
import com.betacom.mtgbazar.be.request.users.CheckoutReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IOrdineServices;
 
import jakarta.persistence.EntityManager;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class OrdineImpl implements IOrdineServices {
 
    private final IOrdineRepository ordineR;
    private final IVoceOrdineRepository voceOrdineR;
    private final IStoricoStatoOrdineRepository storicoR;
    private final ICarrelloRepository carrelloR;
    private final IVoceCarrelloRepository voceCarrelloR;
    private final IIndirizzoRepository indirizzoR;
    private final IMagazzinoSKURepository skuR;
    private final IPortafoglioRepository portafoglioR;
    private final IMovimentoPortafoglioRepository movimentoR;
    private final IUtenteRepository utenteR;
    private final IMessaggioServices msg;
    private final EntityManager em;
 
    // ==================================================================
    // CHECKOUT
    // ==================================================================
 
    @Override
    @Transactional
    public OrdineDTO checkout(CheckoutReq req) {
        log.debug("checkout: utente={} indirizzo={}", req.getUtenteId(), req.getIndirizzoId());
 
        Utente u = caricaUtente(req.getUtenteId());
 
        // 1) Carrello con voci (SKU+prodotto fetchati)
        Carrello carrello = carrelloR.findByUtenteId(u.getId())
                .orElseThrow(() -> new MtgException(msg.get("carrello.vuoto")));
        List<VoceCarrello> voci = voceCarrelloR.findByCarrelloIdWithSku(carrello.getId());
        if (voci.isEmpty())
            throw new MtgException(msg.get("carrello.vuoto"));
 
        // 2) Indirizzo: ownership + attivo (verra' SNAPSHOTTATO)
        Indirizzo ind = indirizzoR.findByIdAndUtenteId(req.getIndirizzoId(), u.getId())
                .filter(Indirizzo::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("indirizzo.non.trovato")));
 
        // 3) LOCK SUGLI SKU — l'ORDER BY id nella query e' l'anti-deadlock.
        //    Da qui in poi le giacenze sono CONGELATE per noi.
        List<Long> skuIds = voci.stream().map(v -> v.getSku().getId()).toList();
        Map<Long, MagazzinoSKU> skuLockati = skuR.findByIdInForUpdate(skuIds).stream()
                .collect(Collectors.toMap(MagazzinoSKU::getId, Function.identity()));
 
        // REFRESH obbligatorio: gli SKU erano GIA' nel persistence context
        // (il JOIN FETCH del carrello) e Hibernate, pur avendo acquisito il
        // lock, restituirebbe le copie STANTIE della cache di primo livello.
        // refresh() ri-legge la riga dal DB — che ormai possediamo.
        skuLockati.values().forEach(em::refresh);
 
        // 4) Verifica scorte e calcolo totale SUI DATI LOCKATI
        BigDecimal totale = BigDecimal.ZERO;
        for (VoceCarrello v : voci) {
            MagazzinoSKU sku = skuLockati.get(v.getSku().getId());
            if (sku == null || !Boolean.TRUE.equals(sku.getAttivo())
                    || sku.getQuantita() < v.getQuantita())
                throw new MtgException(msg.get("sku.non.disponibile"));
            totale = totale.add(sku.getPrezzo()
                    .multiply(BigDecimal.valueOf(v.getQuantita())));
        }
        log.debug("checkout: totale calcolato {}", totale);
 
        // 5) LOCK SUL PORTAFOGLIO — SEMPRE DOPO gli SKU (regola di progetto)
        Portafoglio p = portafoglioR.findByUtenteIdForUpdate(u.getId())
                .orElseThrow(() -> new MtgException(msg.get("portafoglio.non.trovato")));
        if (p.getSaldo().compareTo(totale) < 0)
            throw new MtgException(msg.get("saldo.insufficiente"));
 
        // 6) Decrementi: da qui ogni errore fa rollback di TUTTO
        for (VoceCarrello v : voci)
            skuLockati.get(v.getSku().getId())
                    .setQuantita(skuLockati.get(v.getSku().getId()).getQuantita() - v.getQuantita());
        p.setSaldo(p.getSaldo().subtract(totale));
 
        // 7) Ordine con SNAPSHOT dell'indirizzo
        Ordine o = new Ordine();
        o.setUtente(u);
        o.setStato(StatOrdine.CREATO);
        o.setTotale(totale);
        o.setSpedDestinatario(ind.getDestinatario());
        o.setSpedVia(ind.getVia());
        o.setSpedCivico(ind.getCivico());
        o.setSpedCap(ind.getCap());
        o.setSpedCitta(ind.getCitta());
        o.setSpedProvincia(ind.getProvincia());
        o.setSpedNazione(ind.getNazione());
        ordineR.save(o);
 
        // 8) Voci con SNAPSHOT di descrizione e prezzo
        List<VoceOrdine> vociOrdine = voci.stream().map(v -> {
            MagazzinoSKU sku = skuLockati.get(v.getSku().getId());
            VoceOrdine vo = new VoceOrdine();
            vo.setOrdine(o);
            vo.setSku(sku);
            vo.setDescrizione(descrizioneSnapshot(sku));
            vo.setPrezzoUnitario(sku.getPrezzo());
            vo.setQuantita(v.getQuantita());
            return vo;
        }).toList();
        voceOrdineR.saveAll(vociOrdine);
 
        // 9) Pagamento sul ledger (INTERNO, gia' completato)
        registraMovimento(p, TipoMovimento.PAGAMENTO_ORDINE, totale, o,
                "Pagamento ordine n. " + o.getId());
 
        // 10) Prima riga di storico: null -> CREATO
        registraStorico(o, null, StatOrdine.CREATO, u, null);
 
        // 11) Carrello svuotato: il checkout l'ha consumato
        voceCarrelloR.deleteByCarrelloId(carrello.getId());
 
        log.debug("checkout completato: ordine={} totale={}", o.getId(), totale);
        return OrdineMap.buildOrdineDTOWithVoci(o, vociOrdine);
    }
 
    // ==================================================================
    // LETTURE
    // ==================================================================
 
    @Override
    @Transactional(readOnly = true)
    public List<OrdineDTO> listOrdini(Long utenteId) {
        caricaUtente(utenteId);
        return OrdineMap.buildOrdineDTOList(
                ordineR.findByUtenteIdOrderByCreationDateDesc(utenteId));
    }
 
    @Override
    @Transactional(readOnly = true)
    public OrdineDTO getDettaglio(Long ordineId, Long utenteId) {
        Ordine o = caricaProprio(ordineId, utenteId);
        return OrdineMap.buildOrdineDTOWithVoci(o,
                voceOrdineR.findByOrdineIdWithSku(o.getId()));
    }
 
    @Override
    @Transactional(readOnly = true)
    public List<StoricoStatoOrdineDTO> getTimeline(Long ordineId, Long utenteId) {
        Ordine o = caricaProprio(ordineId, utenteId);
        return StoricoStatoOrdineMap.buildStoricoStatoOrdineDTOList(
                storicoR.findByOrdineIdOrderByCreationDateAsc(o.getId()));
    }
 
    @Override
    @Transactional(readOnly = true)
    public List<OrdineDTO> listByStato(StatOrdine stato) {
        return OrdineMap.buildOrdineDTOList(
                ordineR.findByStatoOrderByCreationDateAsc(stato));
    }
 
    // ==================================================================
    // TRANSIZIONI CLIENTE
    // ==================================================================
 
    @Override
    @Transactional
    public OrdineDTO annulla(Long ordineId, Long utenteId) {
        log.debug("annulla: ordine={} utente={}", ordineId, utenteId);
        Utente u = caricaUtente(utenteId);
        Ordine o = caricaEValidaStatoOwner(ordineId, utenteId, Set.of(StatOrdine.CREATO));
 
        ripristinaScorteERimborsa(o, "Rimborso per annullamento ordine n. " + o.getId());
        return cambiaStato(o, StatOrdine.ANNULLATO, u, null);
    }
 
    @Override
    @Transactional
    public OrdineDTO confermaConsegna(Long ordineId, Long utenteId) {
        log.debug("confermaConsegna: ordine={} utente={}", ordineId, utenteId);
        Utente u = caricaUtente(utenteId);
        Ordine o = caricaEValidaStatoOwner(ordineId, utenteId, Set.of(StatOrdine.SPEDITO));
        return cambiaStato(o, StatOrdine.CONSEGNATO, u, null);
    }
 
    @Override
    @Transactional
    public OrdineDTO segnalaNonConsegnato(Long ordineId, Long utenteId) {
        log.debug("segnalaNonConsegnato: ordine={} utente={}", ordineId, utenteId);
        Utente u = caricaUtente(utenteId);
        Ordine o = caricaEValidaStatoOwner(ordineId, utenteId, Set.of(StatOrdine.SPEDITO));
        return cambiaStato(o, StatOrdine.NON_CONSEGNATO, u, null);
    }
 
    @Override
    @Transactional
    public OrdineDTO richiediReso(Long ordineId, Long utenteId) {
        log.debug("richiediReso: ordine={} utente={}", ordineId, utenteId);
        Utente u = caricaUtente(utenteId);
        Ordine o = caricaEValidaStatoOwner(ordineId, utenteId, Set.of(StatOrdine.CONSEGNATO));
        return cambiaStato(o, StatOrdine.RESO_RICHIESTO, u, null);
    }
 
    // ==================================================================
    // TRANSIZIONI ADMIN
    // ==================================================================
 
    @Override
    @Transactional
    public OrdineDTO spedisci(Long ordineId, Long adminId) {
        log.debug("spedisci: ordine={} admin={}", ordineId, adminId);
        Utente admin = caricaUtente(adminId);
        Ordine o = caricaEValidaStato(ordineId, Set.of(StatOrdine.CREATO));
        return cambiaStato(o, StatOrdine.SPEDITO, admin, null);
    }
 
    @Override
    @Transactional
    public OrdineDTO cancella(Long ordineId, Long adminId) {
        log.debug("cancella: ordine={} admin={}", ordineId, adminId);
        Utente admin = caricaUtente(adminId);
        Ordine o = caricaEValidaStato(ordineId, Set.of(StatOrdine.CREATO));
 
        ripristinaScorteERimborsa(o, "Rimborso per cancellazione ordine n. " + o.getId());
        return cambiaStato(o, StatOrdine.CANCELLATO, admin, null);
    }
 
    @Override
    @Transactional
    public OrdineDTO rimborsa(Long ordineId, Long adminId) {
        log.debug("rimborsa: ordine={} admin={}", ordineId, adminId);
        Utente admin = caricaUtente(adminId);
        Ordine o = caricaEValidaStato(ordineId,
                Set.of(StatOrdine.RESO_RICHIESTO, StatOrdine.NON_CONSEGNATO));
 
        // SOLO denaro: nessun ripristino scorte automatico (il reso puo'
        // tornare danneggiato — la reimmissione a magazzino e' manuale)
        rimborsaSuPortafoglio(o, "Rimborso ordine n. " + o.getId());
        return cambiaStato(o, StatOrdine.RIMBORSATO, admin, null);
    }
 
    // ==================================================================
    // HELPER
    // ==================================================================
 
    private Utente caricaUtente(Long id) {
        return utenteR.findById(id)
                .filter(Utente::getAttivo)
                .orElseThrow(() -> new MtgException(msg.get("utente.non.trovato")));
    }
 
    /** Ownership check: l'ordine altrui "non esiste". */
    private Ordine caricaProprio(Long ordineId, Long utenteId) {
        return ordineR.findByIdAndUtenteId(ordineId, utenteId)
                .orElseThrow(() -> new MtgException(msg.get("ordine.non.trovato")));
    }
 
    /** Pattern Fase 2: carica (owner) e valida che lo stato sia ammesso. */
    private Ordine caricaEValidaStatoOwner(Long ordineId, Long utenteId, Set<StatOrdine> ammessi) {
        Ordine o = caricaProprio(ordineId, utenteId);
        if (!ammessi.contains(o.getStato()))
            throw new MtgException(msg.get("ordine.transizione.non.valida"));
        return o;
    }
 
    /** Variante ADMIN: nessun vincolo di proprieta'. */
    private Ordine caricaEValidaStato(Long ordineId, Set<StatOrdine> ammessi) {
        Ordine o = ordineR.findById(ordineId)
                .orElseThrow(() -> new MtgException(msg.get("ordine.non.trovato")));
        if (!ammessi.contains(o.getStato()))
            throw new MtgException(msg.get("ordine.transizione.non.valida"));
        return o;
    }
 
    /** Unico punto di transizione: cambia stato + riga di storico. */
    private OrdineDTO cambiaStato(Ordine o, StatOrdine nuovo, Utente eseguitoDa, String nota) {
        StatOrdine vecchio = o.getStato();
        o.setStato(nuovo);
        registraStorico(o, vecchio, nuovo, eseguitoDa, nota);
        log.debug("ordine {}: {} -> {}", o.getId(), vecchio, nuovo);
        return OrdineMap.buildOrdineDTO(o);
    }
 
    private void registraStorico(Ordine o, StatOrdine da, StatOrdine a, Utente chi, String nota) {
        StoricoStatoOrdine s = new StoricoStatoOrdine();
        s.setOrdine(o);
        s.setStatoDa(da);
        s.setStatoA(a);
        s.setEseguitoDa(chi);
        s.setNota(nota);
        storicoR.save(s);
    }
 
    /**
     * Annullo/cancellazione: scorte E denaro tornano indietro.
     * STESSA REGOLA DI LOCK del checkout: prima gli SKU (l'ORDER BY id
     * e' nella query), poi il portafoglio.
     */
    private void ripristinaScorteERimborsa(Ordine o, String descrizione) {
        List<VoceOrdine> voci = voceOrdineR.findByOrdineIdWithSku(o.getId());
 
        List<Long> skuIds = voci.stream().map(v -> v.getSku().getId()).toList();
        Map<Long, MagazzinoSKU> skuLockati = skuR.findByIdInForUpdate(skuIds).stream()
                .collect(Collectors.toMap(MagazzinoSKU::getId, Function.identity()));
 
        // Stesso refresh anti-cache-stantia del checkout (le voci ordine
        // hanno fetchato gli SKU prima del lock)
        skuLockati.values().forEach(em::refresh);
 
        for (VoceOrdine v : voci) {
            MagazzinoSKU sku = skuLockati.get(v.getSku().getId());
            sku.setQuantita(sku.getQuantita() + v.getQuantita());
        }
        rimborsaSuPortafoglio(o, descrizione);
    }
 
    /** Rimborso del totale sul portafoglio del cliente (con lock). */
    private void rimborsaSuPortafoglio(Ordine o, String descrizione) {
        Portafoglio p = portafoglioR.findByUtenteIdForUpdate(o.getUtente().getId())
                .orElseThrow(() -> new MtgException(msg.get("portafoglio.non.trovato")));
        p.setSaldo(p.getSaldo().add(o.getTotale()));
        registraMovimento(p, TipoMovimento.RIMBORSO, o.getTotale(), o, descrizione);
    }
 
    private void registraMovimento(Portafoglio p, TipoMovimento tipo,
            BigDecimal importo, Ordine o, String descrizione) {
        MovimentoPortafoglio m = new MovimentoPortafoglio();
        m.setPortafoglio(p);
        m.setTipo(tipo);
        m.setMetodo(MetodoMovimento.INTERNO);
        m.setStato(StatoMovimento.COMPLETATO);
        m.setImporto(importo);
        m.setCommissione(BigDecimal.ZERO);
        m.setOrdine(o);
        m.setDescrizione(descrizione);
        m.setCompletionDate(LocalDateTime.now());
        movimentoR.save(m);
    }
 
    /** "Bustine Dragon Shield (NA, en, NONFOIL)" — congelata nella voce. */
    private String descrizioneSnapshot(MagazzinoSKU sku) {
        return sku.getProdotto().getNome()
                + " (" + sku.getCondizione().name()
                + ", " + sku.getLingua()
                + ", " + sku.getFinitura().name() + ")";
    }
    
}