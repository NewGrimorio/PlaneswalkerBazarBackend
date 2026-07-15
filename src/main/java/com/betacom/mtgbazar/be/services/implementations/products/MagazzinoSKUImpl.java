package com.betacom.mtgbazar.be.services.implementations.products;


import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.mtgbazar.be.dto.products.MagazzinoSKUDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.mapping.products.MagazzinoSKUMap;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.products.enums.Condizione;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;
import com.betacom.mtgbazar.be.repositories.products.IMagazzinoSKURepository;
import com.betacom.mtgbazar.be.repositories.products.IProdottoRepository;
import com.betacom.mtgbazar.be.request.products.MagazzinoSKUReq;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.products.IMagazzinoSKUServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MagazzinoSKUImpl implements IMagazzinoSKUServices {

   private final IMagazzinoSKURepository skuR;
   private final IProdottoRepository prodottoR;
   private final IMessaggioServices msg;

   @Override
   @Transactional(readOnly = true)
   public List<MagazzinoSKUDTO> listByProdotto(Long prodottoId) {
       log.debug("listByProdotto: {}", prodottoId);
       caricaProdotto(prodottoId);
       return MagazzinoSKUMap.buildMagazzinoSKUDTOList(
               skuR.findByProdottoId(prodottoId));
   }

   @Override
   @Transactional
   public MagazzinoSKUDTO createSku(MagazzinoSKUReq req) {
       log.debug("createSku: prodotto={} [{}/{}/{}]", req.getProdottoId(),
               req.getCondizione(), req.getLingua(), req.getFinitura());

       Prodotto p = caricaProdotto(req.getProdottoId());

       // Default della variante (allineati ai default del DB)
       Condizione condizione = req.getCondizione() == null ? Condizione.NA : req.getCondizione();
       String lingua = req.getLingua() == null ? "en" : req.getLingua().trim().toLowerCase();
       Finitura finitura = req.getFinitura() == null ? Finitura.NONFOIL : req.getFinitura();

       // Anticipa il vincolo uq_magazzino_sku_var con un errore pulito
       if (skuR.existsByProdottoIdAndCondizioneAndLinguaAndFinitura(
               p.getId(), condizione, lingua, finitura))
           throw new MtgException(msg.get("sku.variante.duplicata"));

       MagazzinoSKU s = new MagazzinoSKU();
       s.setProdotto(p);
       s.setCondizione(condizione);
       s.setLingua(lingua);
       s.setFinitura(finitura);
       s.setPrezzo(req.getPrezzo());
       s.setQuantita(req.getQuantita());
       if (req.getAttivo() != null) s.setAttivo(req.getAttivo());
       skuR.save(s);

       log.debug("creato sku id={}", s.getId());
       return MagazzinoSKUMap.buildMagazzinoSKUDTO(s);
   }

   @Override
   @Transactional
   public MagazzinoSKUDTO updateSku(MagazzinoSKUReq req) {
       log.debug("updateSku: id={}", req.getId());

       MagazzinoSKU s = skuR.findById(req.getId())
               .orElseThrow(() -> new MtgException(msg.get("sku.non.trovato")));

       // La variante e' IMMUTABILE: si toccano solo prezzo/quantita/attivo.
       // NB: qui niente lock — e' il pannello admin, non il checkout; la
       // giacenza impostata a mano e' una DECISIONE, non un decremento.
       if (req.getPrezzo() != null)   s.setPrezzo(req.getPrezzo());
       if (req.getQuantita() != null) s.setQuantita(req.getQuantita());
       if (req.getAttivo() != null)   s.setAttivo(req.getAttivo());

       return MagazzinoSKUMap.buildMagazzinoSKUDTO(s);
   }

   private Prodotto caricaProdotto(Long id) {
       return prodottoR.findById(id)
               .orElseThrow(() -> new MtgException(msg.get("prodotto.non.trovato")));
   }
   
}