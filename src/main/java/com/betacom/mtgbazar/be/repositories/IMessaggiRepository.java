package com.betacom.mtgbazar.be.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.betacom.mtgbazar.be.model.MessageID;
import com.betacom.mtgbazar.be.model.Messaggi;



@Repository
public interface IMessaggiRepository extends JpaRepository<Messaggi, MessageID> {
	
}