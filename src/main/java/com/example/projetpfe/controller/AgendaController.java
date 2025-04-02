package com.example.projetpfe.controller;

import com.example.projetpfe.entity.Client;
import com.example.projetpfe.entity.ClientStatus;
import com.example.projetpfe.entity.Rappel;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.Impl.ClientService;
import com.example.projetpfe.service.Impl.RappelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    private final ClientService clientService;
    private final RappelService rappelService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public AgendaController(ClientService clientService,
                            RappelService rappelService,
                            UserRepository userRepository,
                            ClientRepository clientRepository) {
        this.clientService = clientService;
        this.rappelService = rappelService;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    @GetMapping("/index")
    public String viewAgenda(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        User currentUser = userRepository.findByEmail(userEmail);

        // Clients à traiter
        List<Client> clientsNonTraites = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.NON_TRAITE);

        // Clients absents (à rappeler)
        List<Client> clientsAbsents = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.ABSENT);

        List<Client> clientsContactes = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.CONTACTE);

        List<Client> clientsRefus = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.REFUS);

        // Rappels à effectuer
        List<Rappel> rappels = rappelService.getRappelsForUser(currentUser);

        // Créer une map client_id -> dernier rappel programmé
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
        List<Rappel> todayRappels = rappelService.findTodayRappelsForUser(currentUser);

        // Initialiser le Set vide (jamais null)
        Set<Long> todayRappelClientIds = new HashSet<>();

        // Ajouter les IDs des clients qui ont des rappels aujourd'hui
        if (!todayRappels.isEmpty()) {
            todayRappelClientIds = todayRappels.stream()
                    .map(rappel -> rappel.getClient().getId())
                    .collect(Collectors.toSet());
        }

        // Statistiques
        Map<String, Object> stats = new HashMap<>();
        stats.put("nonTraites", clientRepository.countByAssignedUserAndStatus(currentUser, ClientStatus.NON_TRAITE));
        stats.put("absents", clientRepository.countByAssignedUserAndStatus(currentUser, ClientStatus.ABSENT));
        stats.put("contactes", clientRepository.countByAssignedUserAndStatus(currentUser, ClientStatus.CONTACTE));
        stats.put("refus", clientRepository.countByAssignedUserAndStatus(currentUser, ClientStatus.REFUS));
        stats.put("todayRappelsCount", todayRappels.size());
        // Trier les clients absents par date de rappel (les plus proches d'abord)
        clientsAbsents.sort((c1, c2) -> {
            Rappel r1 = clientRappelMap.get(c1.getId());
            Rappel r2 = clientRappelMap.get(c2.getId());

            if (r1 == null && r2 == null) return 0;
            if (r1 == null) return 1;  // Clients sans rappel en dernier
            if (r2 == null) return -1; // Clients sans rappel en dernier

            return r1.getDateRappel().compareTo(r2.getDateRappel());
        });

        model.addAttribute("clientsNonTraites", clientsNonTraites);
        model.addAttribute("clientsAbsents", clientsAbsents);
        model.addAttribute("clientsContactes", clientsContactes);
        model.addAttribute("clientsRefus", clientsRefus);
        model.addAttribute("rappels", rappels);
        model.addAttribute("todayRappels", todayRappels);
        model.addAttribute("todayRappelClientIds", todayRappelClientIds);
        model.addAttribute("clientRappelMap", clientRappelMap);
        model.addAttribute("stats", stats);

        return "agenda/index";
    }

    @GetMapping("/search-results")
    public String showAgendaSearchResults(
            @RequestParam(required = false) String query,
            Model model,
            Authentication authentication) {

        // Récupérer les attributs flash d'abord (pour les redirections depuis /clients/search)
        @SuppressWarnings("unchecked")
        List<Long> flashClientIds = (List<Long>) model.asMap().get("searchResults");
        String flashSearchQuery = (String) model.asMap().get("searchQuery");

        // Variables qui contiendront le résultat final
        List<Client> resultClients;
        String resultQuery;

        // Si on a des résultats en flash (redirection), les utiliser
        if (flashClientIds != null && !flashClientIds.isEmpty()) {
            resultClients = flashClientIds.stream()
                    .map(id -> clientService.getById(id))
                    .collect(Collectors.toList());
            resultQuery = flashSearchQuery;
        }
        // Sinon, si on a un paramètre de requête, effectuer une nouvelle recherche
        else if (query != null && !query.isEmpty()) {
            // Effectuer une recherche par CIN ou téléphone
            resultClients = clientService.findByCinOrPhone(query);
            resultQuery = query;
        }
        // Si aucune des deux conditions n'est remplie, afficher juste la page vide
        else {
            model.addAttribute("searchResults", List.of());
            model.addAttribute("searchQuery", "");
            return "agenda/search-results";
        }

        // Filtrer pour ne montrer que les clients assignés à l'utilisateur courant
        String userEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(userEmail);

        List<Client> assignedClients = resultClients.stream()
                .filter(client -> client.getAssignedUser() != null &&
                        client.getAssignedUser().getId().equals(currentUser.getId()))
                .collect(Collectors.toList());

        model.addAttribute("searchResults", assignedClients);
        model.addAttribute("searchQuery", resultQuery);

        return "agenda/search-results";
    }
    @GetMapping("/history")
    public String viewHistory(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        User currentUser = userRepository.findByEmail(userEmail);

        List<Client> clientsContactes = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.CONTACTE);
        List<Client> clientsRefus = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.REFUS);

        model.addAttribute("clientsContactes", clientsContactes);
        model.addAttribute("clientsRefus", clientsRefus);

        return "agenda/history";
    }
}