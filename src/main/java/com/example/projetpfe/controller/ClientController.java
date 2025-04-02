package com.example.projetpfe.controller;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.RappelService;
import com.example.projetpfe.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final RappelService rappelService;
    private final UserService userService;

    @Autowired
    public ClientController(UserService userService,ClientService clientService, RappelService rappelService) {
        this.clientService = clientService;
        this.rappelService = rappelService;
        this.userService = userService;
    }

    // Mise à jour de la méthode viewClient dans ClientController.java pour ajouter les utilisateurs
    @GetMapping("/{id}")
    public String viewClient(@PathVariable Long id, Model model) {
        Client client = clientService.getById(id);
        model.addAttribute("client", client);

        // Si l'utilisateur est un administrateur, ajouter la liste des utilisateurs pour l'assignation
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            List<UserDto> users = userService.findAllUsers();
            model.addAttribute("users", users);
        }

        return "clients/detail";
    }

    @GetMapping("/{id}/edit")
    public String editClient(@PathVariable Long id, Model model) {
        Client client = clientService.getById(id);
        ClientDto clientDto = clientService.convertToDto(client);

        model.addAttribute("client", client);
        model.addAttribute("clientDto", clientDto);
        model.addAttribute("statuses", ClientStatus.values());
        model.addAttribute("raisonsNonRenouvellement", RaisonNonRenouvellement.values());
        model.addAttribute("qualitesService", QualiteService.values());
        model.addAttribute("activitesClient", ActiviteClient.values());
        model.addAttribute("interetsCredit", InteretCredit.values());
        model.addAttribute("profil", Profil.values());
        model.addAttribute("branche", Branche.values());
        model.addAttribute("facteursInfluence", FacteurInfluence.values());
        model.addAttribute("isEditMode", true);

        return "clients/edit";
    }

    @PostMapping("/{id}/edit")
    public String saveEditedClient(@PathVariable Long id,
                                   @Valid @ModelAttribute("clientDto") ClientDto clientDto,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("client", clientService.getById(id));
            model.addAttribute("statuses", ClientStatus.values());
            model.addAttribute("raisonsNonRenouvellement", RaisonNonRenouvellement.values());
            model.addAttribute("qualitesService", QualiteService.values());
            model.addAttribute("activitesClient", ActiviteClient.values());
            model.addAttribute("interetsCredit", InteretCredit.values());
            model.addAttribute("profil", Profil.values());
            model.addAttribute("branche", Branche.values());
            model.addAttribute("facteursInfluence", FacteurInfluence.values());
            model.addAttribute("isEditMode", true);
            return "clients/edit";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        try {
            clientService.updateClientAndQuestionnaire(id, clientDto, userEmail);
            redirectAttributes.addFlashAttribute("success", "Client modifié avec succès");
            return "redirect:/agenda/index";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la modification: " + e.getMessage());
            return "redirect:/clients/" + id + "/edit";
        }
    }

    @GetMapping("/{id}/status")
    public String updateStatusForm(@PathVariable Long id, Model model) {
        Client client = clientService.getById(id);
        model.addAttribute("client", client);
        model.addAttribute("statuses", ClientStatus.values());
        return "clients/update-status";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam ClientStatus status,
                               @RequestParam(required = false) String notes,
                               @RequestParam(required = false) LocalDateTime rappelDate,
                               RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        try {
            clientService.updateStatus(id, status, userEmail);

            if (status == ClientStatus.ABSENT && rappelDate != null) {
                rappelService.createRappel(id, rappelDate, notes, userEmail);
                redirectAttributes.addFlashAttribute("success", "Statut mis à jour et rappel programmé");
                return "redirect:/agenda/index";
            } else if (status == ClientStatus.CONTACTE) {
                redirectAttributes.addFlashAttribute("success", "Statut mis à jour");
                return "redirect:/clients/" + id + "/questionnaire";
            } else if (status == ClientStatus.REFUS) {
                redirectAttributes.addFlashAttribute("success", "Client marqué comme refus");
                return "redirect:/agenda/index";
            } else {
                redirectAttributes.addFlashAttribute("success", "Statut mis à jour");
                return "redirect:/agenda/index";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/clients/" + id + "/status";
        }
    }

    @GetMapping("/{id}/questionnaire")
    public String showQuestionnaire(@PathVariable Long id, Model model) {
        Client client = clientService.getById(id);
        ClientDto clientDto = clientService.convertToDto(client);

        model.addAttribute("client", client);
        model.addAttribute("clientDto", clientDto);
        model.addAttribute("raisonsNonRenouvellement", RaisonNonRenouvellement.values());
        model.addAttribute("qualitesService", QualiteService.values());
        model.addAttribute("activitesClient", ActiviteClient.values());
        model.addAttribute("profil", Profil.values());
        model.addAttribute("branche", Branche.values());
        model.addAttribute("interetsCredit", InteretCredit.values());
        model.addAttribute("facteursInfluence", FacteurInfluence.values());

        return "clients/questionnaire";
    }

    @PostMapping("/{id}/questionnaire")
    public String saveQuestionnaire(@PathVariable Long id,
                                    @Valid @ModelAttribute("clientDto") ClientDto clientDto,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        Client client = clientService.getById(id);
        // Capture toutes les exceptions pour les afficher
        try {
            if (bindingResult.hasErrors()) {
                model.addAttribute("client", client);
                model.addAttribute("raisonsNonRenouvellement", RaisonNonRenouvellement.values());
                model.addAttribute("qualitesService", QualiteService.values());
                model.addAttribute("activitesClient", ActiviteClient.values());
                model.addAttribute("interetsCredit", InteretCredit.values());
                model.addAttribute("profil", Profil.values());
                model.addAttribute("branche", Branche.values());
                model.addAttribute("facteursInfluence", FacteurInfluence.values());
                return "clients/questionnaire";
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();

            clientService.saveQuestionnaire(id, clientDto, userEmail);
            redirectAttributes.addFlashAttribute("success", "Questionnaire sauvegardé avec succès");
            return "redirect:/agenda/index";
        } catch (Exception e) {
            e.printStackTrace(); // Pour voir l'erreur dans les logs
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la sauvegarde: " + e.getMessage());
            model.addAttribute("client", clientService.getById(id));
            model.addAttribute("raisonsNonRenouvellement", RaisonNonRenouvellement.values());
            model.addAttribute("qualitesService", QualiteService.values());
            model.addAttribute("activitesClient", ActiviteClient.values());
            model.addAttribute("interetsCredit", InteretCredit.values());
            model.addAttribute("profil", Profil.values());
            model.addAttribute("branche", Branche.values());
            model.addAttribute("facteursInfluence", FacteurInfluence.values());
            return "clients/questionnaire";
        }
    }

    @GetMapping("/create")
    public String createClientForm(Model model) {
        model.addAttribute("clientDto", new ClientDto());
        return "clients/create";
    }

    @PostMapping("/create")
    public String createClient(@Valid @ModelAttribute("clientDto") ClientDto clientDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "clients/create";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        try {
            clientService.create(clientDto, userEmail);
            redirectAttributes.addFlashAttribute("success", "Client créé avec succès");
            return "redirect:/agenda";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
            return "redirect:/clients/create";
        }
    }
}