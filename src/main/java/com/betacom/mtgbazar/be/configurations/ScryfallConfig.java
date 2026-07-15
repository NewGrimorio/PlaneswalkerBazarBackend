package com.betacom.mtgbazar.be.configurations;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
 
/**
 * Client HTTP verso l'API Scryfall (https://scryfall.com/docs/api).
 * Regole della casa Scryfall:
 *  - User-Agent identificativo OBBLIGATORIO (senza: 403)
 *  - massimo ~10 richieste/secondo (noi: pausa tra le pagine)
 * Il builder iniettato e' quello autoconfigurato da Boot: usa
 * l'ObjectMapper dell'applicazione (unknown properties ignorate).
 */


@Configuration
public class ScryfallConfig {
 
	@Bean
	public RestClient scryfallRestClient() {
	    return RestClient.builder()                    // builder STATICO: nessun bean richiesto
	            .baseUrl("https://api.scryfall.com")
	            .defaultHeader("User-Agent", "PlaneswalkerBazar/1.0")
	            .defaultHeader("Accept", "application/json")
	            .build();
	}
}
