package com.example.projetpfe.controller;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.RappelRepository;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.RappelService;
import com.example.projetpfe.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final RappelService rappelService;
    private final UserService userService;
    @Autowired
    private RappelRepository rappelRepository;

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

        System.out.println("DEBUG: Méthode saveEditedClient appelée");
        System.out.println("DEBUG: ID client = " + id);
        System.out.println("DEBUG: ClientDto reçu = " + clientDto.toString());
        System.out.println("DEBUG: Erreurs de validation = " + bindingResult.hasErrors());

        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> {
                System.out.println("DEBUG: Erreur: " + error.toString());
            });

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
        System.out.println("DEBUG: Email utilisateur = " + userEmail);

        try {
            System.out.println("DEBUG: Avant appel à updateClientAndQuestionnaire");
            Client updatedClient = clientService.updateClientAndQuestionnaire(id, clientDto, userEmail);
            System.out.println("DEBUG: Après appel à updateClientAndQuestionnaire, client ID = " + updatedClient.getId());
            redirectAttributes.addFlashAttribute("success", "Client modifié avec succès");
            return "redirect:/agenda/index";
        } catch (Exception e) {
            System.out.println("DEBUG: Exception lors de la mise à jour: " + e.getMessage());
            e.printStackTrace();
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

    // src/main/java/com/example/projetpfe/controller/ClientController.java
    @PostMapping("/{id}/status")
    @Transactional
    public String updateStatus(@PathVariable Long id,
                               @RequestParam ClientStatus status,
                               @RequestParam(required = false) String notes,
                               @RequestParam(required = false) LocalDateTime rappelDate,
                               RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        try {

            rappelRepository.completeAllRappelsForClient(id);
            clientService.updateStatus(id, status, notes, userEmail);

            // Programmer un rappel uniquement pour le statut ABSENT
            if (status == ClientStatus.ABSENT && rappelDate != null) {
                rappelService.createRappel(id, rappelDate, notes, userEmail);
                redirectAttributes.addFlashAttribute("success", "Statut mis à jour et rappel programmé");
            } else if (status == ClientStatus.CONTACTE) {
                return "redirect:/clients/" + id + "/questionnaire";
            } else {
                String statusMessage = status == ClientStatus.REFUS ? "refus" :
                        (status == ClientStatus.INJOIGNABLE ? "injoignable" :
                                (status == ClientStatus.NUMERO_ERRONE ? "numéro erroné" : "mis à jour"));
                redirectAttributes.addFlashAttribute("success", "Client marqué comme " + statusMessage);
            }
            return "redirect:/agenda/index";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/clients/" + id + "/status";
        }
    }

    @GetMapping("/search")
    public String searchClient(@RequestParam String query, Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        // Rechercher par CIN ou téléphone
        List<Client> clients = clientService.findByCinOrPhone(query);

        if (clients.isEmpty()) {
            redirectAttributes.addFlashAttribute("info", "Aucun client trouvé pour: " + query);
            return "redirect:/agenda/index";
        }

        // Pour tous les cas (un seul ou plusieurs résultats),
        // on redirige vers la page de résultats avec les IDs
        List<Long> clientIds = clients.stream().map(Client::getId).collect(Collectors.toList());
        redirectAttributes.addFlashAttribute("searchResults", clientIds);
        redirectAttributes.addFlashAttribute("searchQuery", query);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return "redirect:/admin/search-results";
        } else {
            return "redirect:/agenda/search-results";
        }
    }

    @Autowired
    private HttpSession session;

    @GetMapping("/{id}/details-from-questionnaire")
    public String viewClientFromQuestionnaire(@PathVariable Long id,
                                              @ModelAttribute("clientDto") ClientDto clientDto) {
        // Stocker directement l'objet DTO dans la session
        session.setAttribute("questionnaire_" + id, clientDto);

        // Rediriger vers la page de détails
        return "redirect:/clients/" + id;
    }

    @GetMapping("/{id}/questionnaire")
    public String showQuestionnaire(@PathVariable Long id, Model model) {
        Client client = clientService.getById(id);
        ClientDto clientDto ;
        ClientDto sessionDto = (ClientDto) session.getAttribute("questionnaire_" + id);
        if(sessionDto != null) {
            clientDto = sessionDto;
            clientDto.setId(client.getId());
            // Ces champs ne doivent pas être modifiés même s'ils sont dans la session
            clientDto.setNom(client.getNom());
            clientDto.setPrenom(client.getPrenom());
            clientDto.setTelephone(client.getTelephone());
            clientDto.setTelephone2(client.getTelephone2());
            clientDto.setCin(client.getCin());
            clientDto.setStatus(client.getStatus());
            clientDto.setAssignedUserId(client.getAssignedUser() != null ? client.getAssignedUser().getId() : null);
        }else {
            // Si pas de données en session, utiliser les données du client
            clientDto = clientService.convertToDto(client);
        }

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

        try {
            // Prétraitement des données conditionnelles
            if (Boolean.FALSE.equals(clientDto.getRendezVousAgence())) {
                clientDto.setNMBRA(null);
                clientDto.setDateHeureRendezVous(null);
            }

            // Validation supplémentaire pour le rendez-vous
            if (Boolean.TRUE.equals(clientDto.getRendezVousAgence())) {
                if (clientDto.getNMBRA() == null) {
                    bindingResult.rejectValue("NMBRA", "error.clientDto", "L'agence est requise pour un rendez-vous");
                }
                if (clientDto.getDateHeureRendezVous() == null) {
                    bindingResult.rejectValue("dateHeureRendezVous", "error.clientDto", "La date de rendez-vous est requise");
                }
            }

            // Vérification des erreurs après validation
            if (bindingResult.hasErrors()) {
                model.addAttribute("client", client);
                model.addAttribute("statuses", ClientStatus.values()); // Ajout des statuts
                model.addAttribute("raisonsNonRenouvellement", RaisonNonRenouvellement.values());
                model.addAttribute("qualitesService", QualiteService.values());
                model.addAttribute("activitesClient", ActiviteClient.values());
                model.addAttribute("interetsCredit", InteretCredit.values());
                model.addAttribute("profil", Profil.values());
                model.addAttribute("branche", Branche.values());
                model.addAttribute("facteursInfluence", FacteurInfluence.values());
                return "clients/questionnaire";
            }

            // Si pas d'erreurs, on sauvegarde
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();

            clientService.saveQuestionnaire(id, clientDto, userEmail);

            // Nettoyage de la session
            session.removeAttribute("questionnaire_" + id);

            redirectAttributes.addFlashAttribute("success", "Questionnaire sauvegardé avec succès");
            return "redirect:/agenda/index";
        } catch (Exception e) {
            // Gestion des erreurs
            e.printStackTrace();
            String errorMessage = "Erreur lors de la sauvegarde: ";

            if (e instanceof NullPointerException) {
                errorMessage += "Une valeur requise est manquante";
            } else if (e instanceof IllegalArgumentException) {
                errorMessage += "Valeur incorrecte: " + e.getMessage();
            } else {
                errorMessage += e.getMessage();
            }

            redirectAttributes.addFlashAttribute("error", errorMessage);

            // Préparation des données pour le formulaire
            model.addAttribute("client", client);
            model.addAttribute("statuses", ClientStatus.values()); // Ajout des statuts
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