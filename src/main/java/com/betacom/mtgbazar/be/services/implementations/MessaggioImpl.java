package com.betacom.mtgbazar.be.services.implementations;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.betacom.mtgbazar.be.model.MessageID;
import com.betacom.mtgbazar.be.model.Messaggi;
import com.betacom.mtgbazar.be.repositories.IMessaggiRepository;
import com.betacom.mtgbazar.be.services.IMessaggioServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class MessaggioImpl implements IMessaggioServices{

	private final IMessaggiRepository msgR;
	
	@Value("${lang}")
	private String lang;
	
	@Override
	public String get(String code) {
		log.debug("get {}", code);
		String r = null;
		Optional<Messaggi> m = msgR.findById(new MessageID(lang, code));
		if (m.isEmpty())
			r = code;
		else
			r = m.get().getMessaggio();
		
		
		return r;
	}

}