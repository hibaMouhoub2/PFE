package com.example.projetpfe.controller;

import ch.qos.logback.core.model.Model;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.ReportService;
import com.example.projetpfe.util.ExcelExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    public Map<String, Object> getReportData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Si aucune date n'est fournie, utiliser des dates par défaut (tout)
        if (startDate == null && endDate == null) {
            // Utiliser la méthode existante sans filtrage par date
            Map<String, Object> data = new HashMap<>();
            data.put("raisonsNonRenouvellement", reportService.getRaisonsNonRenouvellementStats());
            data.put("qualiteService", reportService.getQualiteServiceStats());
            data.put("interetCredit", reportService.getInteretCreditStats());
            data.put("facteurInfluence", reportService.getFacteurInfluenceStats());
            data.put("profil", reportService.getProfilStats());
            data.put("activiteClient", reportService.getActiviteClientStats());
            data.put("rendezVous", reportService.getRendezVousStats());
            data.put("branche", reportService.getBrancheStats());
            data.put("progression", reportService.getStatusProgressionByMonth());
            return data;
        }

        // Initialiser les dates si une seule est fournie
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(6); // Par défaut, 6 mois en arrière
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Convertir en LocalDateTime pour avoir le début et la fin des jours
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // Créer de nouvelles méthodes dans le ReportService pour filtrer par date
        Map<String, Object> data = new HashMap<>();

        // Utiliser le service avec filtre de date
        data.put("raisonsNonRenouvellement", reportService.getRaisonsNonRenouvellementStats(start, end));
        data.put("qualiteService", reportService.getQualiteServiceStats(start, end));
        data.put("interetCredit", reportService.getInteretCreditStats(start, end));
        data.put("facteurInfluence", reportService.getFacteurInfluenceStats(start, end));
        data.put("profil", reportService.getProfilStats(start, end));
        data.put("activiteClient", reportService.getActiviteClientStats(start, end));
        data.put("rendezVous", reportService.getRendezVousStats(start, end));
        data.put("branche", reportService.getBrancheStats(start, end));
        data.put("progression", reportService.getStatusProgressionByMonth(start, end));

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
        String filename = "export_clients_avec_raison" +
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
        String filename = "export_clients_qualité" + qualite.name().toLowerCase() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @GetMapping("/api/export/facteur/{facteurValue}")
    public ResponseEntity<byte[]> exportClientsByFacteur(@PathVariable String facteurValue) throws IOException {
        System.out.println("Valeur reçue: " + facteurValue);

        FacteurInfluence influence = null;
        try {
            influence = FacteurInfluence.valueOf(facteurValue);
        } catch (IllegalArgumentException e) {
            // Correspondance plus intelligente
            for (FacteurInfluence f : FacteurInfluence.values()) {
                // 1. Vérifier les noms simplifiés
                String simplifiedEnum = f.name().replaceAll("[^A-Z]", "");
                String simplifiedInput = facteurValue.replaceAll("[^A-Z]", "");

                // 2. Vérifier si les chaînes simplifiées correspondent partiellement
                if (simplifiedEnum.contains(simplifiedInput) || simplifiedInput.contains(simplifiedEnum)) {
                    influence = f;
                    System.out.println("Correspondance trouvée: " + f.name());
                    break;
                }

                // 3. Vérifier avec le displayName aussi
                String displayName = f.getDisplayName().toUpperCase()
                        .replaceAll("[^A-Z]", "");
                if (displayName.contains(simplifiedInput) || simplifiedInput.contains(displayName)) {
                    influence = f;
                    System.out.println("Correspondance trouvée via displayName: " + f.name());
                    break;
                }
            }

            if (influence == null) {
                System.err.println("Impossible de trouver FacteurInfluence pour: " + facteurValue);
                return ResponseEntity.badRequest().build();
            }
        }

        List<Client> clients = clientRepository.findByFacteurInfluence(influence);
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "export_clients_facteur_" + influence.name().toLowerCase() + "_" +
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
        String filename = "export_clients_interet_" + interetValue.toLowerCase() + "_" +
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
        String filename = "export_clients_profil_" + profilValue.toLowerCase() + "_" +
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
        String filename = "export_clients_activite_" + activiteValue.toLowerCase() + "_" +
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
        String filename = "export_clients_branche_" + brancheValue.toLowerCase() + "_" +
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
        String filename = "export_clients_" + rendezVousText + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @GetMapping("/api/data/agent-performance")
    @ResponseBody
    public Map<String, Object> getAgentPerformanceData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Définir des dates par défaut si non fournies
        LocalDateTime start = startDate != null ?
                startDate.atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ?
                endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        Map<String, Object> data = new HashMap<>();
        data.put("agentPerformance", reportService.getAgentPerformanceStats(start, end));
        data.put("dailyActivity", reportService.getDailyAgentActivityStats(start, end));

        return data;
    }
}