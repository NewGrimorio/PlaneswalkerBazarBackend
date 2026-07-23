package com.betacom.mtgbazar.be.tendenza;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.betacom.mtgbazar.be.dto.products.TendenzaPrezzoCartaDTO;
import com.betacom.mtgbazar.be.dto.products.TendenzaSkuDTO;
import com.betacom.mtgbazar.be.model.products.Carta;
import com.betacom.mtgbazar.be.model.products.Espansione;
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
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.betacom.mtgbazar.be.services.implementations.products.TendenzaPrezzoImpl;
import com.betacom.mtgbazar.be.services.implementations.products.TendenzaSnapshotTx;
import com.betacom.mtgbazar.be.services.implementations.products.TendenzaSnapshotTx.SnapshotSpec;

import lombok.extern.slf4j.Slf4j;

/**
 * Collaudo end-to-end dell'orchestratore delle tendenze, con HTTP stubbato
 * (due MockRestServiceServer: Scryfall + Cardtrader) e repository/Tx mockati.
 * Nessun contesto Spring: verifichiamo il raggruppamento per (finitura,lingua),
 * il bucket per condizione, l'aggancio Cardmarket per finitura, i precedenti
 * e gli snapshot raccolti.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class TendenzaPrezzoImplTest {

    private static final String CT_BASE = "http://cardtrader.test/api/v2";
    private static final String SF_BASE = "http://scryfall.test";
    private static final UUID SCRYFALL_ID =
            UUID.fromString("381854e2-7369-473e-a604-4dd7c010fc89");

    @Mock private IStampaRepository stampaR;
    @Mock private IProdottoRepository prodottoR;
    @Mock private IMagazzinoSKURepository skuR;
    @Mock private IPrezzoRiferimentoRepository prezzoR;
    @Mock private IMessaggioServices msg;
    @Mock private TendenzaSnapshotTx snapshotTx;

    private MockRestServiceServer ctServer;
    private MockRestServiceServer sfServer;
    private TendenzaPrezzoImpl impl;

    @BeforeEach
    public void setup() {
        RestClient.Builder ctBuilder = RestClient.builder().baseUrl(CT_BASE);
        ctServer = MockRestServiceServer.bindTo(ctBuilder).build();
        RestClient ctClient = ctBuilder.build();

        RestClient.Builder sfBuilder = RestClient.builder().baseUrl(SF_BASE);
        sfServer = MockRestServiceServer.bindTo(sfBuilder).build();
        RestClient sfClient = sfBuilder.build();

        // Ordine costruttore: cardtrader, scryfall, stampaR, prodottoR, skuR, prezzoR, msg, tx
        impl = new TendenzaPrezzoImpl(ctClient, sfClient,
                stampaR, prodottoR, skuR, prezzoR, msg, snapshotTx);
    }

    private MagazzinoSKU sku(long id, Condizione cond, String lingua, Finitura fin) {
        MagazzinoSKU s = new MagazzinoSKU();
        s.setId(id);
        s.setCondizione(cond);
        s.setLingua(lingua);
        s.setFinitura(fin);
        return s;
    }

    @Test
    public void tendenzeCartaCompleta() {
        log.debug("TEST: 3 SKU, 2 gruppi CT + 1 Scryfall, bucket per condizione e snapshot");

        // --- Entità: stampa/prodotto/sku ---
        Carta carta = new Carta();       carta.setNome("Sol Ring");
        Espansione esp = new Espansione(); esp.setCodice("cmm");
        Stampa stampa = new Stampa();
        stampa.setId(1L);
        stampa.setScryfallId(SCRYFALL_ID);
        stampa.setCardtraderBlueprintId(555);
        stampa.setCarta(carta);
        stampa.setEspansione(esp);

        Prodotto prod = new Prodotto(); prod.setId(10L);

        MagazzinoSKU nmNonfoil = sku(100L, Condizione.NM, "en", Finitura.NONFOIL);
        MagazzinoSKU nmFoil    = sku(200L, Condizione.NM, "en", Finitura.FOIL);
        MagazzinoSKU lpNonfoil = sku(300L, Condizione.LP, "en", Finitura.NONFOIL);

        when(stampaR.findByIdFetchCartaEspansione(1L)).thenReturn(Optional.of(stampa));
        when(prodottoR.findByStampaId(1L)).thenReturn(Optional.of(prod));
        when(skuR.findByProdottoIdAndAttivoTrue(10L))
                .thenReturn(List.of(nmNonfoil, nmFoil, lpNonfoil));

        // --- Precedenti: solo per NM/nonfoil (gli altri -> Optional.empty di default) ---
        PrezzoRiferimento prCt = new PrezzoRiferimento();  prCt.setPrezzoMin(new BigDecimal("9.00"));
        PrezzoRiferimento prCmk = new PrezzoRiferimento(); prCmk.setPrezzoTrend(new BigDecimal("8.00"));
        when(prezzoR.findFirstByStampaIdAndFonteAndFinituraAndCondizioneAndLinguaOrderByDetectionDateDesc(
                1L, FontePrezzo.CARDTRADER, Finitura.NONFOIL, Condizione.NM, "en"))
                .thenReturn(Optional.of(prCt));
        when(prezzoR.findFirstByStampaIdAndFonteAndFinituraAndCondizioneAndLinguaOrderByDetectionDateDesc(
                1L, FontePrezzo.SCRYFALL, Finitura.NONFOIL, Condizione.NA, "NA"))
                .thenReturn(Optional.of(prCmk));

        // --- HTTP Scryfall: eur/eur_foil/eur_etched ---
        sfServer.expect(once(), requestTo(SF_BASE + "/cards/" + SCRYFALL_ID))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                    {"prices":{"eur":"10.00","eur_foil":"25.00","eur_etched":"40.00"}}
                    """, APPLICATION_JSON));

        // --- HTTP Cardtrader gruppo 1: nonfoil ---
        ctServer.expect(once(), requestTo(
                CT_BASE + "/marketplace/products?blueprint_id=555&foil=false&language=en"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                    {"555":[
                      {"price":{"cents":500,"currency":"EUR"},
                       "properties_hash":{"condition":"Moderately Played","mtg_language":"en","mtg_foil":false}},
                      {"price":{"cents":600,"currency":"EUR"},
                       "properties_hash":{"condition":"Moderately Played","mtg_language":"en","mtg_foil":false}},
                      {"price":{"cents":1050,"currency":"EUR"},
                       "properties_hash":{"condition":"Near Mint","mtg_language":"en","mtg_foil":false}},
                      {"price":{"cents":1100,"currency":"EUR"},
                       "properties_hash":{"condition":"Near Mint","mtg_language":"en","mtg_foil":false}}
                    ]}
                    """, APPLICATION_JSON));

        // --- HTTP Cardtrader gruppo 2: foil ---
        ctServer.expect(once(), requestTo(
                CT_BASE + "/marketplace/products?blueprint_id=555&foil=true&language=en"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                    {"555":[
                      {"price":{"cents":2600,"currency":"EUR"},
                       "properties_hash":{"condition":"Near Mint","mtg_language":"en","mtg_foil":true}},
                      {"price":{"cents":2700,"currency":"EUR"},
                       "properties_hash":{"condition":"Near Mint","mtg_language":"en","mtg_foil":true}}
                    ]}
                    """, APPLICATION_JSON));

        // --- ESECUZIONE ---
        TendenzaPrezzoCartaDTO dto = impl.tendenzeCarta(1L);

        // --- Verifiche righe ---
        assertEquals(3, dto.getRighe().size());

        TendenzaSkuDTO r1 = riga(dto, 100L);   // NM nonfoil
        assertEquals(0, r1.getCtLowest().compareTo(new BigDecimal("10.50")));
        assertEquals(0, r1.getCtMarket().compareTo(new BigDecimal("10.75")));   // (10.50+11.00)/2
        assertEquals(0, r1.getCardmarket().compareTo(new BigDecimal("10.00")));
        assertEquals(0, r1.getCtPrecedente().compareTo(new BigDecimal("9.00")));
        assertEquals(0, r1.getCardmarketPrecedente().compareTo(new BigDecimal("8.00")));

        TendenzaSkuDTO r2 = riga(dto, 200L);   // NM foil
        assertEquals(0, r2.getCtLowest().compareTo(new BigDecimal("26.00")));
        assertEquals(0, r2.getCardmarket().compareTo(new BigDecimal("25.00")));

        TendenzaSkuDTO r3 = riga(dto, 300L);   // LP nonfoil -> bucket Moderately Played
        assertEquals(0, r3.getCtLowest().compareTo(new BigDecimal("5.00")));
        assertEquals(0, r3.getCardmarket().compareTo(new BigDecimal("10.00")));

        // --- Verifica snapshot raccolti: 3 CT (uno per SKU) + 2 Cardmarket (nonfoil, foil) ---
        ArgumentCaptor<List<SnapshotSpec>> cap = ArgumentCaptor.forClass(List.class);
        verify(snapshotTx).salva(cap.capture());
        assertEquals(5, cap.getValue().size());

        sfServer.verify();
        ctServer.verify();
    }

    private TendenzaSkuDTO riga(TendenzaPrezzoCartaDTO dto, long skuId) {
        return dto.getRighe().stream()
                .filter(r -> r.getSkuId().equals(skuId))
                .findFirst().orElseThrow();
    }
}