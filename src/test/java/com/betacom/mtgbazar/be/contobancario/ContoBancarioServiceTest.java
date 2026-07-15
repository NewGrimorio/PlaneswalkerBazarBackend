package com.betacom.mtgbazar.be.contobancario;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.betacom.mtgbazar.be.dto.users.ContoBancarioDTO;
import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.repositories.users.IContoBancarioRepository;
import com.betacom.mtgbazar.be.request.users.ContoBancarioReq;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IContoBancarioServices;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;

import lombok.extern.slf4j.Slf4j;
 
/**
 * Test di ContoBancarioImpl su H2 (Flyway V1+V2 dal contesto).
 * Utenti creati via utenteS.registraUtente (prefisso cbt...).
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc   //Messa nei service perche' i primi controller la implementano
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ContoBancarioServiceTest {
 
    @Autowired private IContoBancarioServices contoS;
    @Autowired private IUtenteServices utenteS;
    @Autowired private IContoBancarioRepository contoR;
 
    private static final AtomicInteger SEQ = new AtomicInteger();
 
    private UtenteDTO utente;
 
    @BeforeEach
    public void setUp() {
        int n = SEQ.incrementAndGet();
        UtenteReq req = new UtenteReq();
        req.setEmail("cbt" + n + "@test.it");
        req.setPassword("passwordSicura1");
        req.setNome("Carlo");
        req.setCognome("Bruni");
        req.setDataNascita(LocalDate.of(1980, 1, 15));
        utente = utenteS.registraUtente(req);
        log.debug("setUp: creato utente {} ({})", utente.getId(), utente.getEmail());
    }
 
    private ContoBancarioReq buildReq(String iban) {
        ContoBancarioReq req = new ContoBancarioReq();
        req.setUtenteId(utente.getId());
        req.setIntestatario("Carlo Bruni");
        req.setIban(iban);
        req.setBic("bpmoit22");                       // minuscolo: deve uscire maiuscolo
        return req;
    }
 
    // ------------------------------------------------------------------
    // CREAZIONE E NORMALIZZAZIONE
    // ------------------------------------------------------------------
 
    @Test
    @Order(1)
    public void createNormalizzaIbanEMascheraInUscita() {
        log.debug("TEST 1: IBAN incollato con spazi e minuscole dall'home banking");
 
        // NB: la Req valida il formato SENZA spazi; la normalizzazione del
        // service copre il caso in cui il dato arrivi gia' pulito ma minuscolo
        ContoBancarioDTO dto = contoS.createConto(buildReq("it60x0542811101000000123456"));
        log.debug("creato: id={} ibanMascherato={} bic={}",
                dto.getId(), dto.getIbanMascherato(), dto.getBic());
 
        // sul DB: maiuscolo e senza spazi
        String ibanSuDb = contoR.findById(dto.getId()).orElseThrow().getIban();
        assertEquals("IT60X0542811101000000123456", ibanSuDb);
        assertEquals("BPMOIT22", contoR.findById(dto.getId()).orElseThrow().getBic());
 
        // in uscita: MAI l'IBAN completo — primi 4 + ultimi 4
        assertEquals("IT60 **** 3456", dto.getIbanMascherato());
    }
 
    @Test
    @Order(2)
    public void listMostraSoloIContiAttiviConIbanMascherato() {
        log.debug("TEST 2: lista con due conti, poi rimozione di uno");
 
        ContoBancarioDTO primo = contoS.createConto(buildReq("IT60X0542811101000000123456"));
        contoS.createConto(buildReq("IT28W8000000292100645211288"));
 
        List<ContoBancarioDTO> lista = contoS.listConti(utente.getId());
        log.debug("lista: {}", lista.stream().map(ContoBancarioDTO::getIbanMascherato).toList());
        assertEquals(2, lista.size());
        assertTrue(lista.stream().allMatch(c -> c.getIbanMascherato().contains("****")));
 
        contoS.removeConto(primo.getId(), utente.getId());
 
        lista = contoS.listConti(utente.getId());
        assertEquals(1, lista.size());                        // soft delete: sparito
        assertEquals("IT28 **** 1288", lista.get(0).getIbanMascherato());
    }
 
    // ------------------------------------------------------------------
    // RIMOZIONE E OWNERSHIP
    // ------------------------------------------------------------------
 
    @Test
    @Order(3)
    public void rimozioneDoppiaRifiutata() {
        log.debug("TEST 3: il conto gia' rimosso non e' piu' raggiungibile");
 
        ContoBancarioDTO conto = contoS.createConto(buildReq("IT60X0542811101000000123456"));
        contoS.removeConto(conto.getId(), utente.getId());
 
        MtgException ex = assertThrows(MtgException.class,
                () -> contoS.removeConto(conto.getId(), utente.getId()));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Conto bancario non trovato", ex.getMessage());
    }
 
    @Test
    @Order(4)
    public void contoAltruiInvisibileEIntoccabile() {
        log.debug("TEST 4: l'utente B non vede e non rimuove i conti di A");
 
        ContoBancarioDTO diA = contoS.createConto(buildReq("IT60X0542811101000000123456"));
 
        int n = SEQ.incrementAndGet();
        UtenteReq reqB = new UtenteReq();
        reqB.setEmail("cbt" + n + "b@test.it");
        reqB.setPassword("passwordSicura1");
        reqB.setNome("Bruno");
        reqB.setCognome("Bianchi");
        UtenteDTO b = utenteS.registraUtente(reqB);
 
        // per B il conto di A "non esiste"
        MtgException ex = assertThrows(MtgException.class,
                () -> contoS.removeConto(diA.getId(), b.getId()));
        assertEquals("Conto bancario non trovato", ex.getMessage());
 
        // e la lista di B e' vuota
        assertEquals(0, contoS.listConti(b.getId()).size());
 
        // mentre il conto di A e' intatto
        List<ContoBancarioDTO> listaA = contoS.listConti(utente.getId());
        log.debug("lista di A dopo il tentativo di B: {}", listaA.size());
        assertEquals(1, listaA.size());
    }
    
}