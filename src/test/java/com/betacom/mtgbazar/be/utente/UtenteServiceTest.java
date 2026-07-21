package com.betacom.mtgbazar.be.utente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;

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

import com.betacom.mtgbazar.be.security.interfaces.IRefreshTokenServices;



import lombok.extern.slf4j.Slf4j;

/**
 * Test di UtenteImpl su H2 (Flyway V1..V9 dal contesto).
 * Ogni test crea i PROPRI utenti con email, username e CF univoci
 * (prefisso ust): indipendenti dall'ordine e dagli altri test della suite.
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc   //Messa nei service perche' i primi controller la implementano
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UtenteServiceTest {

    @Autowired private IUtenteServices utenteS;
    @Autowired private IUtenteRepository utenteR;
    @Autowired private IPortafoglioRepository portafoglioR;
    @Autowired private IRefreshTokenServices refreshS;
    

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String PASSWORD = "passwordSicura1";

    /** Request di registrazione valida con email, username e CF univoci. */
    private UtenteReq buildReq() {
        int n = SEQ.incrementAndGet();
        UtenteReq req = new UtenteReq();
        req.setEmail("ust" + n + "@test.it");
        req.setUsername("ust" + n);
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
    public void registraCreaUtenteEPortafoglioENormalizzaIdentita() {
        log.debug("TEST 1: registrazione con email e username 'sporchi', attesi utente+portafoglio");

        UtenteReq req = buildReq();
        String emailPulita = req.getEmail();
        String usernamePulito = req.getUsername();
        req.setEmail("  " + emailPulita.toUpperCase() + "  ");        // spazi e maiuscole
        req.setUsername(" " + usernamePulito.toUpperCase() + " ");    // idem

        UtenteDTO dto = utenteS.registraUtente(req);
        log.debug("registrato: id={} email={} username={}",
                dto.getId(), dto.getEmail(), dto.getUsername());

        assertNotNull(dto.getId());
        assertEquals(emailPulita, dto.getEmail());                    // normalizzata
        assertEquals(usernamePulito, dto.getUsername());              // normalizzato
        assertEquals("CLIENTE", dto.getRuolo());

        // password MAI in chiaro sul DB
        String hash = utenteR.findById(dto.getId()).orElseThrow().getPasswordHash();
        assertNotEquals(PASSWORD, hash);
        assertTrue(hash.startsWith("$2"));                            // formato BCrypt

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
        login.setIdentificativo(" " + req.getEmail().toUpperCase());
        login.setPassword(PASSWORD);

        UtenteDTO dto = utenteS.loginUtente(login);
        log.debug("login ok: id={}", dto.getId());
        assertEquals(registrato.getId(), dto.getId());
    }

    @Test
    @Order(6)
    public void loginConPasswordErrataOIdentificativoInesistenteStessoMessaggio() {
        log.debug("TEST 6: password errata e identificativo inesistente devono dare LO STESSO errore");

        UtenteReq req = buildReq();
        utenteS.registraUtente(req);

        LoginReq pwdErrata = new LoginReq();
        pwdErrata.setIdentificativo(req.getEmail());
        pwdErrata.setPassword("passwordSbagliata");
        MtgException ex1 = assertThrows(MtgException.class, () -> utenteS.loginUtente(pwdErrata));

        LoginReq emailInesistente = new LoginReq();
        emailInesistente.setIdentificativo("nessuno" + SEQ.get() + "@test.it");
        emailInesistente.setPassword(PASSWORD);
        MtgException ex2 = assertThrows(MtgException.class, () -> utenteS.loginUtente(emailInesistente));

        log.debug("messaggi: '{}' / '{}'", ex1.getMessage(), ex2.getMessage());
        assertEquals("Credenziali errate", ex1.getMessage());
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
        assertEquals("Credenziali errate", ex.getMessage());

        // vecchia corretta -> cambio ok
        CambioPasswordReq ok = new CambioPasswordReq();
        ok.setUtenteId(dto.getId());
        ok.setVecchiaPassword(PASSWORD);
        ok.setNuovaPassword("nuovaPassword1");
        utenteS.changePassword(ok);
        log.debug("password cambiata, verifico i login");

        // la nuova entra, la vecchia no
        LoginReq conNuova = new LoginReq();
        conNuova.setIdentificativo(req.getEmail());
        conNuova.setPassword("nuovaPassword1");
        assertEquals(dto.getId(), utenteS.loginUtente(conNuova).getId());

        LoginReq conVecchia = new LoginReq();
        conVecchia.setIdentificativo(req.getEmail());
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
        login.setIdentificativo(nuova);
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
        log.debug("dopo update: nome={} telefono={} email={} username={}",
                dopo.getNome(), dopo.getTelefono(), dopo.getEmail(), dopo.getUsername());

        assertEquals("Marco", dopo.getNome());
        assertEquals("Rossi", dopo.getCognome());               // non toccato: resta
        assertEquals("+39 3459876543", dopo.getTelefono());
        assertEquals(req.getEmail(), dopo.getEmail());          // email invariata
        assertEquals(req.getUsername(), dopo.getUsername());    // non inviato: resta
    }

    @Test
    @Order(10)
    public void getByIdInesistenteRifiutato() {
        log.debug("TEST 10: getById su id inesistente");

        MtgException ex = assertThrows(MtgException.class, () -> utenteS.getById(999999L));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Utente non trovato", ex.getMessage());
    }

    // ------------------------------------------------------------------
    // USERNAME
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    public void registraConUsernameDuplicatoAncheConCaseDiversoRifiutata() {
        log.debug("TEST 11: doppia registrazione con lo stesso username (case diverso)");

        UtenteReq req = buildReq();
        utenteS.registraUtente(req);

        UtenteReq doppione = buildReq();                          // email e CF univoci
        doppione.setUsername(" " + req.getUsername().toUpperCase() + " ");  // stesso, sporco

        MtgException ex = assertThrows(MtgException.class,
                () -> utenteS.registraUtente(doppione));
        log.debug("eccezione attesa: {}", ex.getMessage());
        assertEquals("Username gia' in uso", ex.getMessage());
    }

    @Test
    @Order(12)
    public void updateProfiloCambiaUsernameEControllaDuplicati() {
        log.debug("TEST 12: cambio username da profilo, con controllo duplicati");

        UtenteReq reqA = buildReq();
        UtenteDTO a = utenteS.registraUtente(reqA);
        UtenteReq reqB = buildReq();
        utenteS.registraUtente(reqB);

        // verso lo username di B (case diverso) -> rifiuto
        UtenteReq duplicato = new UtenteReq();
        duplicato.setId(a.getId());
        duplicato.setUsername(reqB.getUsername().toUpperCase());
        MtgException ex = assertThrows(MtgException.class,
                () -> utenteS.updateProfilo(duplicato));
        assertEquals("Username gia' in uso", ex.getMessage());

        // rimandare il PROPRIO username (anche sporco) non e' un duplicato
        UtenteReq stesso = new UtenteReq();
        stesso.setId(a.getId());
        stesso.setUsername(" " + reqA.getUsername().toUpperCase());
        assertEquals(reqA.getUsername(), utenteS.updateProfilo(stesso).getUsername());

        // verso uno libero -> ok, normalizzato
        String nuovo = "nuovo.ust" + SEQ.incrementAndGet();
        UtenteReq ok = new UtenteReq();
        ok.setId(a.getId());
        ok.setUsername(nuovo.toUpperCase());
        UtenteDTO dopo = utenteS.updateProfilo(ok);
        log.debug("username cambiato in {}", dopo.getUsername());
        assertEquals(nuovo, dopo.getUsername());
    }

    @Test
    @Order(13)
    public void loginConUsernameAncheSporcoFunziona() {
        log.debug("TEST 13: login con USERNAME maiuscolo e spazi -> stesso utente");

        UtenteReq req = buildReq();
        UtenteDTO registrato = utenteS.registraUtente(req);

        LoginReq login = new LoginReq();
        login.setIdentificativo("  " + req.getUsername().toUpperCase() + " ");
        login.setPassword(PASSWORD);

        assertEquals(registrato.getId(), utenteS.loginUtente(login).getId());
    }
    
    @Test
    @Order(14)
    public void immagineProfiloUploadSostituzioneERimozione() throws Exception {
        log.debug("TEST 14: upload avatar, sostituzione (vecchio file eliminato), rimozione");

        UtenteDTO dto = utenteS.registraUtente(buildReq());
        assertNull(dto.getImmagineProfilo());   // nasce senza foto: default frontend

        // upload: campo valorizzato con percorso relativo
        MockMultipartFile foto1 = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});
        UtenteDTO conFoto = utenteS.updateImmagineProfilo(dto.getId(), foto1);
        assertNotNull(conFoto.getImmagineProfilo());
        assertTrue(conFoto.getImmagineProfilo().startsWith("/immagini/utenti/"));

        // il file esiste davvero su disco
        Path base = Path.of("./target/test-uploads").toAbsolutePath().normalize();
        Path file1 = base.resolve(conFoto.getImmagineProfilo().substring("/immagini/".length()));
        assertTrue(Files.exists(file1));

        // sostituzione: nuovo percorso, vecchio file eliminato
        MockMultipartFile foto2 = new MockMultipartFile(
                "file", "avatar2.png", "image/png", new byte[]{4, 5, 6});
        UtenteDTO conFoto2 = utenteS.updateImmagineProfilo(dto.getId(), foto2);
        assertNotEquals(conFoto.getImmagineProfilo(), conFoto2.getImmagineProfilo());
        assertTrue(Files.notExists(file1));     // pulizia avvenuta

        // tipo non ammesso -> errore pulito dei messaggi V5
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{7});
        MtgException ex = assertThrows(MtgException.class,
                () -> utenteS.updateImmagineProfilo(dto.getId(), pdf));
        assertEquals("Formato non supportato (ammessi: JPG, PNG, WebP)", ex.getMessage());

        // rimozione: campo null, file sparito
        UtenteDTO senzaFoto = utenteS.removeImmagineProfilo(dto.getId());
        assertNull(senzaFoto.getImmagineProfilo());
    }
    
    @Test
    @Order(15)
    public void cambioPasswordRevocaTutteLeSessioni() {
        log.debug("TEST 15: cambio password -> ogni refresh token esistente muore");
 
        UtenteReq req = buildReq();
        UtenteDTO dto = utenteS.registraUtente(req);
 
        // due "dispositivi": due famiglie di refresh vive
        String tokenPc = refreshS.emetti(dto.getId(), "pc-test");
        String tokenTelefono = refreshS.emetti(dto.getId(), "telefono-test");
 
        CambioPasswordReq cambio = new CambioPasswordReq();
        cambio.setUtenteId(dto.getId());
        cambio.setVecchiaPassword(PASSWORD);
        cambio.setNuovaPassword("nuovaPassword2");
        utenteS.changePassword(cambio);
 
        // entrambe le sessioni sono morte: la rotazione viene rifiutata
        org.junit.jupiter.api.Assertions.assertThrows(
                com.betacom.mtgbazar.be.exceptions.AuthTokenException.class,
                () -> refreshS.ruota(tokenPc, "pc-test"));
        org.junit.jupiter.api.Assertions.assertThrows(
                com.betacom.mtgbazar.be.exceptions.AuthTokenException.class,
                () -> refreshS.ruota(tokenTelefono, "telefono-test"));
    }
    
}