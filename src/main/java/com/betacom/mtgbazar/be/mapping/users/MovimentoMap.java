package com.betacom.mtgbazar.be.mapping.users;

import java.util.Collection;
import java.util.List;

import com.betacom.mtgbazar.be.dto.users.MovimentoDTO;
import com.betacom.mtgbazar.be.model.users.MovimentoPortafoglio;
import com.betacom.mtgbazar.be.model.users.Utente;

public class MovimentoMap {

    /*
     * Vista PUBBLICA/CLIENTE. ordineId/contoBancarioId: leggere SOLO
     * l'id di una relazione LAZY non scatena il proxy — unico
     * attraversamento sicuro senza fetch. Nessun dato dell'utente qui.
     */
    public static MovimentoDTO buildMovimentoDTO(MovimentoPortafoglio m) {
        return builderComune(m).build();
    }

    /*
     * Vista ADMIN (storico globale). PRE-REQUISITO: portafoglio.utente
     * fetchato (named query MovimentoPortafoglio.storicoAdmin), perche'
     * qui si legge username/nome/cognome: l'admin deve sapere DI CHI e'.
     */
    public static MovimentoDTO buildMovimentoDTOAdmin(MovimentoPortafoglio m) {
        Utente u = m.getPortafoglio().getUtente();
        return builderComune(m)
                .utenteId(u.getId())
                .utenteUsername(u.getUsername())
                .utenteNome(u.getNome() + " " + u.getCognome())
                .build();
    }

    public static List<MovimentoDTO> buildMovimentoDTOList(Collection<MovimentoPortafoglio> lM) {
        return lM.stream().map(m -> buildMovimentoDTO(m)).toList();
    }

    public static List<MovimentoDTO> buildMovimentoDTOAdminList(Collection<MovimentoPortafoglio> lM) {
        return lM.stream().map(m -> buildMovimentoDTOAdmin(m)).toList();
    }

    private static MovimentoDTO.MovimentoDTOBuilder builderComune(MovimentoPortafoglio m) {
        return MovimentoDTO.builder()
                .id(m.getId())
                .tipo(m.getTipo().name())
                .metodo(m.getMetodo().name())
                .stato(m.getStato().name())
                .importo(m.getImporto())
                .commissione(m.getCommissione())
                .riferimentoEsterno(m.getRiferimentoEsterno())
                .descrizione(m.getDescrizione())
                .ordineId(m.getOrdine() == null ? null : m.getOrdine().getId())
                .contoBancarioId(m.getContoBancario() == null ? null : m.getContoBancario().getId())
                .creationDate(m.getCreationDate())
                .completionDate(m.getCompletionDate());
    }
    
}