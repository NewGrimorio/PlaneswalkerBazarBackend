package com.betacom.mtgbazar.be.cardtrader;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.betacom.mtgbazar.be.model.products.enums.Condizione;

/**
 * Traduttore di condizione al confine con Cardtrader. Tiene la logica di
 * mapping fuori dall'enum CondizioneCardtrader, che resta un semplice
 * elenco di valori con i dati di corrispondenza dichiarati.
 *
 * Le due mappe sono costruite UNA volta sola dai dati dell'enum: se domani
 * si sposta una condizione da un grado CT a un altro, basta toccare la riga
 * dell'enum e la traduzione si riallinea da sola.
 */
public final class TraduttoreCondizioneCT {

    private TraduttoreCondizioneCT() { }

    // La NOSTRA Condizione (7 gradi Cardmarket) -> grado Cardtrader.
    // NA (finish-level) non compare in nessun corCmrkt() -> assente -> null.
    private static final Map<Condizione, CondizioneCardtrader> PER_CONDIZIONE =
            Arrays.stream(CondizioneCardtrader.values())
                    .flatMap(ct -> ct.corCmrkt().stream()
                            .map(c -> Map.entry(c, ct)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Etichetta Cardtrader (case-insensitive) -> enum, per leggere properties.condition.
    private static final Map<String, CondizioneCardtrader> PER_ETICHETTA =
            Arrays.stream(CondizioneCardtrader.values())
                    .collect(Collectors.toMap(
                            ct -> ct.etichetta().toLowerCase(),
                            Function.identity()));

    /** La NOSTRA condizione -> scala Cardtrader. NA (finish-level) -> null. */
    public static CondizioneCardtrader daCondizione(Condizione c) {
        return PER_CONDIZIONE.get(c);
    }

    /** properties.condition di Cardtrader -> enum. Sconosciuta/null -> null. */
    public static CondizioneCardtrader daEtichetta(String s) {
        return s == null ? null : PER_ETICHETTA.get(s.trim().toLowerCase());
    }
    
}