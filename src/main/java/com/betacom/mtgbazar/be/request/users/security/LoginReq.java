package com.betacom.mtgbazar.be.request.users.security;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoginReq {

    //@NotBlank(message = "utente.no.email")
    //private String email;
	
	/** Email o username: la '@' decide il percorso di lookup. */
    @NotBlank(message = "utente.no.identificativo")
    private String identificativo;

    @NotBlank(message = "utente.no.pwd")
    @ToString.Exclude
    private String password;
    
}