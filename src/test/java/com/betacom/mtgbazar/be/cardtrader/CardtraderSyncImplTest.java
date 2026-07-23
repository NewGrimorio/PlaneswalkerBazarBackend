package com.betacom.mtgbazar.be.cardtrader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.betacom.mtgbazar.be.dto.products.CardtraderSyncDTO;
import com.betacom.mtgbazar.be.services.implementations.products.CardtraderArricchimentoTx;
import com.betacom.mtgbazar.be.services.implementations.products.CardtraderSyncImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * Collaudo dell'ORCHESTRAZIONE a mappa globale, con HTTP stubbato via
 * MockRestServiceServer e il Tx mockato. Nessun contesto Spring, nessuna
 * rete: verifichiamo solo il comportamento del giro.
 *
 * Rispetto alla versione per-set: l'impl non usa piu' IEspansioneRepository
 * (le espansioni arrivano da /expansions di Cardtrader), costruisce UNA
 * mappa scryfall_id -> blueprint_id da tutte le espansioni, e chiama il Tx
 * una sola volta con quella mappa.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class CardtraderSyncImplTest {

    private static final String BASE = "http://cardtrader.test/api/v2";

    @Mock private CardtraderArricchimentoTx arricchimentoTx;

    private MockRestServiceServer server;
    private CardtraderSyncImpl impl;

    @BeforeEach
    public void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        impl = new CardtraderSyncImpl(client, arricchimentoTx);
    }

    @Test
    public void unEspansioneCheFallisceNonFermaLeAltre() {
        log.debug("TEST: /blueprints/export va in errore su un'espansione, "
                + "l'altra confluisce comunque nella mappa");

        // /expansions: due espansioni Magic (game_id=1)
        server.expect(once(), requestTo(BASE + "/expansions"))
              .andExpect(method(GET))
              .andRespond(withSuccess("""
                  [ {"id":10,"game_id":1,"code":"aaa","name":"A"},
                    {"id":20,"game_id":1,"code":"bbb","name":"B"} ]
                  """, APPLICATION_JSON));

        // export di id=10 -> 500: deve essere ingoiato e non fermare il giro
        server.expect(once(), requestTo(BASE + "/blueprints/export?expansion_id=10"))
              .andExpect(method(GET))
              .andRespond(withServerError());

        // export di id=20 -> ok: un blueprint con scryfall_id valido
        server.expect(once(), requestTo(BASE + "/blueprints/export?expansion_id=20"))
              .andExpect(method(GET))
              .andRespond(withSuccess("""
                  [ {"id":2001,"name":"BP","game_id":1,"category_id":1,
                     "expansion_id":20,
                     "scryfall_id":"381854e2-7369-473e-a604-4dd7c010fc89",
                     "card_market_ids":[]} ]
                  """, APPLICATION_JSON));

        // mappa non vuota -> il Tx viene invocato UNA volta con la mappa globale
        when(arricchimentoTx.arricchisciDaMappa(anyMap()))
              .thenReturn(new CardtraderArricchimentoTx.Esito(1, 28));

        CardtraderSyncDTO dto = impl.sincronizzaBlueprint();

        assertEquals(1, dto.getEspansioniElaborate());   // solo id=20 e' andata a buon fine
        assertEquals(1, dto.getStampeAggiornate());
        assertEquals(28, dto.getBlueprintSenzaCorrispondenza());
        verify(arricchimentoTx, times(1)).arricchisciDaMappa(anyMap());
        server.verify();
    }

    @Test
    public void expansionsGiuInterrompeInModoPulito() {
        log.debug("TEST: se /expansions fallisce, lista vuota -> stop pulito, niente Tx");

        server.expect(once(), requestTo(BASE + "/expansions"))
              .andExpect(method(GET))
              .andRespond(withServerError());

        CardtraderSyncDTO dto = impl.sincronizzaBlueprint();

        assertEquals(0, dto.getEspansioniElaborate());
        assertEquals(0, dto.getStampeAggiornate());
        verifyNoInteractions(arricchimentoTx);   // non si arriva mai al Tx
        server.verify();
    }
}