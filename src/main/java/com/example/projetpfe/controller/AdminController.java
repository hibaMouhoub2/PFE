package com.example.projetpfe.controller;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.Impl.AuditService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    @Autowired
    private ExcelExportUtil excelExportUtil;
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

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
    @Autowired
    private AuditService auditService;

    @GetMapping("/clients")
    public String listAllClients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String branche,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rdvDate,
            Model model) {

        // Conversion du statut en enum
        ClientStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = ClientStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Ignorer si le statut n'est pas valide
            }
        }

        // Conversion de la branche en enum
        Branche brancheEnum = null;
        if (branche != null && !branche.isEmpty()) {
            try {
                brancheEnum = Branche.valueOf(branche);
            } catch (IllegalArgumentException e) {
                // Ignorer si la branche n'est pas valide
            }
        }

        // Récupérer les clients sans le filtre de date
        List<Client> clients = clientRepository.findByFiltersWithBranche(q, statusEnum, userId, brancheEnum);

        // Filtrer manuellement par date si nécessaire
        if (rdvDate != null) {
            clients = clients.stream()
                    .filter(client -> client.getRendezVousAgence() != null &&
                            client.getRendezVousAgence() &&
                            client.getDateHeureRendezVous() != null &&
                            client.getDateHeureRendezVous().toLocalDate().equals(rdvDate))
                    .collect(Collectors.toList());
        }

        // Ajouter la liste des utilisateurs pour le filtre
        List<UserDto> users = userService.findAllUsers();

        model.addAttribute("clients", clients);
        model.addAttribute("users", users);
        model.addAttribute("searchQuery", q);
        model.addAttribute("clientCount", clients.size());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("rdvDate", rdvDate);
        model.addAttribute("selectedBranche", brancheEnum);

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
        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);

        List<Client> unassignedClients = clientService.findUnassignedClients();

        // Filtrer manuellement si ce n'est pas un super admin
        if (!isSuperAdmin) {
            List<String> regionNames = currentUser.getRegions().stream()
                    .map(Region::getName)
                    .collect(Collectors.toList());

            System.out.println("DEBUG: Régions de l'utilisateur: " + regionNames);

            // Filtrer par correspondance partielle
            unassignedClients = unassignedClients.stream()
                    .filter(client -> {
                        String nmreg = client.getNMREG();
                        if (nmreg == null) return false;

                        // Vérifier si une des régions de l'utilisateur correspond partiellement
                        return regionNames.stream()
                                .anyMatch(regionName -> nmreg.contains(regionName) ||
                                        regionName.contains(nmreg));
                    })
                    .collect(Collectors.toList());
        }

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
            // Récupérer l'utilisateur assigné pour l'audit
            User assignedUser = userService.findById(userId);

            // Récupérer l'admin qui fait l'assignation
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth.getName();

            // Audit de l'assignation multiple
            auditService.auditEvent(AuditType.CLIENTS_BULK_ASSIGNED,
                    "User",
                    userId,
                    count + " client(s) assigné(s) à " + assignedUser.getName(),
                    adminEmail);
            redirectAttributes.addFlashAttribute("success", count + " client(s) assigné(s) avec succès");
            return "redirect:/admin/unassigned-clients";
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
        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        User currentUser = userRepository.findByEmail(userEmail);

        // Vérifier si l'utilisateur est un super admin ou un admin régional
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);

        // Déclarer la variable regionCodes en dehors du bloc if/else
        List<String> regionCodes = new ArrayList<>();

        // Si ce n'est pas un super admin, récupérer les codes de région
        if (!isSuperAdmin) {
            regionCodes = currentUser.getRegions() != null ?
                    currentUser.getRegions().stream()
                            .filter(region -> region != null && region.getCode() != null)
                            .map(Region::getCode)
                            .collect(Collectors.toList()) :
                    new ArrayList<>();
        }
        System.out.println("User: " + currentUser.getName());
        System.out.println("Regions: " + (currentUser.getRegions() != null ? currentUser.getRegions().size() : "null"));
        if (currentUser.getRegions() != null) {
            for (Region region : currentUser.getRegions()) {
                System.out.println("Region: " + region.getName() + ", Code: " + region.getCode());
            }
        }
        System.out.println("Region codes: " + regionCodes);

        List<Client> clientsNonTraites;
        List<Client> clientsAbsents;
        List<Client> clientsContactes;
        List<Client> clientsRefus;
        List<Client> clientsInjoignables;
        List<Client> clientsNumeroErrone;

        // Pour un super admin, récupérer tous les clients
        if (isSuperAdmin) {
            clientsNonTraites = clientService.findByStatus(ClientStatus.NON_TRAITE);
            clientsAbsents = clientService.findByStatus(ClientStatus.ABSENT);
            clientsContactes = clientService.findByStatus(ClientStatus.CONTACTE);
            clientsRefus = clientService.findByStatus(ClientStatus.REFUS);
            clientsInjoignables = clientService.findByStatus(ClientStatus.INJOIGNABLE);
            clientsNumeroErrone = clientService.findByStatus(ClientStatus.NUMERO_ERRONE);
        }
        // Pour un admin régional, filtrer par région
        else {
            clientsNonTraites = clientService.findByStatusAndRegions(ClientStatus.NON_TRAITE, regionCodes);
            clientsAbsents = clientService.findByStatusAndRegions(ClientStatus.ABSENT, regionCodes);
            clientsContactes = clientService.findByStatusAndRegions(ClientStatus.CONTACTE, regionCodes);
            clientsRefus = clientService.findByStatusAndRegions(ClientStatus.REFUS, regionCodes);
            clientsInjoignables = clientService.findByStatusAndRegions(ClientStatus.INJOIGNABLE, regionCodes);
            clientsNumeroErrone = clientService.findByStatusAndRegions(ClientStatus.NUMERO_ERRONE, regionCodes);
        }

        // Récupérer les rappels (filtrer également par région si nécessaire)
        List<Rappel> rappels = isSuperAdmin ? rappelService.getAllActiveRappels() :
                rappelService.getAllActiveRappelsByRegions(regionCodes);

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

        Map<String, Object> stats = new HashMap<>();
        if (isSuperAdmin) {
            stats.put("nonTraites", clientRepository.countByStatus(ClientStatus.NON_TRAITE));
            stats.put("absents", clientRepository.countByStatus(ClientStatus.ABSENT));
            stats.put("contactes", clientRepository.countByStatus(ClientStatus.CONTACTE));
            stats.put("refus", clientRepository.countByStatus(ClientStatus.REFUS));
            stats.put("injoignables", clientRepository.countByStatus(ClientStatus.INJOIGNABLE));
            stats.put("numeroErrones", clientRepository.countByStatus(ClientStatus.NUMERO_ERRONE));
        } else {
            stats.put("nonTraites", clientRepository.countByStatusAndNMREGIn(ClientStatus.NON_TRAITE, regionCodes));
            stats.put("absents", clientRepository.countByStatusAndNMREGIn(ClientStatus.ABSENT, regionCodes));
            stats.put("contactes", clientRepository.countByStatusAndNMREGIn(ClientStatus.CONTACTE, regionCodes));
            stats.put("refus", clientRepository.countByStatusAndNMREGIn(ClientStatus.REFUS, regionCodes));
            stats.put("injoignables", clientRepository.countByStatusAndNMREGIn(ClientStatus.INJOIGNABLE, regionCodes));
            stats.put("numeroErrones", clientRepository.countByStatusAndNMREGIn(ClientStatus.NUMERO_ERRONE, regionCodes));
        }
        // Ajouter la liste des utilisateurs pour l'assignation
        List<UserDto> users = userService.findAllUsers();

        model.addAttribute("clientsNonTraites", clientsNonTraites);
        model.addAttribute("clientsAbsents", clientsAbsents);
        model.addAttribute("clientsContactes", clientsContactes);
        model.addAttribute("clientsRefus", clientsRefus);
        model.addAttribute("clientsInjoignables", clientsInjoignables);
        model.addAttribute("clientsNumeroErrone", clientsNumeroErrone);
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
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rdvDate) throws IOException {

        // Conversion du statut en enum
        ClientStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = ClientStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Ignorer si le statut n'est pas valide
            }
        }

        // Utiliser la méthode du repository sans le filtre de date
        List<Client> clients = clientRepository.findByFilters(q, statusEnum, userId);

        // Filtrer manuellement par date si nécessaire
        if (rdvDate != null) {
            clients = clients.stream()
                    .filter(client -> client.getRendezVousAgence() != null &&
                            client.getRendezVousAgence() &&
                            client.getDateHeureRendezVous() != null &&
                            client.getDateHeureRendezVous().toLocalDate().equals(rdvDate))
                    .collect(Collectors.toList());
        }

        // Générer le fichier Excel
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);

        // Audit de l'exportation
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        StringBuilder filters = new StringBuilder();
        if (q != null && !q.isEmpty()) filters.append("Recherche: ").append(q).append(", ");
        if (statusEnum != null) filters.append("Statut: ").append(statusEnum).append(", ");
        if (userId != null) filters.append("Utilisateur: ").append(userId).append(", ");
        if (rdvDate != null) filters.append("Date RDV: ").append(rdvDate);

        String filterText = filters.length() > 0 ? " avec filtres: " + filters.toString() : "";

        auditService.auditEvent(AuditType.EXCEL_EXPORT,
                "Client",
                null,
                "Export Excel de " + clients.size() + " clients" + filterText,
                userEmail);

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
            // Récupérer l'email de l'utilisateur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();

            ClientService.ImportResult result = clientService.importClientsFromExcel(file, userEmail);

            String message = result.getImportedCount() + " client(s) importé(s) avec succès";

            if (result.getSkippedCount() > 0) {
                int skippedForRegion = result.getSkippedForRegion().size();
                int skippedForDuplicate = result.getSkippedCins().size();

                if (skippedForDuplicate > 0) {
                    message += ". " + skippedForDuplicate + " client(s) ignoré(s) car leur CIN existe déjà dans la base de données";
                }

                if (skippedForRegion > 0) {
                    message += ". " + skippedForRegion + " client(s) ignoré(s) car ils n'appartiennent pas à votre région";
                }
            }

            redirectAttributes.addFlashAttribute("success", message);
            return "redirect:/admin/clients";
        } catch (Exception e) {
            e.printStackTrace(); // Ajouter pour le débogage
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

        // Audit de l'exportation
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        String brancheInfo = brancheEnum != null ? " pour la branche " + brancheEnum.getDisplayName() : " pour toutes les agences";

        auditService.auditEvent(AuditType.EXCEL_EXPORT,
                "RendezVous",
                null,
                "Export Excel des rendez-vous du " +
                        exportDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                        brancheInfo + " (" + clientsWithRdv.size() + " rendez-vous)",
                userEmail);

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

            // Audit de l'envoi par email
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();

            String brancheInfo = brancheEnum != null ? " pour la branche " + brancheEnum.getDisplayName() : " pour toutes les agences";

            auditService.auditEvent(AuditType.EMAIL_EXPORT,
                    "RendezVous",
                    null,
                    "Email d'export des rendez-vous du " +
                            exportDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                            brancheInfo + " envoyé à " + emailTo +
                            " (" + clientsWithRdv.size() + " rendez-vous)",
                    userEmail);

            redirectAttributes.addFlashAttribute("success", "Email envoyé avec succès à " + emailTo);
            return "redirect:/admin/clients";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'envoi de l'email: " + e.getMessage());
            return "redirect:/admin/clients";
        }
    }

    @PostMapping("/clients/{id}/delete")
    public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            clientService.deleteClient(id);
            redirectAttributes.addFlashAttribute("success", "Client supprimé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/clients";
    }
}