package com.betacom.mtgbazar.be.services.implementations.products;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.mtgbazar.be.dto.products.ProdottoDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.products.ProdottoMap;
import com.betacom.mtgbazar.be.model.products.Espansione;
import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.repositories.products.IEspansioneRepository;
import com.betacom.mtgbazar.be.repositories.products.IMagazzinoSKURepository;
import com.betacom.mtgbazar.be.repositories.products.IProdottoRepository;
import com.betacom.mtgbazar.be.request.products.ProdottoReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.products.IProdottoServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProdottoImpl implements IProdottoServices {

    private final IProdottoRepository prodottoR;
    private final IEspansioneRepository espansioneR;
    private final IMagazzinoSKURepository skuR;
    private final IMessaggioServices msg;

    // ------------------------------------------------------------------
    // NAVIGAZIONE PUBBLICA
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<ProdottoDTO> listByTipo(TipoProdotto tipo) {
        log.debug("listByTipo: {}", tipo);
        return ProdottoMap.buildProdottoDTOList(
                prodottoR.findByTipoProdottoAndAttivoTrue(tipo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProdottoDTO> listByEspansione(Long espansioneId) {
        log.debug("listByEspansione: {}", espansioneId);
        return ProdottoMap.buildProdottoDTOList(
                prodottoR.findByEspansioneIdAndAttivoTrue(espansioneId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProdottoDTO> searchByNome(String testo) {
        log.debug("searchByNome: {}", testo);
        return ProdottoMap.buildProdottoDTOList(prodottoR.searchByNome(testo));
    }

    @Override
    @Transactional(readOnly = true)
    public ProdottoDTO getBySlug(String slug) {
        log.debug("getBySlug: {}", slug);

        // Grafo completo in una query (stampa -> carta, espansione)
        Prodotto p = prodottoR.findBySlugWithDettagli(slug)
                .filter(Prodotto::getAttivo)      // il disattivato "non esiste"
                .orElseThrow(() -> new MtgException(msg.get("prodotto.non.trovato")));

        // Nella pagina pubblica solo le varianti attive
        return ProdottoMap.buildProdottoDTOWithDettagli(p,
                skuR.findByProdottoIdAndAttivoTrue(p.getId()));
    }

    // ------------------------------------------------------------------
    // ADMIN
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ProdottoDTO createProdotto(ProdottoReq req) {
        log.debug("createProdotto: {} [{}]", req.getNome(), req.getTipoProdotto());

        // I SINGLE nascono SOLO dal sync: qui si creano gli altri tipi
        if (req.getTipoProdotto() == TipoProdotto.SINGLE)
            throw new MtgException(msg.get("prodotto.tipo.non.modificabile"));

        String slug = (req.getSlug() == null || req.getSlug().isBlank())
                ? generaSlug(req.getNome())
                : req.getSlug().trim().toLowerCase();
        if (prodottoR.existsBySlug(slug))
            throw new MtgException(msg.get("prodotto.slug.duplicato"));

        Prodotto p = new Prodotto();
        p.setTipoProdotto(req.getTipoProdotto());
        p.setNome(req.getNome());
        p.setSlug(slug);
        p.setDescrizione(req.getDescrizione());
        p.setImageUrl(req.getImageUrl());
        if (req.getEspansioneId() != null)
            p.setEspansione(caricaEspansione(req.getEspansioneId()));
        prodottoR.save(p);

        log.debug("creato prodotto id={} slug={}", p.getId(), p.getSlug());
        return ProdottoMap.buildProdottoDTO(p);
    }

    @Override
    @Transactional
    public ProdottoDTO updateProdotto(ProdottoReq req) {
        log.debug("updateProdotto: id={}", req.getId());

        Prodotto p = prodottoR.findById(req.getId())
                .orElseThrow(() -> new MtgException(msg.get("prodotto.non.trovato")));

        // Il TIPO e' l'identita' del prodotto: mai modificabile
        if (req.getTipoProdotto() != null && req.getTipoProdotto() != p.getTipoProdotto())
            throw new MtgException(msg.get("prodotto.tipo.non.modificabile"));

        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            String slug = req.getSlug().trim().toLowerCase();
            if (!slug.equals(p.getSlug()) && prodottoR.existsBySlug(slug))
                throw new MtgException(msg.get("prodotto.slug.duplicato"));
            p.setSlug(slug);
        }

        if (req.getNome() != null)        p.setNome(req.getNome());
        if (req.getDescrizione() != null) p.setDescrizione(req.getDescrizione());
        if (req.getImageUrl() != null)    p.setImageUrl(req.getImageUrl());
        if (req.getAttivo() != null)      p.setAttivo(req.getAttivo());
        if (req.getEspansioneId() != null)
            p.setEspansione(caricaEspansione(req.getEspansioneId()));

        return ProdottoMap.buildProdottoDTO(p);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProdottoDTO> listByTipoAdmin(TipoProdotto tipo) {
        log.debug("listByTipoAdmin: {}", tipo);
        return ProdottoMap.buildProdottoDTOList(prodottoR.findByTipoProdotto(tipo));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProdottoDTO> searchByNomeAdmin(String testo) {
        log.debug("searchByNomeAdmin: {}", testo);
        return ProdottoMap.buildProdottoDTOList(prodottoR.searchByNomeAdmin(testo));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProdottoDTO> listByEspansioneETipoAdmin(Long espansioneId, TipoProdotto tipo) {
        log.debug("listByEspansioneETipoAdmin: espansione={} tipo={}", espansioneId, tipo);
        return ProdottoMap.buildProdottoDTOList(
                prodottoR.findByEspansioneIdAndTipoProdottoOrderByNomeAsc(espansioneId, tipo));
    }

    // ------------------------------------------------------------------

    /**
     * "Commander Masters — Booster Box!" -> "commander-masters-booster-box"
     * In caso di collisione: suffisso numerico ("-2", "-3", ...).
     */
    private String generaSlug(String nome) {
        String base = nome.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")     // tutto cio' che non e' alfanumerico
                .replaceAll("(^-|-$)", "");        // niente trattini ai bordi
        String slug = base;
        int i = 2;
        while (prodottoR.existsBySlug(slug))
            slug = base + "-" + i++;
        return slug;
    }

    private Espansione caricaEspansione(Long id) {
        return espansioneR.findById(id)
                .orElseThrow(() -> new MtgException(msg.get("espansione.non.trovata")));
    }
    
}