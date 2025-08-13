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
    public String viewAgenda(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "50") int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        User currentUser = userRepository.findByEmail(userEmail);

        // Récupérer TOUTES les données (logique existante conservée)
        List<Client> allClientsNonTraites = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.NON_TRAITE);
        List<Client> allClientsAbsents = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.ABSENT);
        List<Client> allClientsContactes = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.CONTACTE);
        List<Client> allClientsRefus = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.REFUS);
        List<Client> allClientsInjoignables = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.INJOIGNABLE);
        List<Client> allClientsNumeroErrone = clientService.findClientsByUserAndStatus(currentUser, ClientStatus.NUMERO_ERRONE);

        // PAGINATION : Calculer les sous-listes
        int startIndex = page * size;

        List<Client> clientsNonTraites = getPagedList(allClientsNonTraites, startIndex, size);
        List<Client> clientsAbsents = getPagedList(allClientsAbsents, startIndex, size);
        List<Client> clientsContactes = getPagedList(allClientsContactes, startIndex, size);
        List<Client> clientsRefus = getPagedList(allClientsRefus, startIndex, size);
        List<Client> clientsInjoignables = getPagedList(allClientsInjoignables, startIndex, size);
        List<Client> clientsNumeroErrone = getPagedList(allClientsNumeroErrone, startIndex, size);

        // Rappels (logique existante conservée)
        List<Rappel> rappels = rappelService.getRappelsForUser(currentUser);

        // Map client-rappel (logique existante conservée)
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

        List<Rappel> todayRappels = rappelService.findTodayRappelsForUser(currentUser);
        Set<Long> todayRappelClientIds = todayRappels.stream()
                .map(rappel -> rappel.getClient().getId())
                .collect(Collectors.toSet());

        // Statistiques avec totaux réels
        Map<String, Integer> stats = new HashMap<>();
        stats.put("nonTraites", allClientsNonTraites.size());
        stats.put("absents", allClientsAbsents.size());
        stats.put("contactes", allClientsContactes.size());
        stats.put("refus", allClientsRefus.size());
        stats.put("injoignables", allClientsInjoignables.size());
        stats.put("numerosErrones", allClientsNumeroErrone.size());
        stats.put("rappels", rappels.size());

        // Tri (logique existante conservée)
        clientsAbsents.sort((c1, c2) -> {
            Rappel r1 = clientRappelMap.get(c1.getId());
            Rappel r2 = clientRappelMap.get(c2.getId());
            if (r1 == null && r2 == null) return 0;
            if (r1 == null) return 1;
            if (r2 == null) return -1;
            return r1.getDateRappel().compareTo(r2.getDateRappel());
        });

        // NOUVEAUX ATTRIBUTS pour pagination
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("hasNextPage", hasNextPageForAnyList(startIndex, size,
                allClientsNonTraites, allClientsAbsents, allClientsContactes,
                allClientsRefus, allClientsInjoignables, allClientsNumeroErrone));

        // Attributs existants (conservés)
        model.addAttribute("clientsNonTraites", clientsNonTraites);
        model.addAttribute("clientsAbsents", clientsAbsents);
        model.addAttribute("clientsContactes", clientsContactes);
        model.addAttribute("clientsRefus", clientsRefus);
        model.addAttribute("clientsInjoignables", clientsInjoignables);
        model.addAttribute("clientsNumeroErrone", clientsNumeroErrone);
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


        List<Long> flashClientIds = (List<Long>) model.asMap().get("searchResults");
        String flashSearchQuery = (String) model.asMap().get("searchQuery");

        // Variables qui contiendront le résultat final
        List<Client> resultClients;
        String resultQuery;

        // Si on a des résultats en flash les utiliser
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