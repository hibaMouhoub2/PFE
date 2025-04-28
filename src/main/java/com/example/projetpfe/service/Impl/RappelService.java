package com.example.projetpfe.service.Impl;

import com.example.projetpfe.dto.RappelDto;
import java.time.format.DateTimeFormatter;
import com.example.projetpfe.entity.AuditType;
import com.example.projetpfe.entity.Client;
import com.example.projetpfe.entity.Rappel;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.RappelRepository;
import com.example.projetpfe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class RappelService {

    private final RappelRepository rappelRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    @Autowired
    private AuditService auditService;

    @Autowired
    public RappelService(RappelRepository rappelRepository,
                         ClientRepository clientRepository,
                         UserRepository userRepository) {
        this.rappelRepository = rappelRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Rappel createRappel(Long clientId, LocalDateTime dateRappel, String notes, String userEmail) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        User user = userRepository.findByEmail(userEmail);

        Rappel rappel = new Rappel();
        rappel.setClient(client);
        rappel.setDateRappel(dateRappel);
        rappel.setNotes(notes);
        rappel.setCreatedBy(user);
        rappel.setCompleted(false);

        Rappel savedRappel = rappelRepository.save(rappel);

        // Audit de la création du rappel
        auditService.auditEvent(AuditType.RAPPEL_CREATED,
                "Rappel",
                savedRappel.getId(),
                "Rappel créé pour le client " + client.getNom() + " " + client.getPrenom() +
                        " prévu le " + dateRappel.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                userEmail);

        return savedRappel;
    }

    public Rappel getById(Long id) {
        return rappelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rappel non trouvé avec l'ID: " + id));
    }

    public List<Rappel> getRappelsForUser(User user) {
        return rappelRepository.findByClientAssignedUserAndCompletedFalseOrderByDateRappel(user);
    }

    public List<Rappel> getAllActiveRappelsByRegions(List<String> regionCodes) {
        return rappelRepository.findByCompletedFalseAndClientNMREGInOrderByDateRappel(regionCodes);
    }

    @Transactional
    public Rappel completeRappel(Long rappelId) {
        Rappel rappel = rappelRepository.findById(rappelId)
                .orElseThrow(() -> new RuntimeException("Rappel non trouvé"));

        rappel.setCompleted(true);
        Rappel savedRappel = rappelRepository.save(rappel);

        // Récupérer l'utilisateur pour l'audit
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        // Audit du rappel complété
        auditService.auditEvent(AuditType.RAPPEL_COMPLETED,
                "Rappel",
                savedRappel.getId(),
                "Rappel marqué comme terminé pour le client " + rappel.getClient().getNom() + " " +
                        rappel.getClient().getPrenom(),
                userEmail);

        return savedRappel;
    }

    public long countTodayRappels(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return rappelRepository.countByClientAssignedUserAndDateRappelBetweenAndCompletedFalse(
                user, startOfDay, endOfDay);
    }

    public List<Rappel> findTodayRappelsForUser(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return rappelRepository.findByClientAssignedUserAndDateRappelBetweenAndCompletedFalseOrderByDateRappel(
                user, startOfDay, endOfDay);
    }

    public List<Rappel> findByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return rappelRepository.findByDateRappelBetweenAndCompletedFalse(start, end);
    }
    public List<Rappel> getAllActiveRappels() {
        return rappelRepository.findByCompletedFalseOrderByDateRappel();
    }
}