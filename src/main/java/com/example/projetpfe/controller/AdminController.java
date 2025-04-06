package com.example.projetpfe.controller;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.EmailService;
import com.example.projetpfe.service.Impl.RappelService;
import com.example.projetpfe.service.UserService;
import com.example.projetpfe.util.ExcelExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    @Autowired
    private ExcelExportUtil excelExportUtil;
    @Autowired
    private ClientRepository clientRepository;

    private final ClientService clientService;
    private final UserService userService;
    private final RappelService rappelService;

    @Autowired
    public AdminController(ClientService clientService,
                           UserService userService,
                           RappelService rappelService) {
        this.clientService = clientService;
        this.userService = userService;
        this.rappelService = rappelService;
    }

    @GetMapping("/clients")
    public String listAllClients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            Model model) {

        // Utiliser la méthode optimisée findByFilters
        ClientStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = ClientStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Ignorer si le statut n'est pas valide
            }
        }

        List<Client> clients = clientRepository.findByFilters(q, statusEnum, userId);

        // Ajouter la liste des utilisateurs pour le filtre
        List<UserDto> users = userService.findAllUsers();

        model.addAttribute("clients", clients);
        model.addAttribute("users", users);
        model.addAttribute("searchQuery", q);
        model.addAttribute("clientCount", clients.size());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedUserId", userId);

        return "admin/clients";
    }

    @GetMapping("/search-results")
    public String showAdminSearchResults(Model model) {
        // Récupérer les résultats de recherche depuis l'attribut flash
        @SuppressWarnings("unchecked")
        List<Long> clientIds = (List<Long>) model.asMap().get("searchResults");
        String searchQuery = (String) model.asMap().get("searchQuery");

        if (clientIds == null || clientIds.isEmpty()) {
            return "redirect:/admin/clients";
        }

        // Récupérer les clients correspondants
        List<Client> clients = clientIds.stream()
                .map(id -> clientService.getById(id))
                .collect(Collectors.toList());

        model.addAttribute("clients", clients);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("clientCount", clients.size());

        // Ajouter la liste des utilisateurs pour d'éventuelles actions
        List<UserDto> users = userService.findAllUsers();
        model.addAttribute("users", users);

        return "admin/clients"; // Utiliser la page clients existante
    }

    @GetMapping("/unassigned-clients")
    public String listUnassignedClients(Model model) {
        List<Client> unassignedClients = clientService.findUnassignedClients();
        List<UserDto> users = userService.findAllUsers();

        model.addAttribute("clients", unassignedClients);
        model.addAttribute("users", users);

        return "admin/unassigned-clients";
    }
//    @GetMapping("/agenda")
//    public String viewGlobalAgenda(Model model) {
//        // Statistiques globales
//        Map<String, Long> globalStats = new HashMap<>();
//
//        // Compter tous les clients par statut
//        globalStats.put("nonTraites", clientRepository.countByStatus(ClientStatus.NON_TRAITE));
//        globalStats.put("absents", clientRepository.countByStatus(ClientStatus.ABSENT));
//        globalStats.put("contactes", clientRepository.countByStatus(ClientStatus.CONTACTE));
//        globalStats.put("refus", clientRepository.countByStatus(ClientStatus.REFUS));
//
//        // Récupérer les clients par statut
//        List<Client> clientsNonTraites = clientService.findByStatus(ClientStatus.NON_TRAITE);
//        List<Client> clientsAbsents = clientService.findByStatus(ClientStatus.ABSENT);
//        List<Client> clientsContactes = clientService.findByStatus(ClientStatus.CONTACTE);
//        List<Client> clientsRefus = clientService.findByStatus(ClientStatus.REFUS);
//
//        // Récupérer tous les rappels actifs
//        List<Rappel> activeRappels = rappelService.getAllActiveRappels();
//
//        // Récupérer la liste des utilisateurs pour les modals d'assignation
//        List<UserDto> users = userService.findAllUsers();
//
//        model.addAttribute("stats", globalStats);
//        model.addAttribute("clientsNonTraites", clientsNonTraites);
//        model.addAttribute("clientsAbsents", clientsAbsents);
//        model.addAttribute("clientsContactes", clientsContactes);
//        model.addAttribute("clientsRefus", clientsRefus);
//        model.addAttribute("rappels", activeRappels);
//        model.addAttribute("users", users);
//
//        return "admin/agenda";
//    }

    @PostMapping("/clients/{id}/assign")
    public String assignClient(@PathVariable Long id,
                               @RequestParam Long userId,
                               RedirectAttributes redirectAttributes) {
        try {
            clientService.assignToUser(id, userId);
            redirectAttributes.addFlashAttribute("success", "Client assigné avec succès");
            return "redirect:/admin/clients";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'assignation: " + e.getMessage());
            return "redirect:/admin/unassigned-clients";
        }
    }
    @PostMapping("/clients/assign-multiple")
    public String assignMultipleClients(@RequestParam(required = false) List<Long> clientIds,
                                        @RequestParam Long userId,
                                        RedirectAttributes redirectAttributes) {
        if (clientIds == null || clientIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucun client sélectionné");
            return "redirect:/admin/unassigned-clients";
        }

        try {
            int count = 0;
            for (Long clientId : clientIds) {
                clientService.assignToUser(clientId, userId);
                count++;
            }
            redirectAttributes.addFlashAttribute("success", count + " client(s) assigné(s) avec succès");
            return "redirect:/admin/clients";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'assignation multiple: " + e.getMessage());
            return "redirect:/admin/unassigned-clients";
        }
    }

    @GetMapping("/reports/daily")
    public String dailyReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              Model model) {

        LocalDate reportDate = date != null ? date : LocalDate.now();

        List<Client> contactedClients = clientService.findContactedOnDate(reportDate);
        List<Rappel> rappelsForDay = rappelService.findByDate(reportDate);

        model.addAttribute("reportDate", reportDate);
        model.addAttribute("contactedClients", contactedClients);
        model.addAttribute("rappels", rappelsForDay);

        return "admin/daily-report";
    }

    @GetMapping("/reports/rendez-vous")
    public String rendezVousReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   Model model) {

        LocalDate reportDate = date != null ? date : LocalDate.now();

        List<Client> clientsWithRdv = clientService.findWithRendezVousOnDate(reportDate);

        model.addAttribute("reportDate", reportDate);
        model.addAttribute("clientsWithRdv", clientsWithRdv);

        return "admin/rendez-vous-report";
    }

//    @GetMapping("/export/clients")
//    public String exportClients(Model model) {
//        // La fonction d'export sera implémentée plus tard
//        model.addAttribute("message", "Fonction d'export en cours de développement");
//        return "admin/export";
//    }
    @GetMapping("/agenda")
    public String viewAdminAgenda(Model model) {
        // Récupération de tous les clients par statut
        List<Client> clientsNonTraites = clientService.findByStatus(ClientStatus.NON_TRAITE);
        List<Client> clientsAbsents = clientService.findByStatus(ClientStatus.ABSENT);
        List<Client> clientsContactes = clientService.findByStatus(ClientStatus.CONTACTE);
        List<Client> clientsRefus = clientService.findByStatus(ClientStatus.REFUS);

        // Récupérer tous les rappels
        List<Rappel> rappels = rappelService.getAllActiveRappels();

        // Créer une map client_id -> rappel pour afficher les dates de rappel
        Map<Long, Rappel> clientRappelMap = new HashMap<>();
        for (Rappel rappel : rappels) {
            if (!rappel.getCompleted()) {
                Long clientId = rappel.getClient().getId();
                // Si plusieurs rappels existent pour un client, prendre le plus récent
                if (!clientRappelMap.containsKey(clientId) ||
                        rappel.getDateRappel().isAfter(clientRappelMap.get(clientId).getDateRappel())) {
                    clientRappelMap.put(clientId, rappel);
                }
            }
        }

        // Récupérer les rappels pour aujourd'hui
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        List<Rappel> todayRappels = rappels.stream()
                .filter(r -> !r.getCompleted() && r.getDateRappel().isAfter(startOfDay) && r.getDateRappel().isBefore(endOfDay))
                .collect(Collectors.toList());

        // Extraire les IDs des clients ayant un rappel aujourd'hui
        Set<Long> todayRappelClientIds = todayRappels.stream()
                .map(r -> r.getClient().getId())
                .collect(Collectors.toSet());

        // Statistiques
        Map<String, Object> stats = new HashMap<>();
        stats.put("nonTraites", clientRepository.countByStatus(ClientStatus.NON_TRAITE));
        stats.put("absents", clientRepository.countByStatus(ClientStatus.ABSENT));
        stats.put("contactes", clientRepository.countByStatus(ClientStatus.CONTACTE));
        stats.put("refus", clientRepository.countByStatus(ClientStatus.REFUS));

        // Ajouter la liste des utilisateurs pour l'assignation
        List<UserDto> users = userService.findAllUsers();

        model.addAttribute("clientsNonTraites", clientsNonTraites);
        model.addAttribute("clientsAbsents", clientsAbsents);
        model.addAttribute("clientsContactes", clientsContactes);
        model.addAttribute("clientsRefus", clientsRefus);
        model.addAttribute("rappels", rappels);
        model.addAttribute("clientRappelMap", clientRappelMap);
        model.addAttribute("todayRappelClientIds", todayRappelClientIds);
        model.addAttribute("stats", stats);
        model.addAttribute("users", users);

        return "admin/agenda";
    }
    @GetMapping("/clients/export")
    public ResponseEntity<byte[]> exportClients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) throws IOException {

        // Utiliser la même logique que dans listAllClients pour appliquer les filtres
        List<Client> clients = clientService.findAll();

        // Appliquer les filtres
        if (q != null && !q.isEmpty()) {
            clients = clients.stream()
                    .filter(client ->
                            (client.getNom() != null && client.getNom().toLowerCase().contains(q.toLowerCase())) ||
                                    (client.getPrenom() != null && client.getPrenom().toLowerCase().contains(q.toLowerCase())) ||
                                    (client.getCin() != null && client.getCin().toLowerCase().contains(q.toLowerCase()))
                    )
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isEmpty()) {
            ClientStatus clientStatus = ClientStatus.valueOf(status);
            clients = clients.stream()
                    .filter(client -> client.getStatus() == clientStatus)
                    .collect(Collectors.toList());
        }

        if (userId != null) {
            clients = clients.stream()
                    .filter(client ->
                            client.getAssignedUser() != null &&
                                    client.getAssignedUser().getId().equals(userId)
                    )
                    .collect(Collectors.toList());
        }

        // Générer le fichier Excel avec tous les attributs
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);

        // Préparer la réponse HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        // Définir le nom du fichier avec la date actuelle
        String filename = "clients_export_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @PostMapping("/clients/import")
    public String importClientsFromExcel(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner un fichier à importer");
            return "redirect:/admin/unassigned-clients";
        }

        try {
            int importedCount = clientService.importClientsFromExcel(file);
            redirectAttributes.addFlashAttribute("success", importedCount + " client(s) importé(s) avec succès");
            return "redirect:/admin/clients";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'importation: " + e.getMessage());
            return "redirect:/admin/unassigned-clients";
        }
    }

    @GetMapping("/export/rendez-vous")
    public ResponseEntity<byte[]> exportRendezVous(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String branche) throws IOException {

        // Par défaut, utiliser la date du jour
        LocalDate exportDate = date != null ? date : LocalDate.now();

        // Convertir la chaîne de branche en enum si nécessaire
        Branche brancheEnum = null;
        if (branche != null && !branche.isEmpty()) {
            try {
                brancheEnum = Branche.valueOf(branche);
            } catch (IllegalArgumentException e) {
                // Gérer l'erreur si la branche n'est pas valide
            }
        }

        // Récupérer les clients avec rendez-vous pour cette date et cette branche
        List<Client> clientsWithRdv = clientService.findClientsWithRendezVousForDateAndBranche(exportDate, brancheEnum);

        // Générer le fichier Excel
        byte[] excelContent = excelExportUtil.exportRendezVousToExcel(clientsWithRdv, exportDate);

        // Préparer la réponse HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        // Définir le nom du fichier
        String brancheStr = brancheEnum != null ? "_" + brancheEnum.name() : "_TOUTES_AGENCES";
        String filename = "rendez_vous_" + exportDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + brancheStr + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    @Autowired
    private EmailService emailService;

    @PostMapping("/export/rendez-vous/email")
    public String exportAndSendRendezVousEmail(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String branche,
            @RequestParam String emailTo,
            RedirectAttributes redirectAttributes) {

        try {
            // Par défaut, utiliser la date du jour
            LocalDate exportDate = date != null ? date : LocalDate.now();

            // Convertir la chaîne de branche en enum si nécessaire
            Branche brancheEnum = null;
            if (branche != null && !branche.isEmpty()) {
                try {
                    brancheEnum = Branche.valueOf(branche);
                } catch (IllegalArgumentException e) {
                    // Gérer l'erreur si la branche n'est pas valide
                }
            }

            // Récupérer les clients avec rendez-vous pour cette date
            List<Client> clientsWithRdv = clientService.findClientsWithRendezVousForDateAndBranche(exportDate, brancheEnum);

            // Générer le fichier Excel
            byte[] excelContent = excelExportUtil.exportRendezVousToExcel(clientsWithRdv, exportDate);

            // Envoyer l'email
            emailService.sendRendezVousSummary(emailTo, clientsWithRdv, exportDate, excelContent);

            redirectAttributes.addFlashAttribute("success", "Email envoyé avec succès à " + emailTo);
            return "redirect:/admin/clients";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'envoi de l'email: " + e.getMessage());
            return "redirect:/admin/clients";
        }
    }
}