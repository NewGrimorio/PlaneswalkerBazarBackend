package com.betacom.mtgbazar.be.repositories.users;

import java.util.List;
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
 
import com.betacom.mtgbazar.be.model.users.MovimentoPortafoglio;
import com.betacom.mtgbazar.be.model.users.enums.StatoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;
import org.springframework.data.repository.query.Param;
 
public interface IMovimentoPortafoglioRepository extends JpaRepository<MovimentoPortafoglio, Long> {
 
    /** Lo storico transazioni dell'utente ("Tutte le transazioni"). */
    List<MovimentoPortafoglio> findByPortafoglioIdOrderByCreationDateDesc(Long portafoglioId);
 
    /** Pannello admin: i movimenti IN_ATTESA da lavorare. */
    List<MovimentoPortafoglio> findByStatoOrderByCreationDateAsc(StatoMovimento stato);
 
    /** Ownership check via portafoglio. */
    Optional<MovimentoPortafoglio> findByIdAndPortafoglioId(Long id, Long portafoglioId);

    /** Storico globale admin (concluso), filtri opzionali. Named query. */
    List<MovimentoPortafoglio> storicoAdmin(@Param("stato") StatoMovimento stato,
                                            @Param("metodo") MetodoMovimento metodo);
    
    /** Dashboard: quanti movimenti in un dato stato (es. IN_ATTESA). */
    long countByStato(StatoMovimento stato);
    
}
 