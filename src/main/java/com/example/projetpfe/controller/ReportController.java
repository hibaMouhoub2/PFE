package com.example.projetpfe.controller;


import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.RegionService;
import com.example.projetpfe.service.Impl.ReportService;
import com.example.projetpfe.service.UserService;
import com.example.projetpfe.util.ExcelExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;



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
    private UserRepository userRepository;

    @Autowired
    private RegionService regionService;
    @Autowired
    private UserService userService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Dans la méthode showQuestionnaireReport de ReportController.java
    @GetMapping("/questionnaire")
    public String showQuestionnaireReport(Model model) {
        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);

        // Si c'est un super admin, récupérer toutes les régions
        if (isSuperAdmin) {
            List<Region> regions = regionService.findAll();
            model.addAttribute("regions", regions);
            model.addAttribute("isSuperAdmin", true);
        }

        return "admin/reports/questionnaire";
    }
    @GetMapping("/api/data")
    @ResponseBody
    public Map<String, Object> getReportData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String brancheCode) {

        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);

        // Obtenir la région si un ID est fourni et si l'utilisateur est super admin
        Region selectedRegion = null;
        if (isSuperAdmin && regionId != null) {
            selectedRegion = regionService.findById(regionId).orElse(null);
        }
        Branche selectedBranche = null;
        if (brancheCode != null && !brancheCode.isEmpty()) {
            try {
                selectedBranche = Branche.valueOf(brancheCode);
            } catch (IllegalArgumentException e) {
                // Ignorer si le code n'est pas valide
            }
        }

        // Utiliser le reste du code existant pour traiter les données
        Map<String, Object> data = new HashMap<>();

        // Si aucune date n'est fournie, utiliser des dates par défaut (tout)
        if (startDate == null && endDate == null) {
            if (selectedBranche != null) {
                data.put("raisonsNonRenouvellement", reportService.getRaisonsNonRenouvellementStatsByBranche(selectedBranche));
                data.put("qualiteService", reportService.getQualiteServiceStatsByBranche(selectedBranche));
                data.put("interetCredit", reportService.getInteretCreditStatsByBranche(selectedBranche));
                data.put("facteurInfluence", reportService.getFacteurInfluenceStatsByBranche(selectedBranche));
                data.put("profil", reportService.getProfilStatsByBranche(selectedBranche));
                data.put("activiteClient", reportService.getActiviteClientStatsByBranche(selectedBranche));
                data.put("rendezVous", reportService.getRendezVousStatsByBranche(selectedBranche));
                data.put("branche", reportService.getBrancheStats());
                data.put("progression", reportService.getStatusProgressionByMonthAndBranche(selectedBranche));
            }
            // Si une région est sélectionnée et que l'utilisateur est super admin
            else if (isSuperAdmin && selectedRegion != null) {
                data.put("raisonsNonRenouvellement", reportService.getRaisonsNonRenouvellementStatsByRegion(selectedRegion));
                data.put("qualiteService", reportService.getQualiteServiceStatsByRegion(selectedRegion));
                data.put("interetCredit", reportService.getInteretCreditStatsByRegion(selectedRegion));
                data.put("facteurInfluence", reportService.getFacteurInfluenceStatsByRegion(selectedRegion));
                data.put("profil", reportService.getProfilStatsByRegion(selectedRegion));
                data.put("activiteClient", reportService.getActiviteClientStatsByRegion(selectedRegion));
                data.put("rendezVous", reportService.getRendezVousStatsByRegion(selectedRegion));
                data.put("branche", reportService.getBrancheStatsByRegion(selectedRegion));
                data.put("progression", reportService.getStatusProgressionByMonthAndRegion(selectedRegion));
            } else {
                // Code existant pour obtenir toutes les données
                data.put("raisonsNonRenouvellement", reportService.getRaisonsNonRenouvellementStats());
                data.put("qualiteService", reportService.getQualiteServiceStats());
                data.put("interetCredit", reportService.getInteretCreditStats());
                data.put("facteurInfluence", reportService.getFacteurInfluenceStats());
                data.put("profil", reportService.getProfilStats());
                data.put("activiteClient", reportService.getActiviteClientStats());
                data.put("rendezVous", reportService.getRendezVousStats());
                data.put("branche", reportService.getBrancheStats());
                data.put("progression", reportService.getStatusProgressionByMonth());
            }
        } else {
            // Initialiser les dates si une seule est fournie
            LocalDateTime start = startDate != null ?
                    startDate.atStartOfDay() : LocalDateTime.now().minusMonths(1);
            LocalDateTime end = endDate != null ?
                    endDate.atTime(LocalTime.MAX) : LocalDateTime.now();
            if (selectedBranche != null) {
                data.put("raisonsNonRenouvellement", reportService.getRaisonsNonRenouvellementStatsByBrancheAndDate(selectedBranche, start, end));
                data.put("qualiteService", reportService.getQualiteServiceStatsByBrancheAndDate(selectedBranche, start, end));
                data.put("interetCredit", reportService.getInteretCreditStatsByBrancheAndDate(selectedBranche, start, end));
                data.put("facteurInfluence", reportService.getFacteurInfluenceStatsByBrancheAndDate(selectedBranche, start, end));
                data.put("profil", reportService.getProfilStatsByBrancheAndDate(selectedBranche, start, end));
                data.put("activiteClient", reportService.getActiviteClientStatsByBrancheAndDate(selectedBranche, start, end));
                data.put("rendezVous", reportService.getRendezVousStatsByBrancheAndDate(selectedBranche, start, end));
                data.put("branche", reportService.getBrancheStatsByDate(start, end));
                data.put("progression", reportService.getStatusProgressionByMonthAndBrancheAndDate(selectedBranche, start, end));
            }
            // Même logique pour le filtrage par région avec dates
            else if (isSuperAdmin && selectedRegion != null) {
                // Filtrer les données par région et dates
                data.put("raisonsNonRenouvellement", reportService.getRaisonsNonRenouvellementStatsByRegionAndDate(selectedRegion, start, end));
                data.put("qualiteService", reportService.getQualiteServiceStatsByRegionAndDate(selectedRegion, start, end));
                data.put("interetCredit", reportService.getInteretCreditStatsByRegionAndDate(selectedRegion, start, end));
                data.put("facteurInfluence", reportService.getFacteurInfluenceStatsByRegionAndDate(selectedRegion, start, end));
                data.put("profil", reportService.getProfilStatsByRegionAndDate(selectedRegion, start, end));
                data.put("activiteClient", reportService.getActiviteClientStatsByRegionAndDate(selectedRegion, start, end));
                data.put("rendezVous", reportService.getRendezVousStatsByRegionAndDate(selectedRegion, start, end));
                data.put("branche", reportService.getBrancheStatsByRegionAndDate(selectedRegion, start, end));
                data.put("progression", reportService.getStatusProgressionByMonthAndRegionAndDate(selectedRegion, start, end));
            } else {
                // Code existant pour le filtrage par dates
                data.put("raisonsNonRenouvellement", reportService.getRaisonsNonRenouvellementStats(start, end));
                data.put("qualiteService", reportService.getQualiteServiceStats(start, end));
                data.put("interetCredit", reportService.getInteretCreditStats(start, end));
                data.put("facteurInfluence", reportService.getFacteurInfluenceStats(start, end));
                data.put("profil", reportService.getProfilStats(start, end));
                data.put("activiteClient", reportService.getActiviteClientStats(start, end));
                data.put("rendezVous", reportService.getRendezVousStats(start, end));
                data.put("branche", reportService.getBrancheStats(start, end));
                data.put("progression", reportService.getStatusProgressionByMonth(start, end));
            }
        }

        return data;
    }
    // Filtre les clients par région avec correspondance souple
    private List<Client> filterClientsByRegionForStats(List<String> regionCodes) {
        List<Client> allClients = clientRepository.findAll();
        return allClients.stream()
                .filter(client -> {
                    if (client.getNMREG() == null) return false;
                    String normalizedClientRegion = client.getNMREG().toUpperCase().replace(" ", "");

                    return regionCodes.stream().anyMatch(regionCode -> {
                        String normalizedRegionCode = regionCode.toUpperCase().replace("_", "");
                        return normalizedClientRegion.contains(normalizedRegionCode) ||
                                normalizedRegionCode.contains(normalizedClientRegion) ||
                                (normalizedClientRegion.contains("SIDI") && normalizedRegionCode.contains("SEDY")) ||
                                (normalizedClientRegion.contains("SEDY") && normalizedRegionCode.contains("SIDI")) ||
                                (normalizedClientRegion.contains("FIDAA") && normalizedRegionCode.contains("FIDA")) ||
                                (normalizedClientRegion.contains("FIDA") && normalizedRegionCode.contains("FIDAA"));
                    });
                })
                .collect(Collectors.toList());
    }

    // Filtre les clients par région et date avec correspondance souple
    private List<Client> filterClientsByRegionAndDateForStats(List<String> regionCodes, LocalDateTime start, LocalDateTime end) {
        List<Client> allClients = clientRepository.findAll();
        return allClients.stream()
                .filter(client -> {
                    if (client.getNMREG() == null) return false;
                    if (client.getUpdatedAt() == null || client.getUpdatedAt().isBefore(start) || client.getUpdatedAt().isAfter(end)) return false;

                    String normalizedClientRegion = client.getNMREG().toUpperCase().replace(" ", "");

                    return regionCodes.stream().anyMatch(regionCode -> {
                        String normalizedRegionCode = regionCode.toUpperCase().replace("_", "");
                        return normalizedClientRegion.contains(normalizedRegionCode) ||
                                normalizedRegionCode.contains(normalizedClientRegion) ||
                                (normalizedClientRegion.contains("SIDI") && normalizedRegionCode.contains("SEDY")) ||
                                (normalizedClientRegion.contains("SEDY") && normalizedRegionCode.contains("SIDI")) ||
                                (normalizedClientRegion.contains("FIDAA") && normalizedRegionCode.contains("FIDA")) ||
                                (normalizedClientRegion.contains("FIDA") && normalizedRegionCode.contains("FIDAA"));
                    });
                })
                .collect(Collectors.toList());
    }

    // Calcule les statistiques pour les raisons de non renouvellement
    private Map<String, Long> calculateRaisonsStats(List<Client> clients) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (RaisonNonRenouvellement raison : RaisonNonRenouvellement.values()) {
            long count = clients.stream()
                    .filter(client -> raison.equals(client.getRaisonNonRenouvellement()))
                    .count();
            stats.put(raison.getDisplayName(), count);
        }
        return stats;
    }

    // Calcule les statistiques pour la qualité de service
    private Map<String, Long> calculateQualiteStats(List<Client> clients) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (QualiteService qualite : QualiteService.values()) {
            long count = clients.stream()
                    .filter(client -> qualite.equals(client.getQualiteService()))
                    .count();
            stats.put(qualite.getDisplayName(), count);
        }
        return stats;
    }

    // Calcule les statistiques pour l'intérêt au crédit
    private Map<String, Long> calculateInteretCreditStats(List<Client> clients) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (InteretCredit interet : InteretCredit.values()) {
            long count = clients.stream()
                    .filter(client -> interet.equals(client.getInteretNouveauCredit()))
                    .count();
            stats.put(interet.getDisplayName(), count);
        }
        return stats;
    }

    // Calcule les statistiques pour les facteurs d'influence
    private Map<String, Long> calculateFacteurInfluenceStats(List<Client> clients) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (FacteurInfluence facteur : FacteurInfluence.values()) {
            long count = clients.stream()
                    .filter(client -> facteur.equals(client.getFacteurInfluence()))
                    .count();
            stats.put(facteur.getDisplayName(), count);
        }
        return stats;
    }

    // Calcule les statistiques pour les profils
    private Map<String, Long> calculateProfilStats(List<Client> clients) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Profil profil : Profil.values()) {
            long count = clients.stream()
                    .filter(client -> profil.equals(client.getProfil()))
                    .count();
            stats.put(profil.getDisplayName(), count);
        }
        return stats;
    }

    // Calcule les statistiques pour les activités client
    private Map<String, Long> calculateActiviteClientStats(List<Client> clients) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (ActiviteClient activite : ActiviteClient.values()) {
            long count = clients.stream()
                    .filter(client -> activite.equals(client.getActiviteClient()))
                    .count();
            stats.put(activite.getDisplayName(), count);
        }
        return stats;
    }

    // Calcule les statistiques pour les rendez-vous
    private Map<String, Long> calculateRendezVousStats(List<Client> clients) {
        Map<String, Long> stats = new LinkedHashMap<>();
        long countYes = clients.stream()
                .filter(client -> Boolean.TRUE.equals(client.getRendezVousAgence()))
                .count();
        long countNo = clients.stream()
                .filter(client -> Boolean.FALSE.equals(client.getRendezVousAgence()))
                .count();
        stats.put("Oui", countYes);
        stats.put("Non", countNo);
        return stats;
    }

    // Calcule les statistiques par branche
    private Map<String, Long> calculateBrancheStats(List<Client> clients) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Branche branche : Branche.values()) {
            long count = clients.stream()
                    .filter(client -> branche.equals(client.getNMBRA()))
                    .count();
            stats.put(branche.getDisplayName(), count);
        }
        return stats;
    }

    // Calcule les statistiques de progression par mois
    private Map<String, Long> calculateProgressionStats(List<Client> clients) {
        Map<String, Long> stats = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // Pour les 6 derniers mois
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            String monthYear = start.format(DateTimeFormatter.ofPattern("MM/yyyy"));

            long contactedCount = clients.stream()
                    .filter(client -> client.getStatus() == ClientStatus.CONTACTE &&
                            client.getUpdatedAt() != null &&
                            client.getUpdatedAt().isAfter(start) &&
                            client.getUpdatedAt().isBefore(end))
                    .count();

            stats.put(monthYear, contactedCount);
        }

        return stats;
    }

    // Calcule les statistiques de progression pour une période spécifique
    private Map<String, Long> calculateProgressionByMonthForFilteredClients(List<Client> clients, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();

        // Calculer le nombre de mois entre start et end
        long monthsBetween = ChronoUnit.MONTHS.between(
                start.toLocalDate().withDayOfMonth(1),
                end.toLocalDate().withDayOfMonth(1)
        ) + 1;

        // Limiter à 6 mois maximum pour l'affichage
        int monthsToShow = (int) Math.min(monthsBetween, 6);

        LocalDateTime currentEnd = end;
        for (int i = 0; i < monthsToShow; i++) {
            LocalDateTime monthStart = currentEnd.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

            // Si le mois dépasse la date de début, ajuster
            if (monthStart.isBefore(start)) {
                monthStart = start;
            }

            String monthYear = monthStart.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            final LocalDateTime finalMonthStart = monthStart;
            final LocalDateTime finalMonthEnd = monthEnd;

            long contactedCount = clients.stream()
                    .filter(client -> client.getStatus() == ClientStatus.CONTACTE &&
                            client.getUpdatedAt() != null &&
                            client.getUpdatedAt().isAfter(finalMonthStart) &&
                            client.getUpdatedAt().isBefore(finalMonthEnd))
                    .count();

            stats.put(monthYear, contactedCount);

            // Passer au mois précédent
            currentEnd = monthStart.minusDays(1);
        }

        return stats;
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

    // Ajouter dans ReportController.java
    @GetMapping("/api/regions")
    @ResponseBody
    public List<Map<String, Object>> getRegions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);

        if (!isSuperAdmin) {
            return Collections.emptyList();
        }

        // Récupérer toutes les régions
        List<Region> regions = regionService.findAll();

        // Convertir en format approprié pour JSON
        return regions.stream()
                .map(region -> {
                    Map<String, Object> regionMap = new HashMap<>();
                    regionMap.put("id", region.getId());
                    regionMap.put("name", region.getName());
                    return regionMap;
                })
                .collect(Collectors.toList());
    }
    @GetMapping("/api/branches")
    @ResponseBody
    public List<Map<String, Object>> getBranches() {
        try {
            // Récupérer l'authentification courante
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByEmail(auth.getName());

            // Vérifier les rôles
            boolean isSuperAdmin = userService.isSuperAdmin(currentUser);
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            List<Branche> branches = new ArrayList<>();

            // Si c'est un super admin, récupérer toutes les branches
            if (isSuperAdmin) {
                branches = Arrays.asList(Branche.values());
            }
            // Si c'est un admin normal
            else if (isAdmin) {
                // Récupérer les régions associées à cet admin
                List<Region> adminRegions = currentUser.getRegions();
                // Si l'admin n'a pas de régions directement associées mais a une direction
                if ((adminRegions == null || adminRegions.isEmpty()) && currentUser.getDirection() != null) {
                    // Récupérer les régions associées à sa direction
                    adminRegions = currentUser.getDirection().getRegions();
                }

                if (adminRegions != null && !adminRegions.isEmpty()) {
                    // Extraire les codes de régions
                    List<String> regionCodes = adminRegions.stream()
                            .map(Region::getCode)
                            .collect(Collectors.toList());

                    System.out.println("Codes des régions de l'admin: " + regionCodes);

                    // Filtrer les branches par ces codes de régions
                    branches = Arrays.stream(Branche.values())
                            .filter(branche -> {
                                String brancheRegionCode = branche.getRegionCode();
                                return brancheRegionCode != null && regionCodes.contains(brancheRegionCode);
                            })
                            .collect(Collectors.toList());
                } else {
                    // Par sécurité, si l'admin n'a aucune région, on lui donne toutes les branches
                    branches = Arrays.asList(Branche.values());
                }
            }

            System.out.println("Nombre de branches récupérées: " + branches.size());

            // Convertir en format JSON
            return branches.stream()
                    .map(branche -> {
                        Map<String, Object> brancheMap = new HashMap<>();
                        brancheMap.put("value", branche.name());
                        brancheMap.put("label", branche.getDisplayName());
                        brancheMap.put("regionCode", branche.getRegionCode());
                        return brancheMap;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des branches: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}