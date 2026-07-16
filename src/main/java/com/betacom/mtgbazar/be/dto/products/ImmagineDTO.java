package com.betacom.mtgbazar.be.dto.products;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter 
@AllArgsConstructor
@NoArgsConstructor
public class ImmagineDTO {
    private String url;    // percorso relativo: /immagini/prodotti/<uuid>.<ext>
}