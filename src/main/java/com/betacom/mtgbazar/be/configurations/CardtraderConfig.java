package com.betacom.mtgbazar.be.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * RestClient dedicato a Cardtrader: base URL dell'API v2 + Bearer token
 * come header di default su ogni chiamata (come richiede la loro auth).
 *
 * Il token ha default vuoto ("${...:}"): se manca, il client si costruisce
 * comunque e le chiamate falliranno con 401 — intercettato e loggato dal
 * sync, che prosegue best-effort senza far esplodere l'avvio.
 */
@Configuration
public class CardtraderConfig {

    @Bean
    RestClient cardtraderRestClient(
            @Value("${app.cardtrader.base-url}") String baseUrl,
            @Value("${app.cardtrader.token:}")   String token) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }
    
}