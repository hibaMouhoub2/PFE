package com.example.projetpfe.controller;

import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.Impl.AuditService;
import com.example.projetpfe.service.Impl.ClientService;
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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/agent")
@PreAuthorize("hasRole('USER')")
public class AgentController {

    @Autowired
    private ExcelExportUtil excelExportUtil;

    @Autowired
    private ClientService clientService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Compte les rendez-vous de l'agent pour une période donnée (AJAX)
     */
    @GetMapping("/count-rendez-vous")
    public ResponseEntity<Map<String, Object>> countRendezVous(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> response = new HashMap<>();

        // Validation de la période (max 2 mois)
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 60) {
            response.put("error", "La période ne peut pas dépasser 2 mois");
            return ResponseEntity.badRequest().body(response);
        }

        if (startDate.isAfter(endDate)) {
            response.put("error", "La date de début doit être antérieure à la date de fin");
            return ResponseEntity.badRequest().body(response);
        }

        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        // Compter les rendez-vous de cet agent
        List<Client> myRendezVous = getMyRendezVousForPeriod(userEmail, startDate, endDate);

        response.put("count", myRendezVous.size());
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Export Excel des rendez-vous de l'agent (RÉUTILISE la logique existante)
     */
    @GetMapping("/export/rendez-vous")
    public ResponseEntity<byte[]> exportMyRendezVous(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

        // Validation de la période
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 60) {
            return ResponseEntity.badRequest().build();
        }

        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        // Récupérer MES rendez-vous uniquement
        List<Client> myRendezVous = getMyRendezVousForPeriod(userEmail, startDate, endDate);

        // RÉUTILISER la même méthode d'export Excel que les admins
        byte[] excelContent = excelExportUtil.exportClientsToExcel(myRendezVous);

        // Audit
        String dateInfo = startDate.equals(endDate) ?
                " du " + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) :
                " du " + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                        " au " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        auditService.auditEvent(AuditType.EXCEL_EXPORT,
                "RendezVous",
                null,
                "Export Excel - MES rendez-vous" + dateInfo +
                        " (agent: " + userEmail + ", " + myRendezVous.size() + " rendez-vous)",
                userEmail);

        // RÉUTILISER la même logique de réponse HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        String filename = "mes_rendez_vous" +
                "_du_" + startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "_au_" + endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    /**
     * Méthode privée pour récupérer les rendez-vous de l'agent
     * RÉUTILISE la logique métier existante avec filtrage par agent
     */
    private List<Client> getMyRendezVousForPeriod(String userEmail, LocalDate startDate, LocalDate endDate) {
        // Récupérer l'utilisateur
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            return List.of();
        }

        // RÉUTILISER la méthode existante du service pour chaque jour
        List<Client> allMyRendezVous = List.of();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // Utiliser la méthode existante et filtrer par agent assigné
            List<Client> dayRendezVous = clientService.findClientsWithRendezVousForDateAndBranche(current, null);

            // Filtrer pour ne garder que MES clients
            List<Client> myDayRendezVous = dayRendezVous.stream()
                    .filter(client -> client.getAssignedUser() != null &&
                            client.getAssignedUser().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());

            allMyRendezVous = Stream.concat(allMyRendezVous.stream(), myDayRendezVous.stream())
                    .collect(Collectors.toList());

            current = current.plusDays(1);
        }

        return allMyRendezVous;
    }
}