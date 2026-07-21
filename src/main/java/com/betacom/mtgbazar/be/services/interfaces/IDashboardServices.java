package com.betacom.mtgbazar.be.services.interfaces;

import java.util.List;

import com.betacom.mtgbazar.be.dto.DashboardDTO;
import com.betacom.mtgbazar.be.dto.products.MagazzinoSKUDTO;

/** Aggregazione dei contatori della dashboard admin (solo COUNT). */
public interface IDashboardServices {

    /** I quattro numeri della dashboard in un colpo solo. */
    DashboardDTO getStats();
    
    /** Gli SKU da rifornire (giacenza <= soglia), col nome del prodotto. */
    List<MagazzinoSKUDTO> sottoScorta();
}