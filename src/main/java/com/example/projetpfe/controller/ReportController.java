package com.example.projetpfe.controller;


import ch.qos.logback.core.model.Model;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/questionnaire")
    public String showQuestionnaireReport() {
        return "admin/reports/questionnaire";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public Map<String, Object> getReportData() {
        Map<String, Object> data = new HashMap<>();

        // Raisons de non-renouvellement
        data.put("raisonsNonRenouvellement", reportService.getRaisonsNonRenouvellementStats());

        // Qualité du service
        data.put("qualiteService", reportService.getQualiteServiceStats());

        // Intérêt pour un nouveau crédit
        data.put("interetCredit", reportService.getInteretCreditStats());

        // Facteur d'influence
        data.put("facteurInfluence", reportService.getFacteurInfluenceStats());

        // Profil client
        data.put("profil", reportService.getProfilStats());

        // Activité client
        data.put("activiteClient", reportService.getActiviteClientStats());

        // Rendez-vous
        data.put("rendezVous", reportService.getRendezVousStats());

        // Répartition par agence
        data.put("branche", reportService.getBrancheStats());

        // Progression mensuelle
        data.put("progression", reportService.getStatusProgressionByMonth());

        return data;
    }
}

