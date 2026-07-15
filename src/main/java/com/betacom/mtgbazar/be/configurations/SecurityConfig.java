package com.betacom.mtgbazar.be.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Un solo bean: l'encoder BCrypt di spring-security-crypto.
 * Nel progetto NON c'e' Spring Security completo: solo l'hashing.
 * BCrypt genera il salt da solo e lo include nell'hash; la verifica
 * si fa esclusivamente con matches(), mai confrontando stringhe.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}