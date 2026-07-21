package com.betacom.mtgbazar.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling accende i job @Scheduled: senza, la pulizia dei
 * refresh token (PuliziaRefreshTokenJob) sarebbe un bean inerte.
 */
@SpringBootApplication
@EnableScheduling
public class PlaneswalkerBazarBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlaneswalkerBazarBackendApplication.class, args);
	}

}