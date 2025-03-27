package com.example.projetpfe.service.Impl;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.entity.Client;
import com.example.projetpfe.entity.ClientStatus;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.RappelRepository;
import com.example.projetpfe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService {
    @Autowired
    private RappelRepository rappelRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Autowired
    public ClientService(ClientRepository clientRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    public List<Client> findAll() {
        return clientRepository.findAll();
    }
    public List<Client> findContactedOnDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return clientRepository.findByStatusAndUpdatedAtBetween(ClientStatus.CONTACTE, start, end);
    }

    @Transactional
    public Client updateClientAndQuestionnaire(Long clientId, ClientDto dto, String userEmail) {
        Client client = getById(clientId);
        User user = userRepository.findByEmail(userEmail);

        // Mise à jour du statut
        if (dto.getStatus() != null) {
            client.setStatus(dto.getStatus());
        }

        // Mise à jour des champs du questionnaire
        client.setRaisonNonRenouvellement(dto.getRaisonNonRenouvellement());
        client.setQualiteService(dto.getQualiteService());
        client.setADifficultesRencontrees(dto.getADifficultesRencontrees());
        client.setPrecisionDifficultes(dto.getPrecisionDifficultes());
        client.setInteretNouveauCredit(dto.getInteretNouveauCredit());
        client.setRendezVousAgence(dto.getRendezVousAgence());
        client.setDateHeureRendezVous(dto.getDateHeureRendezVous());
        client.setFacteurInfluence(dto.getFacteurInfluence());
        client.setAutresFacteurs(dto.getAutresFacteurs());

        client.setUpdatedBy(user);
        client.setUpdatedAt(LocalDateTime.now());

        return clientRepository.save(client);
    }

    public List<Client> findWithRendezVousOnDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return clientRepository.findByRendezVousAgenceTrueAndDateHeureRendezVousBetween(start, end);
    }
    // Ajouter cette méthode à ClientService.java
    public List<Client> searchClients(String query) {
        return clientRepository.searchByNomOrPrenom(query);
    }
    public List<Client> findByStatus(ClientStatus status) {
        return clientRepository.findByStatus(status);
    }

    public Client getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'ID: " + id));
    }



    public List<Client> findByUser(User user) {
        return clientRepository.findByAssignedUser(user);
    }

    public List<Client> findClientsByUserAndStatus(User user, ClientStatus status) {
        return clientRepository.findByAssignedUserAndStatus(user, status);
    }

    public List<Client> findUnassignedClients() {
        return clientRepository.findByAssignedUserIsNull();
    }

    @Transactional
    public Client create(ClientDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail);

        Client client = new Client();
        updateClientFromDto(client, dto);
        client.setStatus(ClientStatus.NON_TRAITE);
        client.setCreatedBy(user);
        client.setUpdatedBy(user);

        return clientRepository.save(client);
    }

    @Transactional
    public Client updateStatus(Long clientId, ClientStatus status, String userEmail) {
        Client client = getById(clientId);
        User user = userRepository.findByEmail(userEmail);

        client.setStatus(status);
        client.setUpdatedBy(user);
        client.setUpdatedAt(LocalDateTime.now());

        // Si le statut est CONTACTE, marquer tous les rappels comme complétés
        if (status == ClientStatus.CONTACTE) {
            rappelRepository.completeAllRappelsForClient(clientId);
        }

        return clientRepository.save(client);
    }
    @Transactional
    public Client assignToUser(Long clientId, Long userId) {
        Client client = getById(clientId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        client.setAssignedUser(user);
        client.setUpdatedBy(user);
        client.setUpdatedAt(LocalDateTime.now());

        return clientRepository.save(client);
    }

    @Transactional
    public Client saveQuestionnaire(Long clientId, ClientDto dto, String userEmail) {
        Client client = getById(clientId);
        User user = userRepository.findByEmail(userEmail);

        // Mise à jour des champs du questionnaire
        client.setRaisonNonRenouvellement(dto.getRaisonNonRenouvellement());
        client.setQualiteService(dto.getQualiteService());
        client.setADifficultesRencontrees(dto.getADifficultesRencontrees());
        client.setPrecisionDifficultes(dto.getPrecisionDifficultes());
        client.setInteretNouveauCredit(dto.getInteretNouveauCredit());
        client.setRendezVousAgence(dto.getRendezVousAgence());
        client.setDateHeureRendezVous(dto.getDateHeureRendezVous());
        client.setFacteurInfluence(dto.getFacteurInfluence());
        client.setAutresFacteurs(dto.getAutresFacteurs());

        client.setStatus(ClientStatus.CONTACTE);
        client.setUpdatedBy(user);
        client.setUpdatedAt(LocalDateTime.now());

        // Marquer tous les rappels associés à ce client comme complétés
        rappelRepository.completeAllRappelsForClient(client.getId());

        return clientRepository.save(client);
    }

    private void updateClientFromDto(Client client, ClientDto dto) {
        client.setNom(dto.getNom());
        client.setPrenom(dto.getPrenom());
        client.setTelephone(dto.getTelephone());
        client.setEmail(dto.getEmail());

        if (dto.getAssignedUserId() != null) {
            User assignedUser = userRepository.findById(dto.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur assigné non trouvé"));
            client.setAssignedUser(assignedUser);
        }
    }

    public ClientDto convertToDto(Client client) {
        ClientDto dto = new ClientDto();
        dto.setId(client.getId());
        dto.setNom(client.getNom());
        dto.setPrenom(client.getPrenom());
        dto.setTelephone(client.getTelephone());
        dto.setEmail(client.getEmail());
        dto.setStatus(client.getStatus());

        if (client.getAssignedUser() != null) {
            dto.setAssignedUserId(client.getAssignedUser().getId());
        }

        // Champs du questionnaire
        dto.setRaisonNonRenouvellement(client.getRaisonNonRenouvellement());
        dto.setQualiteService(client.getQualiteService());
        dto.setADifficultesRencontrees(client.getADifficultesRencontrees());
        dto.setPrecisionDifficultes(client.getPrecisionDifficultes());
        dto.setInteretNouveauCredit(client.getInteretNouveauCredit());
        dto.setRendezVousAgence(client.getRendezVousAgence());
        dto.setDateHeureRendezVous(client.getDateHeureRendezVous());
        dto.setFacteurInfluence(client.getFacteurInfluence());
        dto.setAutresFacteurs(client.getAutresFacteurs());

        return dto;
    }
}