package com.betacom.mtgbazar.be.request.users;

import java.math.BigDecimal;

import com.betacom.mtgbazar.be.model.users.enums.MetodoMovimento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Ricarica del portafoglio (schermata "Credito").
 * L'importo e' il LORDO richiesto; la commissione (PayPal 5% + 0,35)
 * la calcola il service, mai il client. Metodo INTERNO non e' ammesso
 * qui: lo genera solo il sistema (verifica nel service).
 */
@Getter
@Setter
@ToString
public class RicaricaReq {

	/** FASE C: valorizzato dal controller dal token, mai dal client. */
    private Long utenteId;

    @NotNull(message = "portafoglio.no.importo")
    @DecimalMin(value = "0.01", message = "portafoglio.importo.min")
    @Digits(integer = 10, fraction = 2, message = "portafoglio.importo.invalid")
    private BigDecimal importo;

    @NotNull(message = "portafoglio.no.metodo")
    private MetodoMovimento metodo;

    /** Transaction id PayPal / CRO del bonifico dichiarato. */
    @Size(max = 100, message = "portafoglio.riferimento.maxlength")
    private String riferimentoEsterno;
}