package com.betacom.mtgbazar.be.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class MessageID implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Column (length=4)
	private String lang;
	
	@Column (length = 50)
	private String code;
}
