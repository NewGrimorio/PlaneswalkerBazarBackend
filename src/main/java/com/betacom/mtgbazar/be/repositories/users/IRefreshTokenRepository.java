package com.betacom.mtgbazar.be.repositories.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.RefreshToken;

/**
 * Le JPQL delle due revoche vivono in META-INF/jpa-named-queries.properties
 * (RefreshToken.revocaFamiglia, RefreshToken.revocaTutteByUtenteId),
 * risolte per convenzione NomeEntity.nomeMetodo. Qui resta SOLO
 * l'annotazione di comportamento @Modifying — come @Lock altrove:
 * il "come eseguire" sta sul metodo, il "cosa eseguire" nelle properties.
 */
@Repository
public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Lookup di validazione: si cerca per HASH, mai per token in chiaro. */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoca dell'INTERA famiglia in un colpo solo: e' la risposta al
     * riuso di un token consumato (furto) e al logout. Bulk update:
     * una query, niente ciclo load-modify-save.
     */
    @Modifying
    int revocaFamiglia(@Param("famiglia") String famiglia);

    /**
     * Revoca di TUTTE le sessioni di un utente, su ogni dispositivo
     * (tutte le famiglie insieme): l'arma del cambio password, e in
     * futuro della disattivazione account. Stesso stile bulk.
     */
    @Modifying
    int revocaTutteByUtenteId(@Param("utenteId") Long utenteId);
}