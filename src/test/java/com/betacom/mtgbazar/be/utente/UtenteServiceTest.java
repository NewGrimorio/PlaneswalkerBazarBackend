package com.betacom.mtgbazar.be.utente;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
 
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.betacom.mtgbazar.be.dto.users.UtenteDTO;
import com.betacom.mtgbazar.be.exceptions.MtgException;
import com.betacom.mtgbazar.be.model.users.Portafoglio;
import com.betacom.mtgbazar.be.repositories.users.IPortafoglioRepository;
import com.betacom.mtgbazar.be.repositories.users.IUtenteRepository;
import com.betacom.mtgbazar.be.request.users.UtenteReq;
import com.betacom.mtgbazar.be.request.users.security.CambioEmailReq;
import com.betacom.mtgbazar.be.request.users.security.CambioPasswordReq;
import com.betacom.mtgbazar.be.request.users.security.LoginReq;
import com.betacom.mtgbazar.be.services.interfaces.users.IUtenteServices;
 
import lombok.extern.slf4j.Slf4j;
 
/**
 * Test di UtenteImpl su H2 (Flyway V1+V2 dal contesto).
 * Ogni test crea i PROPRI utenti con email e CF univoci (prefisso ust):
 * indipendenti dall'ordine e dagli altri test della suite (pwt...).
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc   //Messa nei service perche' i primi controller la implementano
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UtenteServiceTest {
 
    @Autowired private IUtenteServices utenteS;
    @Autowired private IUtenteRepository utenteR;
    @Autowired private IPortafoglioRepository portafoglioR;
 
    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String PASSWORD = "passwordSicura1";
 
    /** Request di registrazione valida con email e CF univoci. */
    private UtenteReq buildReq() {
        int n = SEQ.incrementAndGet();
        UtenteReq req = new UtenteReq();
        req.setEmail("ust" + n + "@test.it");
        req.setPassword(PASSWORD);
        req.setNome("Mario");
        req.setCognome("Rossi");
        req.setTelefono("+39 3331234567");
        req.setDataNascita(LocalDate.of(1990, 5, 20));
        req.setCodiceFiscale(String.format("TSTCF%011d", n));   // 16 char univoci
        return req;
    }
 
    // ------------------------------------------------------------------
    // REGISTRAZIONE
    // ------------------------------------------------------------------
 
    @Test
    @Order(1)
    public void registraCreaUtenteEPortafoglioENormalizzaEmail() {
        log.debug("TEST 1: registrazione con email 'sporca', attesi utente+portafoglio");
 
        UtenteReq req = buildReq();
        String emailPulita = req.getEmail();
        req.setEmail("  " + emailPulita.toUpperCase() + "  ");   // spazi e maiuscole
 
        UtenteDTO dto = utenteS.registraUtente(req);
        log.debug("registrato: id={} email={}", dto.getId(), dto.getEmail());
 
        assertNotNull(dto.getId());
        assertEquals(emailPulita, dto.getEmail());               // normalizzata
        assertEquals("CLIENTE", dto.getRuolo());
 
        // password MAI in chiaro sul DB
        String hash = utenteR.findById(dto.getId()).orElseThrow().getPasswordHash();
        assertNotEquals(PASSWORD, hash);
        assertTrue(hash.startsWith("$2"));                       // formato BCrypt
 
        // portafoglio nato nella stessa transazione, a saldo zero
        Portafoglio p = portafoglioR.findByUtenteId(dto.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(p.getSaldo()));
    }
 
    @Test
    @Order(2)
    public void registraConEmailDuplicataRifiutata() {
        log.debug("TEST 2: doppia registrazione con la stessa email");
 
        UtenteReq req = buildReq();
        utenteS.registraUtente(req);
 
        UtenteReq doppione = buildReq();
        doppione.setEmail("  " + req.getEmail().toUpperCase() + " ");  // stessa, sporca
 
        MtgException ex = assertThrows(MtgException.class,
                () -> utenteS.registraUtente(doppione));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Email gia' registrata", ex.getMessage());
    }
 
    @Test
    @Order(3)
    public void registraConCodiceFiscaleDuplicatoRifiutata() {
        log.debug("TEST 3: doppia registrazione con lo stesso CF");
 
        UtenteReq req = buildReq();
        utenteS.registraUtente(req);
 
        UtenteReq doppione = buildReq();
        doppione.setCodiceFiscale(req.getCodiceFiscale().toLowerCase()); // normalizzato
 
        MtgException ex = assertThrows(MtgException.class,
                () -> utenteS.registraUtente(doppione));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Codice fiscale gia' registrato", ex.getMessage());
    }
 
    @Test
    @Order(4)
    public void registraMinorenneRifiutata() {
        log.debug("TEST 4: registrazione di un minorenne");
 
        UtenteReq req = buildReq();
        req.setDataNascita(LocalDate.now().minusYears(16));
 
        MtgException ex = assertThrows(MtgException.class,
                () -> utenteS.registraUtente(req));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Data di nascita non valida", ex.getMessage());
    }
 
    // ------------------------------------------------------------------
    // LOGIN
    // ------------------------------------------------------------------
 
    @Test
    @Order(5)
    public void loginConCredenzialiCorretteEEmailSporca() {
        log.debug("TEST 5: login con email non normalizzata");
 
        UtenteReq req = buildReq();
        UtenteDTO registrato = utenteS.registraUtente(req);
 
        LoginReq login = new LoginReq();
        login.setEmail(" " + req.getEmail().toUpperCase());
        login.setPassword(PASSWORD);
 
        UtenteDTO dto = utenteS.loginUtente(login);
        log.debug("login ok: id={}", dto.getId());
        assertEquals(registrato.getId(), dto.getId());
    }
 
    @Test
    @Order(6)
    public void loginConPasswordErrataOEmailInesistenteStessoMessaggio() {
        log.debug("TEST 6: password errata ed email inesistente devono dare LO STESSO errore");
 
        UtenteReq req = buildReq();
        utenteS.registraUtente(req);
 
        LoginReq pwdErrata = new LoginReq();
        pwdErrata.setEmail(req.getEmail());
        pwdErrata.setPassword("passwordSbagliata");
        MtgException ex1 = assertThrows(MtgException.class, () -> utenteS.loginUtente(pwdErrata));
 
        LoginReq emailInesistente = new LoginReq();
        emailInesistente.setEmail("nessuno" + SEQ.get() + "@test.it");
        emailInesistente.setPassword(PASSWORD);
        MtgException ex2 = assertThrows(MtgException.class, () -> utenteS.loginUtente(emailInesistente));
 
        log.debug("messaggi: '{}' / '{}'", ex1.getMessage(), ex2.getMessage());
        assertEquals("Email o password errati", ex1.getMessage());
        assertEquals(ex1.getMessage(), ex2.getMessage());   // anti user-enumeration
    }
 
    // ------------------------------------------------------------------
    // CAMBIO PASSWORD / EMAIL
    // ------------------------------------------------------------------
 
    @Test
    @Order(7)
    public void changePasswordRichiedeLaVecchiaELaNuovaFunziona() {
        log.debug("TEST 7: cambio password completo");
 
        UtenteReq req = buildReq();
        UtenteDTO dto = utenteS.registraUtente(req);
 
        // vecchia password errata -> rifiuto
        CambioPasswordReq errata = new CambioPasswordReq();
        errata.setUtenteId(dto.getId());
        errata.setVecchiaPassword("nonSonoIo123");
        errata.setNuovaPassword("nuovaPassword1");
        MtgException ex = assertThrows(MtgException.class, () -> utenteS.changePassword(errata));
        assertEquals("Email o password errati", ex.getMessage());
 
        // vecchia corretta -> cambio ok
        CambioPasswordReq ok = new CambioPasswordReq();
        ok.setUtenteId(dto.getId());
        ok.setVecchiaPassword(PASSWORD);
        ok.setNuovaPassword("nuovaPassword1");
        utenteS.changePassword(ok);
        log.debug("password cambiata, verifico i login");
 
        // la nuova entra, la vecchia no
        LoginReq conNuova = new LoginReq();
        conNuova.setEmail(req.getEmail());
        conNuova.setPassword("nuovaPassword1");
        assertEquals(dto.getId(), utenteS.loginUtente(conNuova).getId());
 
        LoginReq conVecchia = new LoginReq();
        conVecchia.setEmail(req.getEmail());
        conVecchia.setPassword(PASSWORD);
        assertThrows(MtgException.class, () -> utenteS.loginUtente(conVecchia));
    }
 
    @Test
    @Order(8)
    public void changeEmailRichiedePasswordEControllaDuplicati() {
        log.debug("TEST 8: cambio email completo");
 
        UtenteReq reqA = buildReq();
        UtenteDTO a = utenteS.registraUtente(reqA);
        UtenteReq reqB = buildReq();
        utenteS.registraUtente(reqB);
 
        // verso un'email gia' di B -> rifiuto
        CambioEmailReq duplicata = new CambioEmailReq();
        duplicata.setUtenteId(a.getId());
        duplicata.setNuovaEmail(reqB.getEmail());
        duplicata.setPassword(PASSWORD);
        MtgException ex = assertThrows(MtgException.class, () -> utenteS.changeEmail(duplicata));
        assertEquals("Email gia' registrata", ex.getMessage());
 
        // verso una libera, con password giusta -> ok e login con la nuova
        String nuova = "nuova" + SEQ.incrementAndGet() + "@test.it";
        CambioEmailReq ok = new CambioEmailReq();
        ok.setUtenteId(a.getId());
        ok.setNuovaEmail(nuova.toUpperCase());
        ok.setPassword(PASSWORD);
        UtenteDTO dopo = utenteS.changeEmail(ok);
        log.debug("email cambiata in {}", dopo.getEmail());
        assertEquals(nuova, dopo.getEmail());                   // normalizzata
 
        LoginReq login = new LoginReq();
        login.setEmail(nuova);
        login.setPassword(PASSWORD);
        assertEquals(a.getId(), utenteS.loginUtente(login).getId());
    }
 
    // ------------------------------------------------------------------
    // PROFILO / LOOKUP
    // ------------------------------------------------------------------
 
    @Test
    @Order(9)
    public void updateProfiloAggiornaAnagraficaMaIgnoraEmail() {
        log.debug("TEST 9: update profilo, l'email NON deve cambiare da qui");
 
        UtenteReq req = buildReq();
        UtenteDTO dto = utenteS.registraUtente(req);
 
        UtenteReq update = new UtenteReq();
        update.setId(dto.getId());
        update.setNome("Marco");
        update.setTelefono("+39 3459876543");
        update.setEmail("hacker" + SEQ.get() + "@test.it");     // deve essere ignorata
 
        UtenteDTO dopo = utenteS.updateProfilo(update);
        log.debug("dopo update: nome={} telefono={} email={}",
                dopo.getNome(), dopo.getTelefono(), dopo.getEmail());
 
        assertEquals("Marco", dopo.getNome());
        assertEquals("Rossi", dopo.getCognome());               // non toccato: resta
        assertEquals("+39 3459876543", dopo.getTelefono());
        assertEquals(req.getEmail(), dopo.getEmail());          // email invariata
    }
 
    @Test
    @Order(10)
    public void getByIdInesistenteRifiutato() {
        log.debug("TEST 10: getById su id inesistente");
 
        MtgException ex = assertThrows(MtgException.class, () -> utenteS.getById(999999L));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Utente non trovato", ex.getMessage());
    }
}