package com.betacom.mtgbazar.be.controllers.products;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.mtgbazar.be.dto.DashboardDTO;
import com.betacom.mtgbazar.be.dto.products.MagazzinoSKUDTO;
import com.betacom.mtgbazar.be.services.interfaces.IDashboardServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dashboard admin: i contatori e la lista di dettaglio "sotto scorta".
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final IDashboardServices dashboardS;

    @GetMapping
    public DashboardDTO stats() {
        log.debug("GET /api/admin/dashboard");
        return dashboardS.getStats();
    }

    /**
     * GET /api/admin/dashboard/sotto-scorta — gli SKU da rifornire, dal
     * piu' urgente (giacenza piu' bassa). Col nome del prodotto e la
     * variante, per sapere COSA riordinare senza aprire il magazzino.
     */
    @GetMapping("/sotto-scorta")
    public List<MagazzinoSKUDTO> sottoScorta() {
        log.debug("GET /api/admin/dashboard/sotto-scorta");
        return dashboardS.sottoScorta();
    }
    
}