package com.betacom.mtgbazar.be.services.interfaces.products;

import org.springframework.web.multipart.MultipartFile;

import com.betacom.mtgbazar.be.dto.products.ImmagineDTO;

public interface IImmagineServices {
	ImmagineDTO salvaImmagine(MultipartFile file, String sottocartella);
	
	void eliminaImmagine(String percorsoRelativo);
}
