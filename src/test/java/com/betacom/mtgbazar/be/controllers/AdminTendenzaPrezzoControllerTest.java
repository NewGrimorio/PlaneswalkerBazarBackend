package com.betacom.mtgbazar.be.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.betacom.mtgbazar.be.dto.products.TendenzaPrezzoCartaDTO;
import com.betacom.mtgbazar.be.dto.products.TendenzaSkuDTO;
import com.betacom.mtgbazar.be.services.interfaces.products.ITendenzaPrezzoServices;

import lombok.extern.slf4j.Slf4j;

/**
 * Test MockMvc di AdminTendenzaPrezzoController. Il service è mockato con
 * @MockitoBean perché dietro fa chiamate HTTP esterne (Scryfall/Cardtrader)
 * irraggiungibili in test: qui verifichiamo SOLO il contratto REST —
 * mapping del path, path variable, e serializzazione JSON del DTO.
 *
 * NB: @MockitoBean sostituisce un bean nel contesto, quindi questa classe
 * NON condivide la cache di contesto con gli altri test: ne carica uno suo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
public class AdminTendenzaPrezzoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ITendenzaPrezzoServices tendenzaS;

    @Test
    public void tendenzeRestituisceIlDtoDellaCarta() throws Exception {
        log.debug("TEST: GET /stampa/{id}/tendenze -> 200 + JSON del DTO");

        TendenzaSkuDTO riga = TendenzaSkuDTO.builder()
                .skuId(100L)
                .condizione("NM").lingua("en").finitura("NONFOIL")
                .ctLowest(new BigDecimal("10.50"))
                .ctMarket(new BigDecimal("10.75"))
                .ctPrecedente(new BigDecimal("9.00"))
                .cardmarket(new BigDecimal("10.00"))
                .cardmarketPrecedente(new BigDecimal("8.00"))
                .valuta("EUR")
                .build();

        TendenzaPrezzoCartaDTO dto = TendenzaPrezzoCartaDTO.builder()
                .stampaId(1L)
                .nomeCarta("Sol Ring")
                .codiceSet("cmm")
                .righe(List.of(riga))
                .millisecondiImpiegati(1200L)
                .build();

        when(tendenzaS.tendenzeCarta(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/magazzino/stampa/{id}/tendenze", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stampaId").value(1))
                .andExpect(jsonPath("$.nomeCarta").value("Sol Ring"))
                .andExpect(jsonPath("$.codiceSet").value("cmm"))
                .andExpect(jsonPath("$.righe.length()").value(1))
                .andExpect(jsonPath("$.righe[0].skuId").value(100))
                .andExpect(jsonPath("$.righe[0].condizione").value("NM"))
                .andExpect(jsonPath("$.righe[0].finitura").value("NONFOIL"))
                .andExpect(jsonPath("$.righe[0].ctLowest").value(10.50))
                .andExpect(jsonPath("$.righe[0].cardmarket").value(10.00));

        // Il path variable arriva davvero al service
        verify(tendenzaS).tendenzeCarta(eq(1L));
    }
    
}