package com.betacom.mtgbazar.be.model.users;

import java.math.BigDecimal;
import java.time.LocalDateTime;
 
import org.hibernate.annotations.CreationTimestamp;

import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.StatoMovimento;
import com.betacom.mtgbazar.be.model.users.enums.TipoMovimento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
/**
 * Ledger APPEND-ONLY del portafoglio: ogni operazione è una riga che
 * non si modifica mai — l'unica transizione ammessa è sullo stato
 * (IN_ATTESA -> COMPLETATO/RIFIUTATO/ANNULLATO) + completionDate.
 * Lo storico transazioni dell'utente è una SELECT su questa tabella.
 *
 * importo: sempre positivo, il segno lo determina il tipo.
 * commissione: es. PayPal 5% + 0,35 EUR, esplicita e visibile all'utente.
 */
@Entity
@Table(name = "movimento_portafoglio")
@Getter
@Setter
@NoArgsConstructor
public class MovimentoPortafoglio {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portafoglio_id", nullable = false)
    private Portafoglio portafoglio;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimento tipo;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MetodoMovimento metodo = MetodoMovimento.INTERNO;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatoMovimento stato = StatoMovimento.IN_ATTESA;
 
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importo;
 
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commissione = BigDecimal.ZERO;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conto_bancario_id")
    private ContoBancario contoBancario;   // obbligatorio per PRELIEVO
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordine_id")
    private Ordine ordine;                 // per PAGAMENTO_ORDINE/RIMBORSO
 
    @Column(name = "riferimento_esterno", length = 100)
    private String riferimentoEsterno;     // transaction id PayPal / CRO bonifico
 
    @Column(length = 300)
    private String descrizione;
 
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;
 
    @Column(name = "completion_date")
    private LocalDateTime completionDate;
}