package com.example.projetpfe.controller;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.Branche;
import com.example.projetpfe.entity.Client;
import com.example.projetpfe.entity.ClientStatus;
import com.example.projetpfe.repository.BrancheRepository;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.AuditService;
import com.example.projetpfe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/superadmin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private BrancheRepository brancheRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientRepository clientRepository;




    @PostMapping("/clients/import")
    public String importClientsFromExcel(@RequestParam("file") MultipartFile file,
                                         RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner un fichier à importer");
            return "redirect:/superadmin/clients";
        }

        try {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();


            ClientService.ImportResult result = clientService.importClientsFromExcelBySuperAdmin(file, userEmail);

            StringBuilder message = new StringBuilder();
            message.append(result.getImportedCount()).append(" client(s) importé(s) avec succès");

            if (result.getUpdatedCount() > 0) {
                message.append(", ").append(result.getUpdatedCount())
                        .append(" client(s) mis à jour avec une date de fin de contrat plus récente");
            }

            if (result.getSkippedCount() > 0) {
                message.append(". ").append(result.getSkippedCount())
                        .append(" client(s) ignoré(s) car leur CIN existe déjà et la date de fin n'est pas plus récente");
            }

            redirectAttributes.addFlashAttribute("success", message.toString());


            return "redirect:/superadmin/clients";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'importation: " + e.getMessage());
            return "redirect:/superadmin/clients";
        }
    }
    private List<String> getUniqueDirections() {
        return clientService.getUniqueDirections();
    }


    @GetMapping("/clients")
    public String listAllClients(
            @RequestParam(required = false) String q,           // Ajout des paramètres manquants
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,        // Ajout pour filtrage par utilisateur
            @RequestParam(required = false) String branche,     // Ajout pour filtrage par branche
            @RequestParam(required = false) String direction,   // Garder le paramètre existant
            Model model) {


        ClientStatus statusEnum = null;
        try {
            if (status != null && !status.isEmpty()) {
                statusEnum = ClientStatus.valueOf(status);
                System.out.println("DEBUG SuperAdmin - Statut: " + statusEnum);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("DEBUG SuperAdmin - Statut invalide: " + status);
        }


        Branche brancheEnum = null;
        try {
            if (branche != null && !branche.isEmpty()) {
                brancheEnum = brancheRepository.findByCode(branche).orElse(null);
                System.out.println("DEBUG SuperAdmin - Branche: " + brancheEnum);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("DEBUG SuperAdmin - Branche invalide: " + branche);
        }

        List<Client> clients;

        // *** UTILISER LA MÊME REQUÊTE QUE AdminController ***
        System.out.println("DEBUG SuperAdmin - Mode super admin: récupération avec findByFiltersWithBranche");
        clients = clientRepository.findByFiltersWithBranche(q, statusEnum, userId, brancheEnum);

        // Filtre par direction si spécifié (pour maintenir la compatibilité avec l'ancien code)
        if (direction != null && !direction.isEmpty()) {
            final String finalDirection = direction;
            clients = clients.stream()
                    .filter(client -> finalDirection.equals(client.getNMDIR()))
                    .collect(Collectors.toList());
        }

        // *** AJOUTER LES ATTRIBUTS POUR LES FILTRES (manquants dans l'ancien code) ***
        model.addAttribute("statuses", ClientStatus.values());

        // Pour le filtre utilisateur - récupérer tous les utilisateurs
        List<UserDto> users = userService.findAllUsers();
        model.addAttribute("users", users);

        // Pour le filtre branche - récupérer toutes les branches
        List<Branche> branches = brancheRepository.findAll();
        model.addAttribute("branches", branches);

        // Données pour les filtres existants
        model.addAttribute("directions", getUniqueDirections());

        // Données pour la liste
        model.addAttribute("clients", clients);
        model.addAttribute("selectedDirection", direction != null ? direction : "");
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("selectedBranche", branche != null ? branche : "");
        model.addAttribute("searchQuery", q != null ? q : "");
        model.addAttribute("clientCount", clients.size());

        // Statistiques pour l'import
        model.addAttribute("totalClients", clientService.getTotalClientsCount());
        model.addAttribute("unassignedClients", clientService.getUnassignedClientsCount());

        return "superadmin/clients";
    }





}