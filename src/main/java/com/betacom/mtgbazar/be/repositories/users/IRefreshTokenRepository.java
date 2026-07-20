package com.betacom.mtgbazar.be.repositories.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.RefreshToken;

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
    @Query("UPDATE RefreshToken r SET r.revocato = true "
         + "WHERE r.famiglia = :famiglia AND r.revocato = false")
    int revocaFamiglia(@Param("famiglia") String famiglia);
    
}