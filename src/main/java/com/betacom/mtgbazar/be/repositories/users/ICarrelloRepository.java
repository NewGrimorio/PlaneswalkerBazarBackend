package com.betacom.mtgbazar.be.repositories.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.users.Carrello;

@Repository
public interface ICarrelloRepository extends JpaRepository<Carrello, Long> {

    /** Un carrello per utente; se assente lo crea il service al primo add. */
    Optional<Carrello> findByUtenteId(Long utenteId);
}