package com.betacom.mtgbazar.be.services.implementations.products;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.betacom.mtgbazar.be.cardtrader.CardtraderMarketplaceProduct;
import com.betacom.mtgbazar.be.cardtrader.CondizioneCardtrader;
import com.betacom.mtgbazar.be.cardtrader.TraduttoreCondizioneCT;
import com.betacom.mtgbazar.be.dto.products.TendenzaPrezzoCartaDTO;
import com.betacom.mtgbazar.be.dto.products.TendenzaSkuDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.model.products.MagazzinoSKU;
import com.betacom.mtgbazar.be.model.products.PrezzoRiferimento;
import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.products.Stampa;
import com.betacom.mtgbazar.be.model.products.enums.Condizione;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;
import com.betacom.mtgbazar.be.model.products.enums.FontePrezzo;
import com.betacom.mtgbazar.be.repositories.products.IMagazzinoSKURepository;
import com.betacom.mtgbazar.be.repositories.products.IPrezzoRiferimentoRepository;
import com.betacom.mtgbazar.be.repositories.products.IProdottoRepository;
import com.betacom.mtgbazar.be.repositories.products.IStampaRepository;
import com.betacom.mtgbazar.be.scryfall.ScryfallCard;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.implementations.products.TendenzaSnapshotTx.SnapshotSpec;
import com.betacom.mtgbazar.be.services.interfaces.products.ITendenzaPrezzoServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestratore delle tendenze prezzo. NON transazionale: i fetch HTTP
 * (Scryfall + Cardtrader) stanno fuori da ogni transazione; la scrittura
 * degli snapshot è delegata a TendenzaSnapshotTx (transazione breve).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TendenzaPrezzoImpl implements ITendenzaPrezzoServices {

    private static final long PAUSA_CT_MS  = 1100;  // marketplace: 1 req/sec
    private static final int  MARKET_TOP_N = 15;    // CT Market = media prime 15

    private final RestClient cardtraderRestClient;
    private final RestClient scryfallRestClient;
    private final IStampaRepository stampaR;
    private final IProdottoRepository prodottoR;
    private final IMagazzinoSKURepository skuR;
    private final IPrezzoRiferimentoRepository prezzoR;
    private final IMessaggioServices msg;
    private final TendenzaSnapshotTx snapshotTx;

    /** Coordinata della chiamata marketplace: (foil, lingua). */
    private record ChiaveGruppo(boolean foil, String lingua) { }

    /** I due prezzi Cardtrader per una condizione. */
    record StatCT(BigDecimal lowest, BigDecimal market) { }

    @Override
    public TendenzaPrezzoCartaDTO tendenzeCarta(Long stampaId) {
        long inizio = System.currentTimeMillis();

        Stampa stampa = stampaR.findByIdFetchCartaEspansione(stampaId)
                .orElseThrow(() -> new MtgException(msg.get("stampa.non.trovata")));
        Prodotto prodotto = prodottoR.findByStampaId(stampaId)
                .orElseThrow(() -> new MtgException(msg.get("prodotto.non.trovato")));
        List<MagazzinoSKU> skus = skuR.findByProdottoIdAndAttivoTrue(prodotto.getId());

        // 1) Cardmarket per finitura: una sola fetch Scryfall
        Map<Finitura, BigDecimal> cardmarket = prezziCardmarket(stampa);

        // 2) Cardtrader: una chiamata per (foil, lingua), bucket per condizione
        Map<ChiaveGruppo, Map<CondizioneCardtrader, StatCT>> ct = prezziCardtrader(stampa, skus);

        // 3) Righe + raccolta snapshot da persistere
        List<TendenzaSkuDTO> righe = new ArrayList<>();
        List<SnapshotSpec> daSalvare = new ArrayList<>();
        Set<Finitura> cmkSalvate = EnumSet.noneOf(Finitura.class);

        for (MagazzinoSKU sku : skus) {
            ChiaveGruppo g = new ChiaveGruppo(sku.getFinitura() != Finitura.NONFOIL, sku.getLingua());
            CondizioneCardtrader cc = TraduttoreCondizioneCT.daCondizione(sku.getCondizione());
            StatCT stat = (cc == null) ? null : ct.getOrDefault(g, Map.of()).get(cc);

            // DIAGNOSTICA: quale bucket sta cercando questo SKU e se l'ha trovato
            log.debug("SKU {}: {}/{}/{} -> CT {} -> {}",
                    sku.getId(), sku.getCondizione(), sku.getLingua(), sku.getFinitura(),
                    cc, stat == null ? "NESSUN PREZZO" : stat.lowest());

            BigDecimal ctLowest = stat == null ? null : stat.lowest();
            BigDecimal ctMarket = stat == null ? null : stat.market();
            BigDecimal cmk = cardmarket.get(sku.getFinitura());

            BigDecimal ctPrec = prezzoR
                .findFirstByStampaIdAndFonteAndFinituraAndCondizioneAndLinguaOrderByDetectionDateDesc(
                    stampaId, FontePrezzo.CARDTRADER, sku.getFinitura(),
                    sku.getCondizione(), sku.getLingua())
                .map(PrezzoRiferimento::getPrezzoMin).orElse(null);
            BigDecimal cmkPrec = prezzoR
                .findFirstByStampaIdAndFonteAndFinituraAndCondizioneAndLinguaOrderByDetectionDateDesc(
                    stampaId, FontePrezzo.SCRYFALL, sku.getFinitura(), Condizione.NA, "NA")
                .map(PrezzoRiferimento::getPrezzoTrend).orElse(null);

            righe.add(TendenzaSkuDTO.builder()
                .skuId(sku.getId())
                .condizione(sku.getCondizione().name())
                .lingua(sku.getLingua())
                .finitura(sku.getFinitura().name())
                .ctLowest(ctLowest).ctMarket(ctMarket).ctPrecedente(ctPrec)
                .cardmarket(cmk).cardmarketPrecedente(cmkPrec)
                .valuta("EUR")
                .build());

            if (ctLowest != null)
                daSalvare.add(new SnapshotSpec(stampaId, FontePrezzo.CARDTRADER,
                        sku.getFinitura(), sku.getCondizione(), sku.getLingua(),
                        ctLowest, ctMarket, null));

            if (cmk != null && cmkSalvate.add(sku.getFinitura()))
                daSalvare.add(new SnapshotSpec(stampaId, FontePrezzo.SCRYFALL,
                        sku.getFinitura(), Condizione.NA, "NA",
                        null, null, cmk));
        }

        snapshotTx.salva(daSalvare);

        return TendenzaPrezzoCartaDTO.builder()
            .stampaId(stampa.getId())
            .nomeCarta(stampa.getCarta().getNome())
            .codiceSet(stampa.getEspansione().getCodice())
            .righe(righe)
            .millisecondiImpiegati(System.currentTimeMillis() - inizio)
            .build();
    }

    // ==================================================================
    // CARDMARKET (via Scryfall) — per finitura
    // ==================================================================

    private Map<Finitura, BigDecimal> prezziCardmarket(Stampa stampa) {
        if (stampa.getScryfallId() == null) return Map.of();
        ScryfallCard card;
        try {
            card = scryfallRestClient.get()
                    .uri("/cards/{id}", stampa.getScryfallId())
                    .retrieve().body(ScryfallCard.class);
        } catch (Exception e) {
            log.warn("Scryfall /cards/{} fallito ({})", stampa.getScryfallId(), e.getMessage());
            return Map.of();
        }
        if (card == null || card.prices() == null) return Map.of();

        Map<Finitura, BigDecimal> out = new EnumMap<>(Finitura.class);
        if (card.prices().eur() != null)       out.put(Finitura.NONFOIL, card.prices().eur());
        if (card.prices().eurFoil() != null)   out.put(Finitura.FOIL,    card.prices().eurFoil());
        if (card.prices().eurEtched() != null) out.put(Finitura.ETCHED,  card.prices().eurEtched());
        return out;
    }

    // ==================================================================
    // CARDTRADER — per (foil, lingua), bucket per condizione
    // ==================================================================

    private Map<ChiaveGruppo, Map<CondizioneCardtrader, StatCT>> prezziCardtrader(
            Stampa stampa, List<MagazzinoSKU> skus) {

        Integer blueprintId = stampa.getCardtraderBlueprintId();
        if (blueprintId == null) {
            log.debug("stampa {} senza blueprint Cardtrader: niente prezzi CT", stampa.getId());
            return Map.of();
        }

        Set<ChiaveGruppo> gruppi = skus.stream()
                .map(s -> new ChiaveGruppo(s.getFinitura() != Finitura.NONFOIL, s.getLingua()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<ChiaveGruppo, Map<CondizioneCardtrader, StatCT>> out = new HashMap<>();
        boolean prima = true;
        for (ChiaveGruppo g : gruppi) {
            if (!prima) pausaCardtrader();
            prima = false;
            try {
                log.debug("Cardtrader: chiamo marketplace bp={} foil={} lang={}",
                        blueprintId, g.foil(), g.lingua());
                out.put(g, statPerCondizione(offerteMarketplace(blueprintId, g.foil(), g.lingua())));
            } catch (Exception e) {
                log.warn("Cardtrader marketplace fallito bp={} foil={} lang={} ({})",
                        blueprintId, g.foil(), g.lingua(), e.getMessage());
                out.put(g, Map.of());
            }
        }
        return out;
    }

    private List<CardtraderMarketplaceProduct> offerteMarketplace(int blueprintId, boolean foil, String lingua) {
        Map<String, List<CardtraderMarketplaceProduct>> risposta = cardtraderRestClient.get()
                .uri(u -> u.path("/marketplace/products")
                          .queryParam("blueprint_id", blueprintId)
                          .queryParam("foil", foil)
                          .queryParam("language", lingua)
                          .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, List<CardtraderMarketplaceProduct>>>() {});

        // DIAGNOSTICA: le chiavi tornate, per capire se il blueprint corrisponde
        if (risposta == null) {
            log.debug("Cardtrader: risposta NULL dal marketplace");
            return List.of();
        }
        log.debug("Cardtrader: chiavi nella risposta = {} (cerco '{}')",
                risposta.keySet(), blueprintId);
        return risposta.getOrDefault(String.valueOf(blueprintId), List.of());
    }

    /** Bucketizza le offerte (già ordinate per prezzo crescente) per condizione CT. */
    Map<CondizioneCardtrader, StatCT> statPerCondizione(List<CardtraderMarketplaceProduct> offerte) {

        // DIAGNOSTICA: quante offerte e com'e' fatta ciascuna
        log.debug("Cardtrader: {} offerte ricevute", offerte.size());
        for (CardtraderMarketplaceProduct o : offerte)
            log.debug("  offerta: cents={} cur={} cond='{}' lang={} foil={}",
                o.price() == null ? null : o.price().cents(),
                o.price() == null ? null : o.price().currency(),
                o.properties() == null ? null : o.properties().condition(),
                o.properties() == null ? null : o.properties().mtgLanguage(),
                o.properties() == null ? null : o.properties().mtgFoil());

        int scartatePrezzo = 0, scartateValuta = 0, scartateCondizione = 0;

        Map<CondizioneCardtrader, List<BigDecimal>> perCond = new EnumMap<>(CondizioneCardtrader.class);
        for (CardtraderMarketplaceProduct o : offerte) {
            if (o.price() == null || o.price().euro() == null) { scartatePrezzo++; continue; }
            if (o.price().currency() != null && !"EUR".equalsIgnoreCase(o.price().currency())) {
                scartateValuta++; continue;
            }
            CondizioneCardtrader cc = TraduttoreCondizioneCT.daEtichetta(
                    o.properties() == null ? null : o.properties().condition());
            if (cc == null) { scartateCondizione++; continue; }
            perCond.computeIfAbsent(cc, k -> new ArrayList<>()).add(o.price().euro());
        }

        // DIAGNOSTICA: dove sono finite le offerte scartate
        log.debug("Cardtrader: scartate -> prezzo={} valuta={} condizione={}; bucket creati={}",
                scartatePrezzo, scartateValuta, scartateCondizione, perCond.keySet());

        Map<CondizioneCardtrader, StatCT> out = new EnumMap<>(CondizioneCardtrader.class);
        perCond.forEach((cc, prezzi) -> out.put(cc, new StatCT(prezzi.get(0), media(prezzi))));
        return out;
    }

    BigDecimal media(List<BigDecimal> prezzi) {
        List<BigDecimal> primi = prezzi.subList(0, Math.min(MARKET_TOP_N, prezzi.size()));
        BigDecimal somma = primi.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return somma.divide(BigDecimal.valueOf(primi.size()), 2, RoundingMode.HALF_UP);
    }

    private void pausaCardtrader() {
        try { Thread.sleep(PAUSA_CT_MS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    
}