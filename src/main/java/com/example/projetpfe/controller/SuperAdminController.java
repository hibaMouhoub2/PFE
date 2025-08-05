package com.example.projetpfe.controller;

import com.example.projetpfe.entity.Client;
import com.example.projetpfe.entity.ClientStatus;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.AuditService;
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





    @PostMapping("/clients/import")
    public String importClientsFromExcel(@RequestParam("file") MultipartFile file,
                                         RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner un fichier à importer");
            return "/superadmin/clients";
        }

        try {
            // Récupérer l'email de l'utilisateur connecté (SuperAdmin)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();

            // Importer les clients (logique simplifiée pour SuperAdmin - pas de filtres par direction)
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

            // Rediriger vers une page de confirmation ou dashboard SuperAdmin
            return "/superadmin/clients";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'importation: " + e.getMessage());
            return "/superadmin/clients";
        }
    }
    private List<String> getUniqueDirections() {
        return clientService.getUniqueDirections();
    }

    @GetMapping("/clients")
    public String listAllClients(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String status,
            Model model) {

        // Récupérer TOUS les clients (SuperAdmin voit tout)
        List<Client> clients = clientService.findAllClients();

        // Filtre par direction si sélectionnée
        if (direction != null && !direction.isEmpty()) {
            clients = clients.stream()
                    .filter(client -> direction.equals(client.getNMDIR()))
                    .collect(Collectors.toList());
        }

        // Filtre par statut si sélectionné
        if (status != null && !status.isEmpty()) {
            ClientStatus clientStatus = ClientStatus.valueOf(status);
            clients = clients.stream()
                    .filter(client -> clientStatus.equals(client.getStatus()))
                    .collect(Collectors.toList());
        }

        // Données pour les filtres
        model.addAttribute("directions", getUniqueDirections()); // Toutes les directions
        model.addAttribute("statuses", ClientStatus.values());

        // Données pour la liste
        model.addAttribute("clients", clients);
        model.addAttribute("selectedDirection", direction != null ? direction : "");
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("clientCount", clients.size());

        // Statistiques pour l'import
        model.addAttribute("totalClients", clientService.getTotalClientsCount());
        model.addAttribute("unassignedClients", clientService.getUnassignedClientsCount());

        return "/superadmin/clients";
    }


}