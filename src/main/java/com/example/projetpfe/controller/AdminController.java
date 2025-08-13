package com.example.projetpfe.controller;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.BrancheRepository;
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
    private BrancheRepository brancheRepository;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
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

        Branche brancheEnum = null;

        try {
            if (branche != null && !branche.isEmpty()) {
                brancheEnum = brancheRepository.findByCode(branche).orElse(null);
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


        int startIndex = page * size;
        List<Client> pagedClients = getPagedList(clients, startIndex, size);
        model.addAttribute("clients", pagedClients);
        model.addAttribute("allClientsCount", clients.size());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("hasNextPage", hasNextPageForList(clients, startIndex, size));
        model.addAttribute("users", users);
        model.addAttribute("searchQuery", q);
        model.addAttribute("clientCount", clients.size());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("rdvDate", rdvDate);
        model.addAttribute("branches", brancheRepository.findAll());
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
    public String listUnassignedClients(Model model,@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);
        boolean isDirectionAdmin = userService.isDirectionAdmin(currentUser);

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

        int startIndex = page * size;
        List<Client> pagedClients = getPagedList(unassignedClients, startIndex, size);

        model.addAttribute("clients", pagedClients);
        model.addAttribute("allClientsCount", unassignedClients.size());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("hasNextPage", hasNextPageForList(unassignedClients, startIndex, size));


        model.addAttribute("users", eligibleUsers);

        // *** MODIFICATION PRINCIPALE : Utiliser la même logique que ReportController ***
        List<Branche> availableBranches = getFilteredBranchesForCurrentUser(currentUser);
        model.addAttribute("branches", availableBranches);

        return "admin/unassigned-clients";
    }
    @GetMapping("/api/clients/count-by-branche")
    @ResponseBody
    public Map<String, Object> getClientCountByBranche(@RequestParam String branche) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());

        Map<String, Object> response = new HashMap<>();

        try {
            Branche brancheEnum = brancheRepository.findByCode(branche).orElse(null);
            if (brancheEnum == null) {
                response.put("success", false);
                response.put("error", "Branche invalide");
                return response;
            }
            String directionCode = null;

            // Déterminer le code de direction selon le type d'utilisateur
            if (userService.isSuperAdmin(currentUser)) {
                // Super admin peut voir tous les clients
                long count = clientService.findUnassignedClients().stream()
                        .filter(client -> brancheEnum.equals(client.getNMBRA()))
                        .count();
                response.put("count", count);
            } else if (userService.isDirectionAdmin(currentUser) && currentUser.getDirection() != null) {
                directionCode = currentUser.getDirection().getCode();
                long count = clientService.countUnassignedClientsByDirectionAndBranche(directionCode, brancheEnum);
                response.put("count", count);
            } else {
                response.put("count", 0);
            }

            response.put("success", true);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("error", "Branche invalide");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erreur lors du comptage");
        }

        return response;
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
                                    client.getNMBRA().getDisplayname());
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
    public String viewAdminAgenda(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        User currentUser = userRepository.findByEmail(userEmail);

        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);
        boolean isDirectionAdmin = userService.isDirectionAdmin(currentUser);

        // Récupérer TOUTES les données (logique existante conservée)
        List<Client> allClientsNonTraites;
        List<Client> allClientsAbsents;
        List<Client> allClientsContactes;
        List<Client> allClientsRefus;
        List<Client> allClientsInjoignables;
        List<Client> allClientsNumeroErrone;

        if (isSuperAdmin) {
            allClientsNonTraites = clientService.findByStatus(ClientStatus.NON_TRAITE);
            allClientsAbsents = clientService.findByStatus(ClientStatus.ABSENT);
            allClientsContactes = clientService.findByStatus(ClientStatus.CONTACTE);
            allClientsRefus = clientService.findByStatus(ClientStatus.REFUS);
            allClientsInjoignables = clientService.findByStatus(ClientStatus.INJOIGNABLE);
            allClientsNumeroErrone = clientService.findByStatus(ClientStatus.NUMERO_ERRONE);
        }
        else if (isDirectionAdmin && currentUser.getDirection() != null) {
            String directionCode = currentUser.getDirection().getCode();
            allClientsNonTraites = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.NON_TRAITE);
            allClientsAbsents = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.ABSENT);
            allClientsContactes = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.CONTACTE);
            allClientsRefus = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.REFUS);
            allClientsInjoignables = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.INJOIGNABLE);
            allClientsNumeroErrone = clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, ClientStatus.NUMERO_ERRONE);
        }
        else {
            allClientsNonTraites = filterClientsByRegion(currentUser, ClientStatus.NON_TRAITE, null, null);
            allClientsAbsents = filterClientsByRegion(currentUser, ClientStatus.ABSENT, null, null);
            allClientsContactes = filterClientsByRegion(currentUser, ClientStatus.CONTACTE, null, null);
            allClientsRefus = filterClientsByRegion(currentUser, ClientStatus.REFUS, null, null);
            allClientsInjoignables = filterClientsByRegion(currentUser, ClientStatus.INJOIGNABLE, null, null);
            allClientsNumeroErrone = filterClientsByRegion(currentUser, ClientStatus.NUMERO_ERRONE, null, null);
        }

        // PAGINATION : Calculer les sous-listes
        int startIndex = page * size;

        List<Client> clientsNonTraites = getPagedList(allClientsNonTraites, startIndex, size);
        List<Client> clientsAbsents = getPagedList(allClientsAbsents, startIndex, size);
        List<Client> clientsContactes = getPagedList(allClientsContactes, startIndex, size);
        List<Client> clientsRefus = getPagedList(allClientsRefus, startIndex, size);
        List<Client> clientsInjoignables = getPagedList(allClientsInjoignables, startIndex, size);
        List<Client> clientsNumeroErrone = getPagedList(allClientsNumeroErrone, startIndex, size);

        // Reste de la logique existante (conservée)
        List<Rappel> rappels;
        if (isSuperAdmin) {
            rappels = rappelService.getAllActiveRappels();
        } else if (isDirectionAdmin && currentUser.getDirection() != null) {
            String directionCode = currentUser.getDirection().getCode();
            rappels = rappelService.getAllActiveRappels().stream()
                    .filter(rappel -> {
                        Client client = rappel.getClient();
                        return client != null && directionCode.equals(client.getNMDIR());
                    })
                    .collect(Collectors.toList());
        } else {
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

        Map<Long, Rappel> clientRappelMap = new HashMap<>();
        for (Client client : clientsAbsents) {
            List<Rappel> clientRappels = rappels.stream()
                    .filter(rappel -> rappel.getClient().getId().equals(client.getId()))
                    .sorted((r1, r2) -> r2.getDateRappel().compareTo(r1.getDateRappel()))
                    .collect(Collectors.toList());
            if (!clientRappels.isEmpty()) {
                clientRappelMap.put(client.getId(), clientRappels.get(0));
            }
        }

        List<Rappel> todayRappels = rappelService.findByDate(LocalDate.now());
        Set<Long> todayRappelClientIds = todayRappels.stream()
                .map(rappel -> rappel.getClient().getId())
                .collect(Collectors.toSet());

        Map<String, Integer> stats = new HashMap<>();
        stats.put("nonTraites", allClientsNonTraites.size());
        stats.put("absents", allClientsAbsents.size());
        stats.put("contactes", allClientsContactes.size());
        stats.put("refus", allClientsRefus.size());
        stats.put("injoignables", allClientsInjoignables.size());
        stats.put("numerosErrones", allClientsNumeroErrone.size());

        // NOUVEAUX ATTRIBUTS pour pagination
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("hasNextPage", hasNextPageForAnyList(startIndex, size,
                allClientsNonTraites, allClientsAbsents, allClientsContactes,
                allClientsRefus, allClientsInjoignables, allClientsNumeroErrone));

        // Attributs existants
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

        List<UserDto> users = userService.findAllUsers();
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
    public String importClientsFromExcel(@RequestParam("file") MultipartFile file,
                                         RedirectAttributes redirectAttributes) {

        // Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName());

        // Vérifier si c'est un SuperAdmin
        if (userService.isSuperAdmin(currentUser)) {
            // Rediriger vers le contrôleur SuperAdmin
            redirectAttributes.addFlashAttribute("info",
                    "L'importation des clients doit être effectuée depuis l'interface SuperAdmin");
            return "redirect:/superadmin/import-clients";
        }

        // Bloquer l'importation pour les admins de direction
        redirectAttributes.addFlashAttribute("error",
                "L'importation des clients est désormais réservée aux super administrateurs. " +
                        "Contactez votre super administrateur pour importer de nouveaux clients.");

        return "redirect:/admin/unassigned-clients";
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
            brancheEnum = brancheRepository.findByCode(branche).orElse(null);
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

        String brancheInfo = brancheEnum != null ? " pour la branche " + brancheEnum.getDisplayname() : " pour toutes les agences";
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
        String brancheStr = brancheEnum != null ? "_" + brancheEnum.getCode() : "_TOUTES_AGENCES";
        String filename = "rendez_vous" +
                "_du_" + defaultStartDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "_au_" + defaultEndDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                brancheStr + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }
    private List<Branche> getFilteredBranchesForCurrentUser(User currentUser) {
        try {
            // Vérifier les rôles
            boolean isSuperAdmin = userService.isSuperAdmin(currentUser);
            boolean isDirectionAdmin = userService.isDirectionAdmin(currentUser);

            List<Branche> branches = new ArrayList<>();

            // Si c'est un super admin, récupérer toutes les branches
            if (isSuperAdmin) {
                branches = brancheRepository.findAll();
            }
            // Si c'est un admin de direction
            else if (isDirectionAdmin) {
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
                    List<Branche> allBranches = brancheRepository.findAll();
                    branches = allBranches.stream()
                            .filter(branche -> {
                                String brancheRegionCode = branche.getRegion().getCode();
                                return brancheRegionCode != null && regionCodes.contains(brancheRegionCode);
                            })
                            .collect(Collectors.toList());
                } else {
                    // Par sécurité, si l'admin n'a aucune région, on lui donne toutes les branches
                    branches = brancheRepository.findAll();
                }
            }

            System.out.println("Nombre de branches filtrées pour l'utilisateur: " + branches.size());
            return branches;

        } catch (Exception e) {
            System.err.println("Erreur lors du filtrage des branches: " + e.getMessage());
            e.printStackTrace();
            // En cas d'erreur, retourner toutes les branches pour éviter une page cassée
            return brancheRepository.findAll();
        }
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
                brancheEnum = brancheRepository.findByCode(branche).orElse(null);
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

            String brancheInfo = brancheEnum != null ? " pour la branche " + brancheEnum.getDisplayname() : " pour toutes les agences";

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
    @PostMapping("/clients/assign-by-branche")
    public String assignClientsByBranche(@RequestParam Long userId,
                                         @RequestParam String branche,
                                         @RequestParam int clientCount,
                                         RedirectAttributes redirectAttributes) {
        try {
            // DEBUG : Afficher ce qui est reçu
            System.out.println("DEBUG - Assignation par branche:");
            System.out.println("DEBUG - userId: " + userId);
            System.out.println("DEBUG - branche reçue: '" + branche + "'");
            System.out.println("DEBUG - clientCount: " + clientCount);

            // DEBUG : Afficher toutes les branches disponibles
            List<Branche> allBranches = brancheRepository.findAll();
            System.out.println("DEBUG - Branches disponibles dans DB:");
            for (Branche b : allBranches) {
                System.out.println("  - Code: '" + b.getCode() + "', Display: '" + b.getDisplayname() + "'");
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByEmail(auth.getName());

            // Validations de base
            if (clientCount <= 0 || clientCount > 1000) {
                redirectAttributes.addFlashAttribute("error",
                        "Le nombre de clients doit être entre 1 et 1000");
                return "redirect:/admin/unassigned-clients";
            }

            // Vérifier que l'utilisateur cible existe
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Conversion de la branche
            // Conversion de la branche
            Branche brancheEnum = brancheRepository.findByCode(branche).orElse(null);
            if (brancheEnum == null) {
                redirectAttributes.addFlashAttribute("error", "Branche invalide");
                return "redirect:/admin/unassigned-clients";
            }

            // Vérifications de sécurité selon le type d'utilisateur
            List<Client> availableClients;
            if (userService.isSuperAdmin(currentUser)) {
                // Super admin peut assigner tous les clients
                availableClients = clientService.findUnassignedClients().stream()
                        .filter(client -> brancheEnum.equals(client.getNMBRA()))
                        .limit(clientCount)
                        .collect(Collectors.toList());
            } else if (userService.isDirectionAdmin(currentUser) && currentUser.getDirection() != null) {
                // Vérifications pour directeur de division
                String directionCode = currentUser.getDirection().getCode();

                // Vérifier que l'utilisateur cible appartient à la même direction
                if (targetUser.getDirection() == null ||
                        !targetUser.getDirection().getId().equals(currentUser.getDirection().getId())) {
                    redirectAttributes.addFlashAttribute("error",
                            "Vous ne pouvez assigner des clients qu'aux utilisateurs de votre direction");
                    return "redirect:/admin/unassigned-clients";
                }

                // Vérifier la correspondance des branches
                if (targetUser.getAssignedBranche() != null &&
                        !targetUser.getAssignedBranche().equals(brancheEnum)) {
                    redirectAttributes.addFlashAttribute("error",
                            "L'agent sélectionné n'est pas assigné à la branche " + brancheEnum.getDisplayname());
                    return "redirect:/admin/unassigned-clients";
                }

                // Récupérer les clients disponibles
                availableClients = clientService.findUnassignedClientsByDirectionAndBranche(
                        directionCode, brancheEnum, clientCount);
            } else {
                redirectAttributes.addFlashAttribute("error", "Vous n'avez pas les droits nécessaires");
                return "redirect:/admin/unassigned-clients";
            }

            // Vérifier qu'il y a assez de clients disponibles
            if (availableClients.size() < clientCount) {
                redirectAttributes.addFlashAttribute("error",
                        "Seulement " + availableClients.size() + " client(s) disponible(s) pour la branche " +
                                brancheEnum.getDisplayname() + ". Demandé: " + clientCount);
                return "redirect:/admin/unassigned-clients";
            }

            // Effectuer l'assignation
            int assignedCount = 0;
            for (Client client : availableClients) {
                clientService.assignToUser(client.getId(), userId);
                assignedCount++;
            }

            // Audit de l'assignation par branche
            auditService.auditEvent(
                    AuditType.CLIENTS_BULK_ASSIGNED,
                    "Client",
                    null,
                    assignedCount + " clients de la branche " + brancheEnum.getDisplayname() +
                            " assignés à " + targetUser.getName() , currentUser.getEmail()
            );

            redirectAttributes.addFlashAttribute("success",
                    assignedCount + " client(s) de la branche " + brancheEnum.getDisplayname() +
                            " assigné(s) avec succès à " + targetUser.getName());

            return "redirect:/admin/unassigned-clients";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Erreur lors de l'assignation par branche: " + e.getMessage());
            return "redirect:/admin/unassigned-clients";
        }
    }

    private List<Client> filterClientsByCurrentUserScope(ClientStatus status, User currentUser) {
        // Si l'utilisateur est un directeur de division
        if (userService.isDirectionAdmin(currentUser) && currentUser.getDirection() != null) {
            String directionCode = currentUser.getDirection().getCode();
            System.out.println("DEBUG - Filtrage par direction: " + directionCode);

            // Utiliser directement la méthode du repository pour meilleure performance
            if (status != null) {
                return clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, status);
            } else {
                return clientRepository.findByNMDIROrderByUpdatedAtDesc(directionCode);
            }
        }
        // Si c'est un administrateur régional (ancienne méthode)
        else if (!currentUser.getRegions().isEmpty()) {
            // Récupérer les codes de régions
            List<String> regionCodes = currentUser.getRegions().stream()
                    .filter(region -> region != null && region.getCode() != null)
                    .map(Region::getCode)
                    .collect(Collectors.toList());

            System.out.println("DEBUG - Filtrage par régions: " + regionCodes);

            // Récupérer tous les clients du statut donné
            List<Client> allClients;
            if (status != null) {
                allClients = clientService.findByStatus(status);
            } else {
                allClients = clientService.findAll();
            }

            List<Client> filteredClients = new ArrayList<>();

            for (Client client : allClients) {
                // Vérifier si le client appartient à une des régions de l'admin
                boolean regionMatch = false;
                String clientRegion = client.getNMREG();

                if (clientRegion != null) {
                    // Nettoyer et normaliser la région du client
                    String normalizedClientRegion = clientRegion.toUpperCase().replace(" ", "");

                    // Vérifier si la région du client correspond à une des régions de l'admin
                    for (String regionCode : regionCodes) {
                        String normalizedRegionCode = regionCode.toUpperCase().replace("_", "");

                        // Correspondance exacte ou partielle
                        if (normalizedClientRegion.contains(normalizedRegionCode) ||
                                normalizedRegionCode.contains(normalizedClientRegion)) {
                            regionMatch = true;
                            break;
                        }

                        // Gestion des cas spéciaux (variations d'orthographe)
                        if ((normalizedClientRegion.contains("SIDI") && normalizedRegionCode.contains("SEDY")) ||
                                (normalizedClientRegion.contains("SEDY") && normalizedRegionCode.contains("SIDI")) ||
                                (normalizedClientRegion.contains("FIDAA") && normalizedRegionCode.contains("FIDA")) ||
                                (normalizedClientRegion.contains("FIDA") && normalizedRegionCode.contains("FIDAA"))) {
                            regionMatch = true;
                            break;
                        }
                    }
                }

                if (regionMatch) {
                    filteredClients.add(client);
                }
            }

            return filteredClients;
        }

        // Si aucune condition n'est remplie, retourner une liste vide
        return new ArrayList<>();
    }

    private List<Client> getPagedList(List<Client> fullList, int startIndex, int size) {
        if (fullList == null || fullList.isEmpty()) {
            return new ArrayList<>();
        }
        int endIndex = Math.min(startIndex + size, fullList.size());
        if (startIndex >= fullList.size()) {
            return new ArrayList<>();
        }
        return fullList.subList(startIndex, endIndex);
    }

    // Méthode pour vérifier s'il y a une page suivante pour UNE liste
    private boolean hasNextPageForList(List<Client> list, int startIndex, int size) {
        return list != null && (startIndex + size) < list.size();
    }

    // Méthode pour vérifier s'il y a une page suivante pour PLUSIEURS listes
    private boolean hasNextPageForAnyList(int startIndex, int size, List<Client>... lists) {
        for (List<Client> list : lists) {
            if (list != null && (startIndex + size) < list.size()) {
                return true;
            }
        }
        return false;
    }
}