package com.betacom.mtgbazar.be.repositories.users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.ContoBancario;

@Repository
public interface IContoBancarioRepository extends JpaRepository<ContoBancario, Long> {

    List<ContoBancario> findByUtenteIdAndAttivoTrue(Long utenteId);

    /** Ownership check: il conto per il prelievo deve essere dell'utente. */
    Optional<ContoBancario> findByIdAndUtenteId(Long id, Long utenteId);
}