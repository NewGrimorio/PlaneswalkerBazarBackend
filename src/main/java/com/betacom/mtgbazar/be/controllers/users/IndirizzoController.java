package com.betacom.mtgbazar.be.controllers.users;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.users.IndirizzoDTO;
import com.betacom.mtgbazar.be.request.ValidationGroups;
import com.betacom.mtgbazar.be.request.users.IndirizzoReq;
import com.betacom.mtgbazar.be.security.SecurityUtils;
import com.betacom.mtgbazar.be.services.interfaces.users.IIndirizzoServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Rubrica indirizzi — FASE C: l'utenteId non arriva piu' dal client,
 * si sovrascrive dal token (pattern del tutor). L'ownership check nel
 * service resta identico: ora pero' lavora su un id FIDATO.
 */
@RestController
@RequestMapping("/api/indirizzi")
@RequiredArgsConstructor
@Slf4j
public class IndirizzoController {

   private final IIndirizzoServices indirizzoS;

   @GetMapping
   public List<IndirizzoDTO> list(Authentication authentication) {
       Long utenteId = SecurityUtils.utenteId(authentication);
       log.debug("GET /api/indirizzi utente={}", utenteId);
       return indirizzoS.listIndirizzi(utenteId);
   }

   @PostMapping
   public IndirizzoDTO create(
           @Validated(ValidationGroups.Create.class) @RequestBody IndirizzoReq req,
           Authentication authentication) {
       req.setUtenteId(SecurityUtils.utenteId(authentication));   // identita' dal token
       log.debug("POST /api/indirizzi utente={}", req.getUtenteId());
       return indirizzoS.createIndirizzo(req);
   }

   @PutMapping
   public IndirizzoDTO update(
           @Validated(ValidationGroups.Update.class) @RequestBody IndirizzoReq req,
           Authentication authentication) {
       req.setUtenteId(SecurityUtils.utenteId(authentication));
       log.debug("PUT /api/indirizzi id={}", req.getId());
       return indirizzoS.updateIndirizzo(req);
   }

   @DeleteMapping("/{id}")
   public void remove(@PathVariable Long id, Authentication authentication) {
       Long utenteId = SecurityUtils.utenteId(authentication);
       log.debug("DELETE /api/indirizzi/{} utente={}", id, utenteId);
       indirizzoS.removeIndirizzo(id, utenteId);
   }

   /** kebab-case, come da convenzione dei path del progetto. */
   @PostMapping("/{id}/set-predefinito")
   public void setPredefinito(@PathVariable Long id, Authentication authentication) {
       Long utenteId = SecurityUtils.utenteId(authentication);
       log.debug("POST /api/indirizzi/{}/set-predefinito utente={}", id, utenteId);
       indirizzoS.setPredefinito(id, utenteId);
   }

}
