package com.betacom.mtgbazar.be;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import com.betacom.mtgbazar.be.carrello.CarrelloServiceTest;
import com.betacom.mtgbazar.be.catalogo.CatalogoServiceTest;
import com.betacom.mtgbazar.be.contobancario.ContoBancarioServiceTest;
import com.betacom.mtgbazar.be.controllers.AdminControllerRestTest;
import com.betacom.mtgbazar.be.controllers.ControllerRestTest;
import com.betacom.mtgbazar.be.indirizzo.IndirizzoServiceTest;
import com.betacom.mtgbazar.be.ordine.OrdineServiceTest;
import com.betacom.mtgbazar.be.portafoglio.PortafoglioServiceTest;
import com.betacom.mtgbazar.be.recensione.RecensioneServiceTest;
import com.betacom.mtgbazar.be.utente.UtenteServiceTest;

@Suite
@SelectClasses({
    PortafoglioServiceTest.class,
    UtenteServiceTest.class,
    IndirizzoServiceTest.class,
    ContoBancarioServiceTest.class,
    CarrelloServiceTest.class,
    OrdineServiceTest.class,
    RecensioneServiceTest.class,
    CatalogoServiceTest.class,
    ControllerRestTest.class,
    AdminControllerRestTest.class
    // qui si accoderanno UtenteServiceTest, CarrelloServiceTest, OrdineServiceTest...
})

public class PlaneswalkerBazarSuiteClass {

}
