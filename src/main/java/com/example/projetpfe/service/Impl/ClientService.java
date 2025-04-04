package com.example.projetpfe.service.Impl;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.entity.Client;
import com.example.projetpfe.entity.ClientStatus;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.RappelRepository;
import com.example.projetpfe.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;
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
        client.setAutresRaisons(dto.getAutresRaisons());

        client.setUpdatedBy(user);
        client.setUpdatedAt(LocalDateTime.now());

        return clientRepository.save(client);
    }

    public List<Client> findWithRendezVousOnDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return clientRepository.findByRendezVousAgenceTrueAndDateHeureRendezVousBetween(start, end);
    }

    public List<Client> searchClients(String query) {
        return clientRepository.searchByNomOrPrenomOrCin(query);
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
        client.setActiviteClient(dto.getActiviteClient());
        client.setNMBRA(dto.getNMBRA());
        client.setADifficultesRencontrees(dto.getADifficultesRencontrees());
        client.setPrecisionDifficultes(dto.getPrecisionDifficultes());
        client.setInteretNouveauCredit(dto.getInteretNouveauCredit());
        client.setRendezVousAgence(dto.getRendezVousAgence());
        client.setDateHeureRendezVous(dto.getDateHeureRendezVous());
        client.setFacteurInfluence(dto.getFacteurInfluence());
        client.setAutresFacteurs(dto.getAutresFacteurs());
        client.setAutresRaisons(dto.getAutresRaisons());
        client.setProfil(dto.getProfil());
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
        client.setCin(dto.getCin());

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
        dto.setCin(client.getCin());
        dto.setNMDIR(client.getNMDIR());
        dto.setNMREG(client.getNMREG());
        dto.setNMBRA(client.getNMBRA());
        dto.setDTFINC(client.getDTFINC());
        dto.setDTDEBC(client.getDTDEBC());
        dto.setTelephone2(client.getTelephone2());
        dto.setActiviteActuelle(client.getActiviteActuelle());
        dto.setBAREM(client.getBAREM());
        dto.setMNTDEB(client.getMNTDEB());
        dto.setNBINC(client.getNBINC());
        dto.setNBPRETS(client.getNBPRETS());
        dto.setAgeClient(client.getAgeClient());
        dto.setStatus(client.getStatus());

        if (client.getAssignedUser() != null) {
            dto.setAssignedUserId(client.getAssignedUser().getId());
        }

        // Champs du questionnaire
        dto.setRaisonNonRenouvellement(client.getRaisonNonRenouvellement());
        dto.setQualiteService(client.getQualiteService());
        dto.setActiviteClient(client.getActiviteClient());
        dto.setADifficultesRencontrees(client.getADifficultesRencontrees());
        dto.setPrecisionDifficultes(client.getPrecisionDifficultes());
        dto.setInteretNouveauCredit(client.getInteretNouveauCredit());
        dto.setRendezVousAgence(client.getRendezVousAgence());
        dto.setDateHeureRendezVous(client.getDateHeureRendezVous());
        dto.setFacteurInfluence(client.getFacteurInfluence());
        dto.setProfil(client.getProfil());
        dto.setAutresFacteurs(client.getAutresFacteurs());
        dto.setAutresRaisons(client.getAutresRaisons());

        return dto;
    }

    @Transactional
    public int importClientsFromExcel(MultipartFile file) throws IOException {
        int importedCount = 0;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            Iterator<Row> rowIterator = sheet.rowIterator();
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip header row
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Skip empty rows
                if (isEmptyRow(row)) continue;

                Client client = new Client();
                client.setNom(getCellValueAsString(row.getCell(0)));
                client.setPrenom(getCellValueAsString(row.getCell(1)));
                client.setCin(getCellValueAsString(row.getCell(2)));
                client.setTelephone(getCellValueAsString(row.getCell(3)));
                client.setTelephone2(getCellValueAsString(row.getCell(4)));

                // Set default values
                client.setStatus(ClientStatus.NON_TRAITE);
                client.setCreatedAt(LocalDateTime.now());
                client.setUpdatedAt(LocalDateTime.now());

                clientRepository.save(client);
                importedCount++;
            }
        }
        return importedCount;
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;

        for (int i = 0; i < 5; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    public List<Client> findByCinOrPhone(String query) {
        return clientRepository.findByCinOrPhone(query);
    }
}