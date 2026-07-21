package com.betacom.mtgbazar.be.dto.users;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Una riga dello storico transazioni. importo sempre positivo + tipo:
 * e' il frontend che mostra il segno (RICARICA/RIMBORSO in verde,
 * PRELIEVO/PAGAMENTO_ORDINE in rosso).
 *
 * utenteId/utenteUsername/utenteNome: popolati SOLO nella vista ADMIN
 * (storico globale), dove serve sapere DI CHI e' il movimento. Nello
 * storico personale del cliente restano null (e' gia' il suo).
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentoDTO {
    private Long id;
    private String tipo;
    private String metodo;
    private String stato;
    private BigDecimal importo;
    private BigDecimal commissione;
    private String riferimentoEsterno;
    private String descrizione;
    private Long ordineId;
    private Long contoBancarioId;
    private LocalDateTime creationDate;
    private LocalDateTime completionDate;

    // --- Solo vista admin ---
    private Long utenteId;
    private String utenteUsername;
    private String utenteNome;
    
}