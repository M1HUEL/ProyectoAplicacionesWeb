package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.DashboardResumenDTO;
import com.itson.ProyectoAplicacionesWeb.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;


@Controller
public class PanelAdministrativoController {
    private final DashboardService dashboardService;

    public PanelAdministrativoController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Muestra el resumen del negocio.
     *
     * El periodo es opcional; por defecto abarca desde el primer día
     * del mes actual hasta hoy.
     */
    @GetMapping("/admin")
    public String inicioAdmin(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        DashboardResumenDTO resumen;

        try {
            resumen = dashboardService.obtenerResumen(desde, hasta);
        } catch (IllegalArgumentException e) {
            // Periodo inválido: se informa y se muestra el periodo por defecto.
            model.addAttribute("error", e.getMessage());
            resumen = dashboardService.obtenerResumen(null, null);
        }

        model.addAttribute("resumen", resumen);

        return "panel-administrativo";
    }
}
