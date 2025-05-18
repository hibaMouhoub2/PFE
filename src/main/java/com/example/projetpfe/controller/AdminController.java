package com.example.projetpfe.controller;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.DirectionRepository;
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
    @Autowired
    private DirectionRepository directionRepository;

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

        System.out.println("DEBUG - Entrée dans listAllClients");

        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);
        boolean isDirectionAdmin = userService.isDirectionAdmin(currentUser);

        System.out.println("DEBUG - Utilisateur: " + currentUser.getName() +
                ", est super admin: " + isSuperAdmin +
                ", est directeur: " + isDirectionAdmin);

        // Conversion du statut en enum
        ClientStatus statusEnum = null;
        try {
            if (status != null && !status.isEmpty()) {
                statusEnum = ClientStatus.valueOf(status);
                System.out.println("DEBUG - Statut: " + statusEnum);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("DEBUG - Statut invalide: " + status);
        }

        // Conversion de la branche en enum
        Branche brancheEnum = null;
        try {
            if (branche != null && !branche.isEmpty()) {
                brancheEnum = Branche.valueOf(branche);
                System.out.println("DEBUG - Branche: " + brancheEnum);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("DEBUG - Branche invalide: " + branche);
        }

        List<Client> clients;

        // Si c'est un super admin, afficher tous les clients
        if (isSuperAdmin) {
            System.out.println("DEBUG - Mode super admin: récupération de tous les clients");
            // Récupérer les clients sans le filtre de date
            clients = clientRepository.findByFiltersWithBranche(q, statusEnum, userId, brancheEnum);
        }
        // Si c'est un directeur de division
        else if (isDirectionAdmin && currentUser.getDirection() != null) {
            String directionCode = currentUser.getDirection().getCode();
            System.out.println("DEBUG - Mode directeur: filtrage par direction " + directionCode);

            // Récupérer les clients par direction
            if (statusEnum != null) {
                clients = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, statusEnum);
            } else {
                clients = clientRepository.findByNMDIROrderByUpdatedAtDesc(directionCode);
            }

            // Appliquer des filtres supplémentaires manuellement
            if (userId != null) {
                final Long finalUserId = userId;
                clients = clients.stream()
                        .filter(client -> client.getAssignedUser() != null &&
                                client.getAssignedUser().getId().equals(finalUserId))
                        .collect(Collectors.toList());
            }

            if (q != null && !q.isEmpty()) {
                final String query = q.toLowerCase();
                clients = clients.stream()
                        .filter(client ->
                                (client.getNom() != null && client.getNom().toLowerCase().contains(query)) ||
                                        (client.getPrenom() != null && client.getPrenom().toLowerCase().contains(query)) ||
                                        (client.getCin() != null && client.getCin().toLowerCase().contains(query)))
                        .collect(Collectors.toList());
            }

            if (brancheEnum != null) {
                final Branche finalBrancheEnum = brancheEnum;
                clients = clients.stream()
                        .filter(client -> client.getNMBRA() == finalBrancheEnum)
                        .collect(Collectors.toList());
            }
        }
        // Mode admin régional (ancien comportement)
        else {
            System.out.println("DEBUG - Mode admin régional: filtrage par région");
            // Utiliser la méthode auxiliaire pour le filtrage
            clients = filterClientsByRegion(currentUser, statusEnum, userId, q);

            // Filtre de branche en dehors de la méthode auxiliaire
            if (brancheEnum != null) {
                final Branche finalBrancheEnum = brancheEnum;
                clients = clients.stream()
                        .filter(client -> client.getNMBRA() == finalBrancheEnum)
                        .collect(Collectors.toList());
            }
        }

        // Filtrer manuellement par date si nécessaire (pour tous les types d'utilisateurs)
        if (rdvDate != null) {
            System.out.println("DEBUG - Application du filtre de date: " + rdvDate);
            final LocalDate finalRdvDate = rdvDate;
            clients = clients.stream()
                    .filter(client -> client.getRendezVousAgence() != null &&
                            client.getRendezVousAgence() &&
                            client.getDateHeureRendezVous() != null &&
                            client.getDateHeureRendezVous().toLocalDate().equals(finalRdvDate))
                    .collect(Collectors.toList());
            System.out.println("DEBUG - Après filtre date: " + clients.size() + " clients");
        }

        // Ajouter la liste des utilisateurs pour le filtre
        List<UserDto> users;
        if (isSuperAdmin) {
            users = userService.findAllUsers();
        } else if (isDirectionAdmin && currentUser.getDirection() != null) {
            // Pour un directeur, uniquement les utilisateurs de sa direction
            users = userService.findUsersByDirection(currentUser.getDirection().getId());
        } else {
            // Pour un admin régional, seulement ses utilisateurs créés
            users = userService.findUsersByCreator(currentUser.getId());
        }
        System.out.println("DEBUG - Nombre d'utilisateurs disponibles: " + users.size());

        model.addAttribute("clients", clients);
        model.addAttribute("users", users);
        model.addAttribute("searchQuery", q);
        model.addAttribute("clientCount", clients.size());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("rdvDate", rdvDate);
        model.addAttribute("selectedBranche", brancheEnum);

        System.out.println("DEBUG - Nombre final de clients affichés: " + clients.size());
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

        List<Client> unassignedClients;
        List<UserDto> eligibleUsers;

        // Si c'est un super admin, récupérer tous les clients non assignés
        if (isSuperAdmin) {
            unassignedClients = clientService.findUnassignedClients();
            eligibleUsers = userService.findAllUsers();
        } else {
            // Pour un admin de direction, filtrer par sa direction
            String directionCode = currentUser.getDirection() != null ?
                    currentUser.getDirection().getCode() : null;

            if (directionCode != null) {
                unassignedClients = clientService.findUnassignedClientsByDirection(directionCode);

                // Récupérer uniquement les utilisateurs de cette direction
                eligibleUsers = userService.findUsersByDirection(currentUser.getDirection().getId());
            } else {
                unassignedClients = new ArrayList<>();
                eligibleUsers = new ArrayList<>();
            }
        }

        model.addAttribute("clients", unassignedClients);
        model.addAttribute("users", eligibleUsers);

        // Ajouter les branches pour le filtrage
        model.addAttribute("branches", Branche.values());

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
            // Récupérer le client et l'utilisateur cible
            Client client = clientService.getById(id);
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

            // Récupérer l'utilisateur courant (admin)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByEmail(auth.getName());
            boolean isSuperAdmin = userService.isSuperAdmin(currentUser);

            // Vérifier que l'admin peut assigner ce client (basé sur la direction)
            if (!isSuperAdmin && currentUser.getDirection() != null) {
                String adminDirectionCode = currentUser.getDirection().getCode();

                // Vérifier si le client appartient à la direction de l'admin
                if (client.getNMDIR() == null || !client.getNMDIR().equals(adminDirectionCode)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Vous ne pouvez assigner que des clients de votre direction");
                    return "redirect:/admin/clients";
                }

                // Vérifier si l'utilisateur cible appartient à la direction de l'admin
                // OU s'il a été créé par cet admin
                boolean isUserFromSameDirection = targetUser.getDirection() != null &&
                        targetUser.getDirection().getId().equals(currentUser.getDirection().getId());
                boolean isUserCreatedByAdmin = targetUser.getCreatedByAdmin() != null &&
                        targetUser.getCreatedByAdmin().getId().equals(currentUser.getId());

                if (!isUserFromSameDirection && !isUserCreatedByAdmin) {
                    redirectAttributes.addFlashAttribute("error",
                            "Vous ne pouvez assigner des clients qu'aux utilisateurs de votre direction ou que vous avez créés");
                    return "redirect:/admin/clients";
                }

                // Vérifier que l'agent a la bonne branche si le client a une branche définie
                if (client.getNMBRA() != null && targetUser.getAssignedBranche() != null &&
                        !client.getNMBRA().equals(targetUser.getAssignedBranche())) {
                    redirectAttributes.addFlashAttribute("error",
                            "Vous ne pouvez assigner ce client qu'à un agent de la branche " +
                                    client.getNMBRA().getDisplayName());
                    return "redirect:/admin/clients";
                }
            }

            // Si toutes les vérifications passent, assigner le client
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
            // Récupérer l'authentification
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByEmail(auth.getName());
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

            boolean isSuperAdmin = userService.isSuperAdmin(currentUser);

            // Vérifications pour les administrateurs de direction
            if (!isSuperAdmin && currentUser.getDirection() != null) {
                String adminDirectionCode = currentUser.getDirection().getCode();

                // Vérifier si l'utilisateur cible appartient à la direction de l'admin OU a été créé par cet admin
                boolean isUserFromSameDirection = targetUser.getDirection() != null &&
                        targetUser.getDirection().getId().equals(currentUser.getDirection().getId());
                boolean isUserCreatedByAdmin = targetUser.getCreatedByAdmin() != null &&
                        targetUser.getCreatedByAdmin().getId().equals(currentUser.getId());

                if (!isUserFromSameDirection && !isUserCreatedByAdmin) {
                    redirectAttributes.addFlashAttribute("error",
                            "Vous ne pouvez assigner des clients qu'aux utilisateurs de votre direction ou que vous avez créés");
                    return "redirect:/admin/unassigned-clients";
                }

                // Vérifier que tous les clients appartiennent à la direction de l'admin
                for (Long clientId : clientIds) {
                    Client client = clientService.getById(clientId);
                    if (client.getNMDIR() == null || !client.getNMDIR().equals(adminDirectionCode)) {
                        redirectAttributes.addFlashAttribute("error",
                                "Certains clients sélectionnés n'appartiennent pas à votre direction");
                        return "redirect:/admin/unassigned-clients";
                    }

                    // Vérifier la correspondance des branches si applicable
                    if (targetUser.getAssignedBranche() != null && client.getNMBRA() != null &&
                            !client.getNMBRA().equals(targetUser.getAssignedBranche())) {
                        redirectAttributes.addFlashAttribute("error",
                                "Certains clients ne correspondent pas à la branche de l'agent sélectionné");
                        return "redirect:/admin/unassigned-clients";
                    }
                }
            }

            // Si toutes les vérifications passent, assigner les clients
            int count = 0;
            for (Long clientId : clientIds) {
                clientService.assignToUser(clientId, userId);
                count++;
            }

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

        // Vérifier si l'utilisateur est un super admin ou un admin régional/directeur
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);
        boolean isDirectionAdmin = userService.isDirectionAdmin(currentUser);

        System.out.println("User: " + currentUser.getName());
        System.out.println("Is Super Admin: " + isSuperAdmin);
        System.out.println("Is Direction Admin: " + isDirectionAdmin);
        if (isDirectionAdmin && currentUser.getDirection() != null) {
            System.out.println("Direction: " + currentUser.getDirection().getName() +
                    ", Code: " + currentUser.getDirection().getCode());
        }

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
        // Pour un directeur de division
        else if (isDirectionAdmin && currentUser.getDirection() != null) {
            String directionCode = currentUser.getDirection().getCode();
            System.out.println("Filtering by direction code: " + directionCode);

            clientsNonTraites = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.NON_TRAITE);
            clientsAbsents = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.ABSENT);
            clientsContactes = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.CONTACTE);
            clientsRefus = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.REFUS);
            clientsInjoignables = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.INJOIGNABLE);
            clientsNumeroErrone = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.NUMERO_ERRONE);
        }
        // Pour un admin régional, utiliser la méthode de filtrage souple
        else {
            clientsNonTraites = filterClientsByRegion(currentUser, ClientStatus.NON_TRAITE, null, null);
            clientsAbsents = filterClientsByRegion(currentUser, ClientStatus.ABSENT, null, null);
            clientsContactes = filterClientsByRegion(currentUser, ClientStatus.CONTACTE, null, null);
            clientsRefus = filterClientsByRegion(currentUser, ClientStatus.REFUS, null, null);
            clientsInjoignables = filterClientsByRegion(currentUser, ClientStatus.INJOIGNABLE, null, null);
            clientsNumeroErrone = filterClientsByRegion(currentUser, ClientStatus.NUMERO_ERRONE, null, null);
        }

        // Ajouter des logs pour déboguer
        System.out.println("Clients non traités: " + clientsNonTraites.size());
        System.out.println("Clients absents: " + clientsAbsents.size());
        System.out.println("Clients contactés: " + clientsContactes.size());
        System.out.println("Clients refus: " + clientsRefus.size());
        System.out.println("Clients injoignables: " + clientsInjoignables.size());
        System.out.println("Clients numéro erroné: " + clientsNumeroErrone.size());

        // Récupérer les rappels
        List<Rappel> rappels;
        if (isSuperAdmin) {
            rappels = rappelService.getAllActiveRappels();
        } else if (isDirectionAdmin && currentUser.getDirection() != null) {
            // Récupérer les rappels pour cette direction
            String directionCode = currentUser.getDirection().getCode();
            rappels = rappelService.getAllActiveRappels().stream()
                    .filter(rappel -> {
                        Client client = rappel.getClient();
                        return client != null && directionCode.equals(client.getNMDIR());
                    })
                    .collect(Collectors.toList());
        } else {
            // Pour un admin régional, filtrer les rappels manuellement
            final List<String> regionCodes = currentUser.getRegions().stream()
                    .filter(region -> region != null && region.getCode() != null)
                    .map(Region::getCode)
                    .collect(Collectors.toList());

            rappels = rappelService.getAllActiveRappels().stream()
                    .filter(rappel -> {
                        Client client = rappel.getClient();
                        if (client == null || client.getNMREG() == null) return false;

                        String clientRegion = client.getNMREG().toUpperCase().replace(" ", "");

                        return regionCodes.stream().anyMatch(regionCode -> {
                            String normalizedRegionCode = regionCode.toUpperCase().replace("_", "");
                            return clientRegion.contains(normalizedRegionCode) ||
                                    normalizedRegionCode.contains(clientRegion) ||
                                    (clientRegion.contains("SIDI") && normalizedRegionCode.contains("SEDY")) ||
                                    (clientRegion.contains("SEDY") && normalizedRegionCode.contains("SIDI")) ||
                                    (clientRegion.contains("FIDAA") && normalizedRegionCode.contains("FIDA")) ||
                                    (clientRegion.contains("FIDA") && normalizedRegionCode.contains("FIDAA"));
                        });
                    })
                    .collect(Collectors.toList());
        }

        System.out.println("Rappels actifs: " + rappels.size());

        // Le reste du code reste le même...
        // [code existant pour clientRappelMap, todayRappels, stats, etc.]

        model.addAttribute("clientsNonTraites", clientsNonTraites);
        model.addAttribute("clientsAbsents", clientsAbsents);
        model.addAttribute("clientsContactes", clientsContactes);
        model.addAttribute("clientsRefus", clientsRefus);
        model.addAttribute("clientsInjoignables", clientsInjoignables);
        model.addAttribute("clientsNumeroErrone", clientsNumeroErrone);
        model.addAttribute("rappels", rappels);

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

        // Utiliser les tailles des listes filtrées pour les statistiques
        stats.put("nonTraites", clientsNonTraites.size());
        stats.put("absents", clientsAbsents.size());
        stats.put("contactes", clientsContactes.size());
        stats.put("refus", clientsRefus.size());
        stats.put("injoignables", clientsInjoignables.size());
        stats.put("numeroErrones", clientsNumeroErrone.size());

        // Ajouter la liste des utilisateurs pour l'assignation
        List<UserDto> users = userService.findAllUsers();

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

        System.out.println("DEBUG - Entrée dans exportClients");

        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        User currentUser = userRepository.findByEmail(userEmail);
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);

        System.out.println("DEBUG - Utilisateur: " + currentUser.getName() + ", est super admin: " + isSuperAdmin);

        // Conversion du statut en enum
        ClientStatus statusEnum = null;
        try {
            if (status != null && !status.isEmpty()) {
                statusEnum = ClientStatus.valueOf(status);
                System.out.println("DEBUG - Statut: " + statusEnum);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("DEBUG - Statut invalide: " + status);
        }

        List<Client> clients;

        // Si c'est un super admin, afficher tous les clients
        if (isSuperAdmin) {
            System.out.println("DEBUG - Mode super admin: récupération de tous les clients");
            clients = clientRepository.findByFilters(q, statusEnum, userId);
        } else {
            System.out.println("DEBUG - Mode directeur régional: filtrage par région");
            // Utiliser la méthode auxiliaire pour le filtrage
            clients = filterClientsByRegion(currentUser, statusEnum, userId, q);
        }

        // Filtrer manuellement par date si nécessaire
        if (rdvDate != null) {
            System.out.println("DEBUG - Application du filtre de date: " + rdvDate);
            final LocalDate finalRdvDate = rdvDate; // Cette variable est effectivement finale
            clients = clients.stream()
                    .filter(client -> client.getRendezVousAgence() != null &&
                            client.getRendezVousAgence() &&
                            client.getDateHeureRendezVous() != null &&
                            client.getDateHeureRendezVous().toLocalDate().equals(finalRdvDate))
                    .collect(Collectors.toList());
            System.out.println("DEBUG - Après filtre date: " + clients.size() + " clients");
        }

        // Générer le fichier Excel
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clients);
        System.out.println("DEBUG - Fichier Excel généré pour " + clients.size() + " clients");

        // Audit de l'exportation
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

        System.out.println("DEBUG - Exportation terminée avec succès");
        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }
    @GetMapping("/export/phone-changes")
    public ResponseEntity<byte[]> exportClientsWithPhoneChanges(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

        // Par défaut, utiliser les 30 derniers jours
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<Client> clientsWithPhoneChanges = clientService.findClientsWithPhoneChanges(start, end);

        // Générer le fichier Excel avec uniquement les données de base (fonction spécifique)
        byte[] excelContent = excelExportUtil.exportClientsWithPhoneChangesToExcel(clientsWithPhoneChanges);

        // Audit de l'exportation
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(AuditType.EXCEL_EXPORT,
                "Client",
                null,
                "Export Excel des clients avec numéro de téléphone modifié entre " +
                        start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " et " +
                        end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (" + clientsWithPhoneChanges.size() + " clients)",
                userEmail);

        // Préparer la réponse HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        // Définir le nom du fichier avec la date actuelle
        String filename = "clients_phone_changes_export_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
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

            StringBuilder message = new StringBuilder();
            message.append(result.getImportedCount()).append(" client(s) importé(s) avec succès");

            if (result.getUpdatedCount() > 0) {
                message.append(", ").append(result.getUpdatedCount())
                        .append(" client(s) mis à jour avec une date de fin de contrat plus récente");
            }

            if (result.getSkippedCount() > 0) {
                int skippedForRegion = result.getSkippedForRegion().size();
                int skippedForDuplicate = result.getSkippedCins().size();

                if (skippedForDuplicate > 0) {
                    message.append(". ").append(skippedForDuplicate)
                            .append(" client(s) ignoré(s) car leur CIN existe déjà et la date de fin n'est pas plus récente");
                }

                if (skippedForRegion > 0) {
                    message.append(". ").append(skippedForRegion)
                            .append(" client(s) ignoré(s) car ils n'appartiennent pas à votre région");
                }
            }

            redirectAttributes.addFlashAttribute("success", message.toString());
            return "redirect:/admin/clients";
        } catch (Exception e) {
            e.printStackTrace(); // Ajouter pour le débogage
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'importation: " + e.getMessage());
            return "redirect:/admin/unassigned-clients";
        }
    }

    @GetMapping("/export/rendez-vous")
    public ResponseEntity<byte[]> exportRendezVous(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String branche) throws IOException {

        // Si startDate est null, utilisez la date du jour
        LocalDate defaultStartDate = startDate != null ? startDate : LocalDate.now();
        // Si endDate est null, utilisez aussi la date du jour (pour un jour spécifique)
        LocalDate defaultEndDate = endDate != null ? endDate : defaultStartDate;

        // Convertir la chaîne de branche en enum si nécessaire
        Branche brancheEnum = null;
        if (branche != null && !branche.isEmpty()) {
            try {
                brancheEnum = Branche.valueOf(branche);
            } catch (IllegalArgumentException e) {
                // Gérer l'erreur si la branche n'est pas valide
            }
        }

        // Récupérer tous les clients avec rendez-vous pour cette plage de dates et cette branche
        List<Client> clientsWithRdv = new ArrayList<>();

        // Parcourir chaque jour dans la plage de dates
        LocalDate currentDate = defaultStartDate;
        while (!currentDate.isAfter(defaultEndDate)) {
            clientsWithRdv.addAll(clientService.findClientsWithRendezVousForDateAndBranche(currentDate, brancheEnum));
            currentDate = currentDate.plusDays(1);
        }

        // Générer le fichier Excel avec tous les détails (fonction existante)
        byte[] excelContent = excelExportUtil.exportClientsToExcel(clientsWithRdv);

        // Audit de l'exportation
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        String brancheInfo = brancheEnum != null ? " pour la branche " + brancheEnum.getDisplayName() : " pour toutes les agences";
        String dateInfo = defaultStartDate.equals(defaultEndDate) ?
                " du " + defaultStartDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) :
                " du " + defaultStartDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                        " au " + defaultEndDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        auditService.auditEvent(AuditType.EXCEL_EXPORT,
                "RendezVous",
                null,
                "Export Excel des rendez-vous" + dateInfo +
                        brancheInfo + " (" + clientsWithRdv.size() + " rendez-vous)",
                userEmail);

        // Préparer la réponse HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        // Définir le nom du fichier
        String brancheStr = brancheEnum != null ? "_" + brancheEnum.name() : "_TOUTES_AGENCES";
        String filename = "rendez_vous" +
                "_du_" + defaultStartDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "_au_" + defaultEndDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                brancheStr + ".xlsx";
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

    private List<Client> filterClientsByRegion(User currentUser, ClientStatus status, Long userId, String searchQuery) {
        // Si l'utilisateur est un directeur de division
        if (userService.isDirectionAdmin(currentUser) && currentUser.getDirection() != null) {
            String directionCode = currentUser.getDirection().getCode();
            System.out.println("DEBUG - Filtrage par direction: " + directionCode);

            // Utiliser directement la méthode du repository pour meilleure performance
            List<Client> clientsFromDirection;

            if (status != null) {
                clientsFromDirection = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, status);
            } else {
                clientsFromDirection = clientRepository.findByNMDIROrderByUpdatedAtDesc(directionCode);
            }

            // Appliquer les filtres additionnels (userId, searchQuery)
            return clientsFromDirection.stream()
                    .filter(client -> {
                        // Filtre par utilisateur assigné si nécessaire
                        if (userId != null && (client.getAssignedUser() == null ||
                                !client.getAssignedUser().getId().equals(userId))) {
                            return false;
                        }

                        // Filtre par texte de recherche si nécessaire
                        if (searchQuery != null && !searchQuery.isEmpty()) {
                            String query = searchQuery.toLowerCase();
                            return (client.getNom() != null && client.getNom().toLowerCase().contains(query)) ||
                                    (client.getPrenom() != null && client.getPrenom().toLowerCase().contains(query)) ||
                                    (client.getCin() != null && client.getCin().toLowerCase().contains(query));
                        }

                        return true;
                    })
                    .collect(Collectors.toList());
        }
        // Sinon, si c'est un administrateur régional (ancienne méthode)
        else if (!currentUser.getRegions().isEmpty()) {
            // Récupérer les codes de régions
            List<String> regionCodes = currentUser.getRegions().stream()
                    .filter(region -> region != null && region.getCode() != null)
                    .map(Region::getCode)
                    .collect(Collectors.toList());

            System.out.println("DEBUG - Filtrage par régions: " + regionCodes);

            // Récupérer tous les clients
            List<Client> allClients = clientRepository.findAll();
            List<Client> filteredClients = new ArrayList<>();

            for (Client client : allClients) {
                // Vérifier si le client appartient à une des régions du directeur
                boolean regionMatch = false;
                String clientRegion = client.getNMREG();

                if (clientRegion != null) {
                    String normalizedClientRegion = clientRegion.toUpperCase().replace(" ", "");

                    for (String regionCode : regionCodes) {
                        String normalizedRegionCode = regionCode.toUpperCase().replace("_", "");

                        if (normalizedClientRegion.contains(normalizedRegionCode) ||
                                normalizedRegionCode.contains(normalizedClientRegion) ||
                                (normalizedClientRegion.contains("SIDI") && normalizedRegionCode.contains("SEDY")) ||
                                (normalizedClientRegion.contains("SEDY") && normalizedRegionCode.contains("SIDI")) ||
                                (normalizedClientRegion.contains("FIDAA") && normalizedRegionCode.contains("FIDA")) ||
                                (normalizedClientRegion.contains("FIDA") && normalizedRegionCode.contains("FIDAA"))) {
                            regionMatch = true;
                            break;
                        }
                    }
                }

                if (!regionMatch) continue;

                // Vérifier le statut
                if (status != null && client.getStatus() != status) continue;

                // Vérifier l'utilisateur assigné
                if (userId != null && (client.getAssignedUser() == null ||
                        !client.getAssignedUser().getId().equals(userId))) {
                    continue;
                }

                // Vérifier la recherche textuelle
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    boolean textMatch = false;
                    String query = searchQuery.toLowerCase();

                    if (client.getNom() != null && client.getNom().toLowerCase().contains(query)) {
                        textMatch = true;
                    } else if (client.getPrenom() != null && client.getPrenom().toLowerCase().contains(query)) {
                        textMatch = true;
                    } else if (client.getCin() != null && client.getCin().toLowerCase().contains(query)) {
                        textMatch = true;
                    }

                    if (!textMatch) continue;
                }

                filteredClients.add(client);
            }

            return filteredClients;
        }

        return new ArrayList<>(); // Retourner une liste vide si pas de direction ni région
    }

    /**
     * Vérifie si l'utilisateur courant peut gérer une direction
     */
    private boolean canCurrentUserManageDirection(String directionCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());
        Direction direction = directionRepository.findByCode(directionCode);

        return userService.isUserManagingDirection(currentUser, direction);
    }

    /**
     * Filtre les clients accessibles par l'utilisateur courant basé sur sa direction
     */
    private List<Client> filterClientsByDirectionAccess(List<Client> clients) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());

        // Super admin voit tout
        if (userService.isSuperAdmin(currentUser)) {
            return clients;
        }
        // Admin de direction filtre par sa direction
        if (userService.isDirectionAdmin(currentUser) && currentUser.getDirection() != null) {
            return clients.stream()
                    .filter(client -> client.getNMDIR() != null &&
                            client.getNMDIR().equals(currentUser.getDirection().getCode()))
                    .collect(Collectors.toList());
        }

        // Utilisateur normal ne voit que ses clients assignés
        return clients.stream()
                .filter(client -> client.getAssignedUser() != null &&
                        client.getAssignedUser().getId().equals(currentUser.getId()))
                .collect(Collectors.toList());
    }
}