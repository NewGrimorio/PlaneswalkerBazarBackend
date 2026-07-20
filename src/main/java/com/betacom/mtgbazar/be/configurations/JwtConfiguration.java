package com.betacom.mtgbazar.be.configurations;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.betacom.mtgbazar.be.dto.ResponseDTO;
import com.betacom.mtgbazar.be.services.IMessaggioServices;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Il lato VALIDAZIONE del JWT (il lato generazione e' JwtImpl):
 * decoder Nimbus sulla stessa chiave HS512 e converter che legge il
 * claim "roles" (gia' prefissato ROLE_, quindi prefisso vuoto qui).
 *
 * Entry point e denied handler: i 401/403 della filter chain nascono
 * PRIMA dei controller e il GlobalExceptionHandler non li vede mai.
 * Questi due bean scrivono lo stesso {"msg": ...} del resto del
 * progetto — il contratto errori del frontend regge fino in fondo.
 */
@Configuration
public class JwtConfiguration {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        return NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)   // identico alla generazione
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();
        rolesConverter.setAuthoritiesClaimName("roles");
        rolesConverter.setAuthorityPrefix("");   // il claim contiene gia' ROLE_...

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesConverter);
        return converter;
    }

    /** 401 in JSON: niente token o token invalido/scaduto. */
    @Bean
    public AuthenticationEntryPoint authEntryPoint(IMessaggioServices msg, ObjectMapper om) {
        return (request, response, ex) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            om.writeValue(response.getWriter(),
                    ResponseDTO.builder().msg(msg.get("auth.non.autenticato")).build());
        };
    }

    /** 403 in JSON: autenticato ma senza il ruolo richiesto. */
    @Bean
    public AccessDeniedHandler accessDeniedHandler(IMessaggioServices msg, ObjectMapper om) {
        return (request, response, ex) -> {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            om.writeValue(response.getWriter(),
                    ResponseDTO.builder().msg(msg.get("auth.accesso.negato")).build());
        };
    }
}