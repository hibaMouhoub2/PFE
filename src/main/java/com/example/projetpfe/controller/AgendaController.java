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

        // Rappels à effectuer
        List<Rappel> rappels = rappelService.getRappelsForUser(currentUser);

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

        // Ajouter le nombre de rappels pour aujourd'hui
        stats.put("todayRappelsCount", todayRappels.size());

        model.addAttribute("clientsNonTraites", clientsNonTraites);
        model.addAttribute("clientsAbsents", clientsAbsents);
        model.addAttribute("rappels", rappels);
        model.addAttribute("todayRappels", todayRappels);
        model.addAttribute("todayRappelClientIds", todayRappelClientIds);
        model.addAttribute("stats", stats);

        return "agenda/index";
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