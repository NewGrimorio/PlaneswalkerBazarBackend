package com.betacom.mtgbazar.be.services.implementations.products;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.betacom.mtgbazar.be.dto.products.SincronizzazioneDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.model.products.Carta;
import com.betacom.mtgbazar.be.model.products.Espansione;
import com.betacom.mtgbazar.be.model.products.PrezzoRiferimento;
import com.betacom.mtgbazar.be.model.products.Prodotto;
import com.betacom.mtgbazar.be.model.products.Stampa;
import com.betacom.mtgbazar.be.model.products.enums.Finitura;
import com.betacom.mtgbazar.be.model.products.enums.FontePrezzo;
import com.betacom.mtgbazar.be.model.products.enums.Rarita;
import com.betacom.mtgbazar.be.model.products.enums.TipoProdotto;
import com.betacom.mtgbazar.be.repositories.products.ICartaRepository;
import com.betacom.mtgbazar.be.repositories.products.IEspansioneRepository;
import com.betacom.mtgbazar.be.repositories.products.IPrezzoRiferimentoRepository;
import com.betacom.mtgbazar.be.repositories.products.IProdottoRepository;
import com.betacom.mtgbazar.be.repositories.products.IStampaRepository;
import com.betacom.mtgbazar.be.scryfall.ScryfallCard;
import com.betacom.mtgbazar.be.scryfall.ScryfallCardFace;
import com.betacom.mtgbazar.be.scryfall.ScryfallCardList;
import com.betacom.mtgbazar.be.scryfall.ScryfallSet;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.interfaces.products.ISincronizzazioneServices;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SincronizzazioneImpl implements ISincronizzazioneServices {

    //Pausa tra le pagine: Scryfall chiede 50-100ms tra le richieste.
    private static final long PAUSA_TRA_PAGINE_MS = 100;

    private final RestClient scryfallRestClient;
    private final IEspansioneRepository espansioneR;
    private final ICartaRepository cartaR;
    private final IStampaRepository stampaR;
    private final IProdottoRepository prodottoR;
    private final IPrezzoRiferimentoRepository prezzoR;
    private final IMessaggioServices msg;
    private final ObjectMapper objectMapper;

    //Contatori del report (azzerati a ogni sync) 
    private int carteNuove, carteAggiornate, stampeNuove, stampeAggiornate, prodottiCreati;
    //Scryfall id visti in questo import: alimenta il rilevamento orfani 
    private Set<UUID> vistiScryfallIds;
    private static final String ORDINE_COLORI = "WUBRG";

    @Override
    @Transactional
    public SincronizzazioneDTO sincronizzaSet(String codiceSet) {
        long inizio = System.currentTimeMillis();
        String codice = codiceSet.trim().toLowerCase();
        log.debug("sincronizzaSet: {}", codice);
        carteNuove = carteAggiornate = stampeNuove = stampeAggiornate = prodottiCreati = 0;
        vistiScryfallIds = new HashSet<>();

        // 1) Il set da Scryfall -> upsert espansione
        ScryfallSet setScryfall = chiamaScryfallSet(codice);
        Espansione espansione = upsertEspansione(setScryfall);

     // 2) Le stampe del set, pagina per pagina (175 a pagina).
     // NB: URI gia' costruito e codificato UNA volta sola — .uri(String)
     // di RestClient ri-codificherebbe i '%' (doppio encoding -> 404)
        URI url = UriComponentsBuilder
             .fromUriString("https://api.scryfall.com/cards/search")
             .queryParam("q", "e:" + codice)
             .queryParam("unique", "prints")
             .queryParam("order", "set")
             .encode()
             .build()
             .toUri();

        int totale = 0;
        while (url != null) {
        	ScryfallCardList pagina = chiamaScryfallPagina(url);
        	if (pagina.data() != null) {
        		for (ScryfallCard card : pagina.data())
        			importaCarta(card, espansione);
        		totale += pagina.data().size();
        	}
         // next_page e' un URL completo gia' codificato: URI.create lo usa as-is
         url = (Boolean.TRUE.equals(pagina.hasMore()) && pagina.nextPage() != null)
                 ? URI.create(pagina.nextPage()) : null;
         if (url != null) pausa();
         log.debug("pagina completata: {} stampe finora", totale);
     	}

        // 3) Rilevamento orfani: le stampe attive non viste in questo import.
        // Il flag e' SOLO segnalazione: la disattivazione resta una decisione
        // admin. Dentro la transazione del set: un import fallito a meta'
        // non tocca ne' i dati ne' i flag (all-or-nothing).
        List<Stampa> attive = stampaR.findByEspansioneIdAndAttivoTrue(espansione.getId());
        attive.forEach(s -> s.setOrfana(!vistiScryfallIds.contains(s.getScryfallId())));
        long stampeOrfane = attive.stream().filter(Stampa::getOrfana).count();
        if (stampeOrfane > 0)
            log.warn("sync {}: {} stampe orfane da revisionare", codice, stampeOrfane);
 
        espansione.setDataUltimaSincronizzazione(LocalDateTime.now());
 
        long durata = System.currentTimeMillis() - inizio;
        log.debug("sync {} completata in {}ms: carte {}/{} stampe {}/{} prodotti {}",
                codice, durata, carteNuove, carteAggiornate,
                stampeNuove, stampeAggiornate, prodottiCreati);

        return SincronizzazioneDTO.builder()
                .codiceSet(codice)
                .nomeSet(espansione.getNome())
                .totaleStampe(totale)
                .carteNuove(carteNuove)
                .carteAggiornate(carteAggiornate)
                .stampeNuove(stampeNuove)
                .stampeAggiornate(stampeAggiornate)
                .prodottiCreati(prodottiCreati)
                .stampeOrfane(stampeOrfane)
                .durataMs(durata)
                .build();
    }

    // ==================================================================
    // CHIAMATE HTTP
    // ==================================================================

    private ScryfallSet chiamaScryfallSet(String codice) {
        try {
            return scryfallRestClient.get()
                    .uri("/sets/{codice}", codice)
                    .retrieve()
                    .body(ScryfallSet.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404)
                throw new MtgException(msg.get("sync.set.non.trovato"));
            log.error("Scryfall /sets/{}: HTTP {}", codice, e.getStatusCode(), e);
            throw new MtgException(msg.get("sync.errore.comunicazione"));
        } catch (Exception e) {
            log.error("Scryfall /sets/{}: errore di rete", codice, e);
            throw new MtgException(msg.get("sync.errore.comunicazione"));
        }
    }

    private ScryfallCardList chiamaScryfallPagina(URI url) {
    	try {
            return scryfallRestClient.get()
                    .uri(url)                    // URI assoluto: usato cosi' com'e'
                    .retrieve()
                    .body(ScryfallCardList.class);
        } catch (RestClientResponseException e) {
            // 404 sulla search = set senza carte (es. appena annunciato)
            if (e.getStatusCode().value() == 404)
                return new ScryfallCardList(List.of(), false, null, 0);
            log.error("Scryfall search: HTTP {}", e.getStatusCode(), e);
            throw new MtgException(msg.get("sync.errore.comunicazione"));
        } catch (Exception e) {
            log.error("Scryfall search: errore di rete", e);
            throw new MtgException(msg.get("sync.errore.comunicazione"));
        }
    }

    private void pausa() {
        try {
            Thread.sleep(PAUSA_TRA_PAGINE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================================================================
    // UPSERT
    // ==================================================================

    private Espansione upsertEspansione(ScryfallSet s) {
        Espansione e = espansioneR.findByCodice(s.code()).orElseGet(Espansione::new);
        boolean nuova = e.getId() == null;
        e.setCodice(s.code());
        e.setNome(s.name());
        e.setTipoSet(s.setType());
        e.setCodiceSetPadre(s.parentSetCode());
        e.setDataUscita(s.releasedAt());
        e.setIconUrl(s.iconSvgUri());
        e.setNumeroCarte(s.cardCount());
        espansioneR.save(e);
        log.debug("espansione {} {}", s.code(), nuova ? "creata" : "aggiornata");
        return e;
    }

    private void importaCarta(ScryfallCard card, Espansione espansione) {
        // Schede accessorie senza oracle_id (es. art series): skip
        if (card.oracleId() == null) {
            log.debug("skip {} (senza oracle_id)", card.name());
            return;
        }

        Carta carta = upsertCarta(card);
        Stampa stampa = upsertStampa(card, carta, espansione);
        creaProdottoSePrimaVolta(card, stampa, espansione);
        registraPrezzo(card, stampa);
    }

    /** LIVELLO ORACLE: la carta-concetto, unica per oracleId. */
    private Carta upsertCarta(ScryfallCard card) {
        Carta c = cartaR.findByOracleId(card.oracleId()).orElseGet(Carta::new);
        boolean nuova = c.getId() == null;

        c.setOracleId(card.oracleId());
        c.setNome(card.name());
        c.setCostoMana(card.manaCost());
        c.setValoreMana(card.cmc());
        c.setTipoRiga(card.typeLine());
        c.setTestoOracle(card.oracleText());
        c.setForza(card.power());
        c.setCostituzione(card.toughness());
        //c.setColori(joinNoSep(card.colors()));
        c.setColori(coloriDi(card));
        c.setIdentitaColore(joinNoSep(card.colorIdentity()));
        c.setParoleChiave(joinCsv(card.keywords()));
        c.setLegal(toJson(card.legalities()));
        c.setCardFaces(card.cardFaces() == null ? null : toJson(card.cardFaces()));
        cartaR.save(c);

        if (nuova) carteNuove++; else carteAggiornate++;
        return c;
    }

    /** LIVELLO PUBBLICAZIONE: la stampa, unica per scryfallId. */
    private Stampa upsertStampa(ScryfallCard card, Carta carta, Espansione espansione) {
        Stampa s = stampaR.findByScryfallId(card.id()).orElseGet(Stampa::new);
        boolean nuova = s.getId() == null;
        vistiScryfallIds.add(card.id());

        s.setScryfallId(card.id());
        s.setCarta(carta);
        s.setEspansione(espansione);
        s.setNumeroCollezione(card.collectorNumber());
        s.setRarita(mappaRarita(card.rarity()));
        s.setArtista(card.artist());
        s.setPromo(Boolean.TRUE.equals(card.promo()));
        List<String> finiture = card.finishes() == null ? List.of() : card.finishes();
        s.setHasNonFoil(finiture.contains("nonfoil"));
        s.setHasFoil(finiture.contains("foil"));
        s.setHasEtchedFoil(finiture.contains("etched"));
        s.setEffettiCornice(joinCsv(card.frameEffects()));
        s.setTipiPromo(joinCsv(card.promoTypes()));
        s.setImageUrl(card.immagine());
        if (card.multiverseIds() != null && !card.multiverseIds().isEmpty())
            s.setMultiverseId(card.multiverseIds().get(0));
        s.setCardmarketId(card.cardmarketId());
        stampaR.save(s);

        if (nuova) stampeNuove++; else stampeAggiornate++;
        return s;
    }

    /**
     * LIVELLO COMMERCIALE: il prodotto SINGLE nasce SOLO alla prima
     * importazione della stampa — nome, slug e attivo poi appartengono
     * all'admin, il sync non li ritocca mai (rispetto del lavoro manuale).
     */
    private void creaProdottoSePrimaVolta(ScryfallCard card, Stampa stampa, Espansione espansione) {
        if (prodottoR.existsByStampaId(stampa.getId()))
            return;

        Prodotto p = new Prodotto();
        p.setTipoProdotto(TipoProdotto.SINGLE);
        p.setStampa(stampa);
        p.setEspansione(espansione);
        // "Sol Ring (CMM 464)" — nome disambiguato da set e numero
        p.setNome("%s (%s %s)".formatted(card.name(),
                espansione.getCodice().toUpperCase(), card.collectorNumber()));
        p.setSlug(generaSlug(p.getNome()));
        p.setImageUrl(card.immagine());
        prodottoR.save(p);
        prodottiCreati++;
    }

    /**
     * Serie storica dei prezzi: una rilevazione PER FINITURA a ogni sync
     * (tabella append-only: mai update, solo nuovi rilevamenti).
     * I prezzi EUR di Scryfall arrivano da Cardmarket e sono indicatori
     * di tendenza: finiscono in prezzoTrend; medio/min restano alle
     * fonti che li forniscono davvero.
     */
    private void registraPrezzo(ScryfallCard card, Stampa stampa) {
        if (card.prices() == null) return;

        salvaRilevazione(stampa, Finitura.NONFOIL, card.prices().eur());
        salvaRilevazione(stampa, Finitura.FOIL, card.prices().eurFoil());
    }

    private void salvaRilevazione(Stampa stampa, Finitura finitura, BigDecimal prezzo) {
        if (prezzo == null) return;   // Scryfall non ha il prezzo per questa finitura

        PrezzoRiferimento pr = new PrezzoRiferimento();
        pr.setStampa(stampa);
        pr.setFonte(FontePrezzo.SCRYFALL);
        pr.setFinitura(finitura);
        pr.setPrezzoTrend(prezzo);
        // valuta "EUR" e detectionDate: default dell'entity (@CreationTimestamp)
        prezzoR.save(pr);
    }

    // ==================================================================
    // Helper
    // ==================================================================

    /** "mythic" -> MYTHIC; valori nuovi/ignoti -> SPECIAL (mai esplodere). */
    private Rarita mappaRarita(String rarity) {
        if (rarity == null) return Rarita.SPECIAL;
        try {
            return Rarita.valueOf(rarity.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("rarita' sconosciuta da Scryfall: {}", rarity);
            return Rarita.SPECIAL;
        }
    }

    
    private String joinNoSep(List<String> valori) {
        return valori == null ? "" : String.join("", valori);
    }

    private String joinCsv(List<String> valori) {
        return (valori == null || valori.isEmpty()) ? null : String.join(",", valori);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.warn("serializzazione JSON fallita", e);
            return null;
        }
    }

    private String generaSlug(String nome) {
        String base = nome.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String slug = base;
        int i = 2;
        while (prodottoR.existsBySlug(slug))
            slug = base + "-" + i++;
        return slug;
    }
    
    private String coloriDi(ScryfallCard card) {
        if (card.colors() != null)
            return String.join("", card.colors());

        if (card.cardFaces() != null) {
            Set<String> unione = new HashSet<>();
            for (ScryfallCardFace f : card.cardFaces())
                if (f.colors() != null) unione.addAll(f.colors());
            StringBuilder sb = new StringBuilder();
            for (char c : ORDINE_COLORI.toCharArray())
                if (unione.contains(String.valueOf(c))) sb.append(c);
            return sb.toString();
        }
        return "";
    }
    
}