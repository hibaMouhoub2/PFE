package com.example.projetpfe.controller;


import ch.qos.logback.core.model.Model;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.ReportService;
import com.example.projetpfe.util.ExcelExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {
    @Autowired
    private ExcelExportUtil excelExportUtil;

    private final ReportService reportService;
    @Autowired
    private ClientRepository clientRepository;

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
    @GetMapping("/api/export/raison/{raisonValue}")
    public ResponseEntity<byte[]> exportClientsByRaison(
            @PathVariable String raisonValue) throws IOException {

        // Convertir la chaîne en enum
        RaisonNonRenouvellement raison;
        try {
            raison = RaisonNonRenouvellement.valueOf(raisonValue);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        List<Client> clients = clientRepository.findByRaisonNonRenouvellement(raison);

        // Générer le fichier Excel
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);

        // Préparer la réponse HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        // Nom du fichier avec date
        String filename = "clients_raison_" +
                raison.name().toLowerCase() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                ".xlsx";

        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @GetMapping("/api/export/qualite/{qualiteValue}")
    public ResponseEntity<byte[]> exportClientsByQualite(
            @PathVariable String qualiteValue) throws IOException {

        QualiteService qualite;
        try {
            qualite = QualiteService.valueOf(qualiteValue);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        List<Client> clients = clientRepository.findByQualiteService(qualite);
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "clients_qualite_" + qualite.name().toLowerCase() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @GetMapping("/api/export/facteur/{facteurValue}")
    public ResponseEntity<byte[]> exportClientsByFacteur(@PathVariable String facteurValue) throws IOException {
        FacteurInfluence influence;
        try{
            influence = FacteurInfluence.valueOf(facteurValue);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().build();
        }
        List<Client> clients = clientRepository.findByFacteurInfluence(influence);
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "clients_facteur_" + facteurValue.toLowerCase() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @GetMapping("/api/export/interet/{interetValue}")
    public ResponseEntity<byte[]> exportClientsByInteret(@PathVariable String interetValue) throws IOException {
        InteretCredit interet ;
        try {
            interet = InteretCredit.valueOf(interetValue);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().build();
        }
        List<Client> clients = clientRepository.findByInteretNouveauCredit(interet);
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "clients_facteur_" + interetValue.toLowerCase() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }
    @GetMapping("/api/export/profil/{profilValue}")
    public ResponseEntity<byte[]> exportClientsByProfil(@PathVariable String profilValue) throws IOException {
        Profil profil;
        try{
            profil = Profil.valueOf(profilValue);
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().build();
        }
        List<Client> clients = clientRepository.findByProfil(profil);
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "clients_facteur_" + profilValue.toLowerCase() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @GetMapping("/api/export/activite/{activiteValue}")
    public ResponseEntity<byte[]> exportClientsByActivite(@PathVariable String activiteValue) throws IOException {
        ActiviteClient activite;
        try{
            activite = ActiviteClient.valueOf(activiteValue);
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().build();
        }
        List<Client> clients = clientRepository.findByActiviteClient(activite);
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "clients_facteur_" + activiteValue.toLowerCase() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @GetMapping("/api/export/branche/{brancheValue}")
    public ResponseEntity<byte[]> exportClientsByBranche(@PathVariable String brancheValue) throws IOException {
        Branche branche;
        try{
            branche = Branche.valueOf(brancheValue);
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().build();
        }
        List<Client> clients = clientRepository.findByNMBRA(branche);
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "clients_facteur_" + brancheValue.toLowerCase() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @GetMapping("/api/export/rendez-vous/{value}")
    public ResponseEntity<byte[]> exportClientsByRendezVous(@PathVariable String value) throws IOException {
        // Convertir la chaîne en booléen
        boolean hasRendezVous;
        if (value.equalsIgnoreCase("OUI")) {
            hasRendezVous = true;
        } else if (value.equalsIgnoreCase("NON")) {
            hasRendezVous = false;
        } else {
            return ResponseEntity.badRequest().build();
        }

        // Récupérer les clients avec ou sans rendez-vous
        List<Client> clients = clientRepository.findByRendezVousAgence(hasRendezVous);

        // Générer le fichier Excel
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);

        // Préparer la réponse HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        // Nom du fichier avec date
        String rendezVousText = hasRendezVous ? "avec_rdv" : "sans_rdv";
        String filename = "clients_" + rendezVousText + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }




}

