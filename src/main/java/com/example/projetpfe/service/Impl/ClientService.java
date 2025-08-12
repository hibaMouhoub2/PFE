package com.example.projetpfe.service.Impl;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.BrancheRepository;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.RappelRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.UserService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ClientService {
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final RappelRepository rappelRepository;
    private final AuditService auditService;
    private final UserService userService;
    private final BrancheRepository brancheRepository;

    @Autowired
    public ClientService(ClientRepository clientRepository,
                         UserRepository userRepository,
                         RappelRepository rappelRepository,
                         AuditService auditService,
                         UserService userService,
                         BrancheRepository brancheRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.rappelRepository = rappelRepository;
        this.auditService = auditService;
        this.userService = userService;
        this.brancheRepository = brancheRepository;
    }

    public List<Client> findAll() {
        return clientRepository.findAll();
    }
    public List<Client> findContactedOnDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return clientRepository.findByStatusAndUpdatedAtBetweenOrderByUpdatedAtDesc(ClientStatus.CONTACTE, start, end);
    }

    public List<Client> findUnassignedClientsByDirectionAndBranche(String directionCode, Branche branche, int limit) {
        if (directionCode == null) {
            return new ArrayList<>();
        }

        return clientRepository.findAll().stream()
                .filter(client -> client.getAssignedUser() == null &&
                        directionCode.equals(client.getNMDIR()) &&
                        (branche == null || branche.equals(client.getNMBRA())))
                .limit(limit)
                .collect(Collectors.toList());
    }
    public long countUnassignedClientsByDirectionAndBranche(String directionCode, Branche branche) {
        if (directionCode == null) {
            return 0;
        }

        return clientRepository.findAll().stream()
                .filter(client -> client.getAssignedUser() == null &&
                        directionCode.equals(client.getNMDIR()) &&
                        (branche == null || Objects.equals(branche.getId(), client.getNMBRA() != null ? client.getNMBRA().getId() : null))).count();
    }

    @Transactional
    public Client updateClientAndQuestionnaire(Long clientId, ClientDto dto, String userEmail) {
        System.out.println("DEBUG: Méthode updateClientAndQuestionnaire appelée");
        System.out.println("DEBUG: ClientID = " + clientId);
        System.out.println("DEBUG: DTO = " + dto.toString());

        Client client = getById(clientId);
        System.out.println("DEBUG: Client récupéré = " + client.getId());

        User user = userRepository.findByEmail(userEmail);
        System.out.println("DEBUG: User récupéré = " + user.getName());

        // Conserver l'état avant modification pour l'audit
        ClientStatus oldStatus = client.getStatus();

        // Mise à jour du statut
        if (dto.getStatus() != null) {
            System.out.println("DEBUG: Mise à jour du statut: " + dto.getStatus());
            client.setStatus(dto.getStatus());
        }

        // Affichez les valeurs avant la mise à jour
        System.out.println("DEBUG: Avant mise à jour - RaisonNonRenouvellement: " + client.getRaisonNonRenouvellement());
        System.out.println("DEBUG: DTO RaisonNonRenouvellement: " + dto.getRaisonNonRenouvellement());

        // Mise à jour des champs du questionnaire
        client.setRaisonNonRenouvellement(dto.getRaisonNonRenouvellement());
        client.setQualiteService(dto.getQualiteService());
        client.setActiviteClient(dto.getActiviteClient());
        client.setProfil(dto.getProfil());
        client.setADifficultesRencontrees(dto.getADifficultesRencontrees());
        client.setPrecisionDifficultes(dto.getPrecisionDifficultes());
        client.setInteretNouveauCredit(dto.getInteretNouveauCredit());
        client.setRendezVousAgence(dto.getRendezVousAgence());
        client.setDateHeureRendezVous(dto.getDateHeureRendezVous());
        client.setNMBRA(dto.getNMBRA());
        client.setFacteurInfluence(dto.getFacteurInfluence());
        client.setAutresFacteurs(dto.getAutresFacteurs());
        client.setAutresRaisons(dto.getAutresRaisons());

        client.setUpdatedBy(user);
        client.setUpdatedAt(LocalDateTime.now());

        System.out.println("DEBUG: Après mise à jour - RaisonNonRenouvellement: " + client.getRaisonNonRenouvellement());

        Client savedClient = clientRepository.save(client);
        System.out.println("DEBUG: Client sauvegardé, ID: " + savedClient.getId());

        // Audit de la modification
        auditService.auditEvent(AuditType.CLIENT_QUESTIONNAIRE_UPDATED,
                "Client",
                savedClient.getId(),
                "Questionnaire modifié pour client: " + savedClient.getNom() + " " + savedClient.getPrenom(),
                userEmail);

        // Si le statut a changé, ajouter une entrée d'audit spécifique
        if (dto.getStatus() != null && oldStatus != dto.getStatus()) {
            auditService.auditEvent(AuditType.CLIENT_STATUS_CHANGE,
                    "Client",
                    savedClient.getId(),
                    "Statut modifié: " + oldStatus + " -> " + dto.getStatus(),
                    userEmail);
        }
        return savedClient;
    }

    public List<Client> findWithRendezVousOnDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return clientRepository.findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenOrderByUpdatedAtDesc(start, end);
    }

    public List<Client> findClientsWithRendezVousForDateAndBranche(LocalDate date, Branche branche) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        if (branche != null) {
            return clientRepository.findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenAndNMBRAOrderByUpdatedAtDesc(
                    startOfDay, endOfDay, branche);
        } else {
            return clientRepository.findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenOrderByUpdatedAtDesc(
                    startOfDay, endOfDay);
        }
    }

    public List<Client> searchClients(String query) {
        return clientRepository.searchByNomOrPrenomOrCin(query);
    }
    public List<Client> findByStatus(ClientStatus status) {
        return clientRepository.findByStatusOrderByUpdatedAtDesc(status);
    }

    public Client getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'ID: " + id));
    }



    public List<Client> findByUser(User user) {
        return clientRepository.findByAssignedUserOrderByUpdatedAtDesc(user);
    }

    public List<Client> findClientsByUserAndStatus(User user, ClientStatus status) {
        return clientRepository.findByAssignedUserAndStatusOrderByUpdatedAtDesc(user, status);
    }

    public List<Client> findUnassignedClients() {
        return clientRepository.findByAssignedUserIsNullOrderByUpdatedAtDesc();
    }

    @Transactional
    public Client create(ClientDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail);

        Client client = new Client();
        updateClientFromDto(client, dto);
        client.setStatus(ClientStatus.NON_TRAITE);
        client.setCreatedBy(user);
        client.setUpdatedBy(user);

        Client savedClient = clientRepository.save(client);

        // Audit de la création du client
        auditService.auditEvent(AuditType.CLIENT_CREATED,
                "Client",
                savedClient.getId(),
                "Client créé: " + savedClient.getNom() + " " + savedClient.getPrenom(),
                userEmail);

        return savedClient;
    }

    @Transactional
    public Client updateStatus(Long clientId, ClientStatus status,String notes, String userEmail) {
        Client client = getById(clientId);
        User user = userRepository.findByEmail(userEmail);
        // Statut précédent pour l'audit
        ClientStatus oldStatus = client.getStatus();
        client.setStatus(status);
        client.setUpdatedBy(user);
        client.setNotes(notes);
        client.setUpdatedAt(LocalDateTime.now());

        // Si le statut est CONTACTE, marquer tous les rappels comme complétés
        if (status == ClientStatus.CONTACTE) {
            rappelRepository.completeAllRappelsForClient(clientId);
        }

        Client savedClient = clientRepository.save(client);

        // Audit du changement de statut
        auditService.auditEvent(AuditType.CLIENT_STATUS_CHANGE,
                "Client",
                client.getId(),
                "Statut modifié: " + oldStatus + " -> " + status + (notes != null && !notes.isEmpty() ? " (Notes: " + notes + ")" : ""),
                userEmail);

        return savedClient;
    }
    @Transactional
    public Client assignToUser(Long clientId, Long userId) {
        Client client = getById(clientId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Conserver l'utilisateur précédent pour l'audit
        User previousUser = client.getAssignedUser();
        boolean isReassignment = previousUser != null;
        client.setAssignedUser(user);
        client.setUpdatedBy(user);
        client.setUpdatedAt(LocalDateTime.now());

        Client savedClient = clientRepository.save(client);

        // Audit de l'assignation
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();

        if (isReassignment) {
            auditService.auditEvent(AuditType.CLIENT_REASSIGNED,
                    "Client",
                    client.getId(),
                    "Client réassigné de " + previousUser.getName() + " à " + user.getName(),
                    adminEmail);
        } else {
            auditService.auditEvent(AuditType.CLIENT_ASSIGNED,
                    "Client",
                    client.getId(),
                    "Client assigné à " + user.getName(),
                    adminEmail);
        }

        return savedClient;
    }

    @Transactional
    public Client saveQuestionnaire(Long clientId, ClientDto dto, String userEmail) {
        Client client = getById(clientId);
        User user = userRepository.findByEmail(userEmail);
        System.out.println("DEBUG - Avant enregistrement du questionnaire - Branche: " + client.getNMBRA());
        Branche clientBranche = client.getNMBRA();
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
        if (clientBranche != null) {
            client.setNMBRA(clientBranche);
        }
        System.out.println("DEBUG - Juste avant sauvegarde - Branche: " + client.getNMBRA());
        Client savedClient = clientRepository.save(client);
        System.out.println("DEBUG - Après sauvegarde - Branche: " + savedClient.getNMBRA());
        // Audit du questionnaire complété
        auditService.auditEvent(AuditType.CLIENT_QUESTIONNAIRE_COMPLETED,
                "Client",
                client.getId(),
                "Questionnaire complété pour client: " + client.getNom() + " " + client.getPrenom(),
                userEmail);

        // Audit du changement de statut
        auditService.auditEvent(AuditType.CLIENT_STATUS_CHANGE,
                "Client",
                client.getId(),
                "Statut modifié à CONTACTE suite au questionnaire",
                userEmail);

        return savedClient;
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
    public Client updateStatusAndPhone(Long clientId, ClientStatus status, String notes, String telephone, String userEmail) {
        Client client = getById(clientId);
        User user = userRepository.findByEmail(userEmail);
        System.out.println("DEBUG - Avant mise à jour - Branche: " + client.getNMBRA());
        // Statut précédent pour l'audit
        ClientStatus oldStatus = client.getStatus();
        client.setStatus(status);
        client.setUpdatedBy(user);
        client.setNotes(notes);
        client.setUpdatedAt(LocalDateTime.now());
        Branche clientBranche = client.getNMBRA();

        // Vérifier si le téléphone a été modifié
        boolean phoneChanged = false;
        String oldPhone = client.getTelephone();

        if (telephone != null && !telephone.isEmpty() && !telephone.equals(oldPhone)) {
            client.setTelephone(telephone);
            phoneChanged = true;
        }

        // Si le statut est CONTACTE, marquer tous les rappels comme complétés
        if (status == ClientStatus.CONTACTE) {
            rappelRepository.completeAllRappelsForClient(clientId);
        }
        if (clientBranche != null) {
            client.setNMBRA(clientBranche);
        }

        Client savedClient = clientRepository.save(client);
        System.out.println("DEBUG - Après sauvegarde - Branche: " + savedClient.getNMBRA());
        // Audit du changement de statut
        auditService.auditEvent(AuditType.CLIENT_STATUS_CHANGE,
                "Client",
                client.getId(),
                "Statut modifié: " + oldStatus + " -> " + status + (notes != null && !notes.isEmpty() ? " (Notes: " + notes + ")" : ""),
                userEmail);

        // Audit du changement de téléphone si nécessaire
        if (phoneChanged) {
            auditService.auditEvent(AuditType.CLIENT_PHONE_CHANGED,
                    "Client",
                    client.getId(),
                    "Téléphone modifié: " + oldPhone + " -> " + telephone,
                    userEmail);
        }

        return savedClient;
    }

    @Transactional
    public ImportResult importClientsFromExcel(MultipartFile file, String userEmail) throws IOException {
        int importedCount = 0;
        int skippedCount = 0;
        int updatedCount = 0;  // Nouveau compteur pour les clients mis à jour
        List<String> skippedCins = new ArrayList<>();
        List<String> skippedForDirection = new ArrayList<>();
        List<String> updatedCins = new ArrayList<>();  // Liste pour suivre les CIN mises à jour

        System.out.println("Starting import for user: " + userEmail);

        // Récupérer l'utilisateur connecté et sa direction
        User currentUser = userRepository.findByEmail(userEmail);
        boolean isSuperAdmin = userService.isSuperAdmin(currentUser);

        // Récupérer le code de direction de l'administrateur
        String userDirectionCode = null;
        if (!isSuperAdmin && currentUser.getDirection() != null) {
            userDirectionCode = currentUser.getDirection().getCode();
        }

        System.out.println("User direction codes: " + userDirectionCode);
        System.out.println("Is super admin: " + isSuperAdmin);

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

                // Récupérer le CIN avant de créer le client
                String cin = getCellValueAsString(row.getCell(3));  // Indice 3 pour la colonne CIN
                String nmdir = getCellValueAsString(row.getCell(0));

                System.out.println("Processing row - CIN: " + cin + ", NMDIR: " + nmdir);

                // Vérifier les autorisations de direction

                boolean directionMatches = false;
                if (isSuperAdmin) {
                    directionMatches = true;
                } else if (userDirectionCode != null && nmdir != null) {
                    // Utilisons la méthode de comparaison normalisée
                    directionMatches = codesMatch(nmdir, userDirectionCode);
                    System.out.println("Direction match check: '" + nmdir + "' vs '" + userDirectionCode + "' = " + directionMatches);
                }

                if (!directionMatches) {
                    skippedCount++;
                    if (cin != null && !cin.isEmpty()) {
                        skippedForDirection.add(cin);
                    }
                    System.out.println("Skipping client " + cin + " - direction mismatch");
                    continue;
                }

                // Extraire la date de fin de contrat du fichier Excel
                Date dtfincImported = null;
                Cell dtfincCell = row.getCell(6);
                if (dtfincCell != null && dtfincCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dtfincCell)) {
                    dtfincImported = dtfincCell.getDateCellValue();
                }

                boolean updateExisting = false;
                Client existingClient = null;

                // Vérifier si le CIN existe déjà
                if (cin != null && !cin.isEmpty()) {
                    existingClient = clientRepository.findByCin(cin);

                    if (existingClient != null) {
                        Date existingDtfinc = existingClient.getDTFINC();

                        // Comparer les dates de fin de contrat
                        if (dtfincImported != null && existingDtfinc != null &&
                                dtfincImported.after(existingDtfinc)) {

                            // La date importée est plus récente, on met à jour le client existant
                            updateExisting = true;
                            System.out.println("Updating existing client " + cin +
                                    " with newer contract end date: " + dtfincImported +
                                    " (old: " + existingDtfinc + ")");
                        } else {
                            // La date n'est pas plus récente, on ignore ce client
                            skippedCount++;
                            skippedCins.add(cin);
                            System.out.println("Skipping existing CIN: " + cin +
                                    " - new date not newer than existing");
                            continue;
                        }
                    }
                }

                // Créer ou mettre à jour le client
                Client client;
                if (updateExisting) {
                    client = existingClient;
                    // Garder l'utilisateur assigné et les autres données importantes
                    // qui ne devraient pas être écrasées
                    User assignedUser = client.getAssignedUser();
                    ClientStatus oldStatus = client.getStatus();
                    // Autres champs à préserver si nécessaire

                    // Mettre à jour les données de base (à adapter selon vos besoins)
                    updateClientBaseInfo(client, row);

                    // Restaurer les données importantes
                    client.setAssignedUser(assignedUser);

                    // Réinitialiser le statut uniquement si nécessaire
                    // Par exemple, si vous voulez que les clients mis à jour soient considérés
                    // comme "non traités" à nouveau:
                    client.setStatus(ClientStatus.NON_TRAITE);

                    // Date de mise à jour
                    client.setUpdatedAt(LocalDateTime.now());

                    updatedCount++;
                    updatedCins.add(cin);
                } else {
                    // Créer un nouveau client
                    client = new Client();
                    updateClientBaseInfo(client, row);

                    // Définir les valeurs par défaut pour un nouveau client
                    client.setStatus(ClientStatus.NON_TRAITE);
                    client.setCreatedAt(LocalDateTime.now());
                    client.setUpdatedAt(LocalDateTime.now());

                    importedCount++;
                }

                clientRepository.save(client);
                System.out.println((updateExisting ? "Updated" : "Imported") + " client: " + cin);
            }

            System.out.println("Import summary - Imported: " + importedCount +
                    ", Updated: " + updatedCount +
                    ", Skipped: " + skippedCount);
            System.out.println("Skipped CINs: " + skippedCins);
            System.out.println("Updated CINs: " + updatedCins);
            System.out.println("Skipped for direction: " + skippedForDirection);

            // Audit de l'importation
            String auditMessage = "Importation Excel: " + importedCount + " clients importés, " +
                    updatedCount + " clients mis à jour, " +
                    skippedCount + " ignorés";

            auditService.auditEvent(AuditType.EXCEL_IMPORT,
                    "System",
                    null,
                    auditMessage,
                    userEmail);
        }

        // Modifier la classe ImportResult pour inclure les clients mis à jour
        return new ImportResult(importedCount, updatedCount, skippedCount, skippedCins, updatedCins, skippedForDirection);
    }
    private String normalizeCode(String code) {
        if (code == null) return null;
        return code.trim()
                .toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replaceAll("_+", "_")  // Remplace plusieurs underscores consécutifs par un seul
                .replaceAll("^_|_$", ""); // Supprime les underscores au début et à la fin
    }
    private boolean codesMatch(String code1, String code2) {
        if (code1 == null || code2 == null) return false;
        return normalizeCode(code1).equals(normalizeCode(code2));
    }

    // Méthode auxiliaire pour mettre à jour les informations de base d'un client
    private void updateClientBaseInfo(Client client, Row row) {
        // Normaliser NMDIR lors de l'importation
        String nmdir = getCellValueAsString(row.getCell(0));
        if (nmdir != null) {
            nmdir = normalizeCode(nmdir);
        }
        client.setNMDIR(nmdir);

        // Normaliser NMREG lors de l'importation
        String nmreg = getCellValueAsString(row.getCell(1));
        if (nmreg != null) {
            nmreg = normalizeCode(nmreg);
        }
        client.setNMREG(nmreg);

        // Pour NMBRA (entité), conversion nécessaire
        String nmbraStr = getCellValueAsString(row.getCell(2));
        if (nmbraStr != null && !nmbraStr.isEmpty()) {
            // Normaliser aussi le code de branche pour la recherche
            String normalizedBrancheCode = normalizeCode(nmbraStr);

            // Essayer de trouver par code normalisé
            Branche branche = brancheRepository.findByCode(normalizedBrancheCode).orElse(null);

            // Si pas trouvé par code normalisé, essayer avec le code original
            if (branche == null) {
                branche = brancheRepository.findByCode(nmbraStr).orElse(null);
            }

            // Si toujours pas trouvé, essayer par displayName
            if (branche == null) {
                List<Branche> allBranches = brancheRepository.findAll();
                final String originalBrancheStr = nmbraStr;
                for (Branche b : allBranches) {
                    if (b.getDisplayname() != null &&
                            (b.getDisplayname().equalsIgnoreCase(originalBrancheStr) ||
                                    normalizeCode(b.getDisplayname()).equals(normalizedBrancheCode))) {
                        branche = b;
                        break;
                    }
                }
            }

            client.setNMBRA(branche);

            // Log pour debug
            if (branche != null) {
                System.out.println("Branche trouvée: " + nmbraStr + " -> " + branche.getDisplayname());
            } else {
                System.out.println("Branche non trouvée pour: " + nmbraStr);
            }
        }

        // CIN
        client.setCin(getCellValueAsString(row.getCell(3)));

        // Nom
        client.setNom(getCellValueAsString(row.getCell(4)));

        // Prénom
        client.setPrenom(getCellValueAsString(row.getCell(5)));

        // Pour DTFINC (date de fin de contrat)
        Cell dtfincCell = row.getCell(6);
        if (dtfincCell != null && dtfincCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dtfincCell)) {
            client.setDTFINC(dtfincCell.getDateCellValue());
        }

        // Téléphone principal
        client.setTelephone(getCellValueAsString(row.getCell(7)));

        // Téléphone secondaire
        client.setTelephone2(getCellValueAsString(row.getCell(8)));

        // Activité actuelle
        client.setActiviteActuelle(getCellValueAsString(row.getCell(9)));

        // BAREM
        client.setBAREM(getCellValueAsString(row.getCell(10)));

        // Pour DTDEBC (date de début de contrat)
        Cell dtdebcCell = row.getCell(11);
        if (dtdebcCell != null && dtdebcCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dtdebcCell)) {
            client.setDTDEBC(dtdebcCell.getDateCellValue());
        }

        // Pour MNTDEB (montant de début - double)
        Cell mntDebCell = row.getCell(12);
        if (mntDebCell != null && mntDebCell.getCellType() == CellType.NUMERIC) {
            client.setMNTDEB(mntDebCell.getNumericCellValue());
        }

        // Pour NBINC (nombre d'incidents - integer)
        Cell nbreIncCell = row.getCell(13);
        if (nbreIncCell != null && nbreIncCell.getCellType() == CellType.NUMERIC) {
            client.setNBINC((int) nbreIncCell.getNumericCellValue());
        }

        // Pour AgeClient (âge du client - integer)
        Cell ageCell = row.getCell(14);
        if (ageCell != null && ageCell.getCellType() == CellType.NUMERIC) {
            client.setAgeClient((int) ageCell.getNumericCellValue());
        }

        // Pour NBPRETS (nombre de prêts - integer)
        Cell nbPretsCell = row.getCell(15);
        if (nbPretsCell != null && nbPretsCell.getCellType() == CellType.NUMERIC) {
            client.setNBPRETS((int) nbPretsCell.getNumericCellValue());
        }
    }


    // Classe pour retourner les résultats de l'importation
    public static class ImportResult {
        private final int importedCount;
        private final int updatedCount;  // Nouveau compteur
        private final int skippedCount;
        private final List<String> skippedCins;
        private final List<String> updatedCins;  // Nouvelle liste
        private final List<String> skippedForRegion;

        public ImportResult(int importedCount, int updatedCount, int skippedCount,
                            List<String> skippedCins, List<String> updatedCins,
                            List<String> skippedForRegion) {
            this.importedCount = importedCount;
            this.updatedCount = updatedCount;
            this.skippedCount = skippedCount;
            this.skippedCins = skippedCins;
            this.updatedCins = updatedCins;
            this.skippedForRegion = skippedForRegion;
        }

        public int getImportedCount() {
            return importedCount;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public List<String> getSkippedCins() {
            return skippedCins;
        }

        public List<String> getUpdatedCins() {
            return updatedCins;
        }

        public List<String> getSkippedForRegion() {
            return skippedForRegion;
        }
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;

        // Vérifier au moins les colonnes essentielles (nom, prénom, CIN, téléphone)
        for (int i : new int[]{3, 4, 5, 7}) {  // indices de cin, NMCLI, PNCLI, teledo
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                        return sdf.format(date);
                    } else {
                        double numericValue = cell.getNumericCellValue();
                        // Pour les nombres entiers, éviter la notation scientifique
                        if (numericValue == (long) numericValue) {
                            return String.valueOf((long) numericValue);
                        } else {
                            return String.valueOf(numericValue);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    // Évaluer la formule et retourner le résultat
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);

                    switch (cellValue.getCellType()) {
                        case STRING:
                            return cellValue.getStringValue().trim();
                        case NUMERIC:
                            double numValue = cellValue.getNumberValue();
                            if (numValue == (long) numValue) {
                                return String.valueOf((long) numValue);
                            } else {
                                return String.valueOf(numValue);
                            }
                        case BOOLEAN:
                            return String.valueOf(cellValue.getBooleanValue());
                        default:
                            return null;
                    }
                case BLANK:
                case _NONE:
                    return null;
                default:
                    System.err.println("Type de cellule non supporté: " + cell.getCellType());
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture de la cellule (" +
                    cell.getRowIndex() + "," + cell.getColumnIndex() + "): " + e.getMessage());
            return null;
        }
    }

    public List<Client> findByCinOrPhone(String query) {
        return clientRepository.findByCinOrPhoneOrderByUpdatedAtDesc(query);
    }
    @Transactional
    public void deleteClient(Long id) {
        Client client = getById(id);

        // Récupérer les informations pour l'audit avant la suppression
        String clientName = client.getNom() + " " + client.getPrenom();
        String clientCin = client.getCin();

        // D'abord supprimer les rappels associés au client (pour éviter les violations de contraintes FK)
        rappelRepository.deleteByClient(client);

        // Supprimer le client
        clientRepository.delete(client);

        // Audit de la suppression
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(AuditType.CLIENT_DELETED,
                "Client",
                id,
                "Client supprimé: " + clientName + " (CIN: " + (clientCin != null ? clientCin : "N/A") + ")",
                userEmail);
    }

    public List<Client> findByStatusAndRegions(ClientStatus status, List<String> regionCodes) {
        return clientRepository.findByStatusAndNMREGInOrderByUpdatedAtDesc(status, regionCodes);
    }

    public List<Client> findUnassignedClientsByRegions(List<String> regionCodes) {
        return clientRepository.findByAssignedUserIsNullAndNMREGInOrderByUpdatedAtDesc(regionCodes);
    }

    /**
     * Filtre les clients par direction (pour les statistiques et rapports)
     */
    public List<Client> filterClientsByDirection(String directionCode) {
        if (directionCode == null) {
            return new ArrayList<>();
        }

        return clientRepository.findAll().stream()
                .filter(client -> directionCode.equals(client.getNMDIR()))
                .collect(Collectors.toList());
    }


    public List<Client> filterClientsByDirectionAndDate(String directionCode, LocalDateTime start, LocalDateTime end) {
        if (directionCode == null) {
            return new ArrayList<>();
        }

        return clientRepository.findAll().stream()
                .filter(client -> directionCode.equals(client.getNMDIR()) &&
                        client.getUpdatedAt() != null &&
                        client.getUpdatedAt().isAfter(start) &&
                        client.getUpdatedAt().isBefore(end))
                .collect(Collectors.toList());
    }

    /**
     * Trouve les clients non assignés par direction
     */
    public List<Client> findUnassignedClientsByDirection(String directionCode) {
        return clientRepository.findAll().stream()
                .filter(client -> client.getAssignedUser() == null &&
                        directionCode.equals(client.getNMDIR()))
                .collect(Collectors.toList());
    }

    /**
     * Trouve les clients par direction et statut
     */
    public List<Client> findClientsByDirectionAndStatus(String directionCode, ClientStatus status) {
        if (directionCode == null) {
            return new ArrayList<>();
        }
        return clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, status);
    }

    public List<Client> findClientsWithPhoneChanges(LocalDateTime startDate, LocalDateTime endDate) {
        return clientRepository.findClientsWithPhoneChanges(startDate, endDate);
    }

    @Transactional
    public ImportResult importClientsFromExcelBySuperAdmin(MultipartFile file, String userEmail) throws IOException {
        int importedCount = 0;
        int skippedCount = 0;
        int updatedCount = 0;
        List<String> skippedCins = new ArrayList<>();
        List<String> updatedCins = new ArrayList<>();

        System.out.println("Starting SuperAdmin import for user: " + userEmail);

        // Récupérer l'utilisateur connecté
        User currentUser = userRepository.findByEmail(userEmail);

        // Vérification de sécurité supplémentaire
        if (!userService.isSuperAdmin(currentUser)) {
            throw new SecurityException("Seuls les super administrateurs peuvent importer des clients");
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip header
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int currentRowNum = row.getRowNum();

                try {

                    String cin = getCellValueAsString(row.getCell(4)); // Colonne CIN
                    if (cin == null || cin.trim().isEmpty()) {
                        skippedCount++;
                        continue;
                    }

                    // Vérifier si le client existe déjà
                    Client existingClient = clientRepository.findByCin(cin);

                    if (existingClient != null) {
                        // Logique de mise à jour basée sur la date de fin de contrat
                        Date newEndDate = getCellValueAsDate(row.getCell(9)); // DTFINC
                        if (newEndDate != null && existingClient.getDTFINC() != null) {
                            if (newEndDate.after(existingClient.getDTFINC())) {
                                // Mettre à jour avec les nouvelles informations
                                updateClientBaseInfo(existingClient, row);
                                clientRepository.save(existingClient);
                                updatedCount++;
                                updatedCins.add(cin);
                                System.out.println("Updated client: " + cin);
                            } else {
                                // Date plus ancienne, ignorer
                                skippedCount++;
                                skippedCins.add(cin);
                            }
                        } else {
                            skippedCount++;
                            skippedCins.add(cin);
                        }
                    } else {
                        // Nouveau client - PAS de filtre par direction pour SuperAdmin
                        Client client = new Client();
                        updateClientBaseInfo(client, row);

                        // Définir les informations d'audit
                        client.setCreatedBy(currentUser);
                        client.setUpdatedBy(currentUser);

                        clientRepository.save(client);
                        importedCount++;
                        System.out.println("Imported new client: " + cin);
                    }

                } catch (Exception e) {
                    System.err.println("❌ ERREUR ligne " + currentRowNum + ": " + e.getMessage());
                    e.printStackTrace();
                    skippedCount++;
                }
            }

            System.out.println("SuperAdmin Import summary - Imported: " + importedCount +
                    ", Updated: " + updatedCount +
                    ", Skipped: " + skippedCount);

            // Audit de l'importation par SuperAdmin
            String auditMessage = "Importation Excel par SuperAdmin: " + importedCount + " clients importés, " +
                    updatedCount + " clients mis à jour, " +
                    skippedCount + " ignorés";

            auditService.auditEvent(AuditType.EXCEL_IMPORT,
                    "System",
                    null,
                    auditMessage,
                    userEmail);
        }

        return new ImportResult(importedCount, updatedCount, skippedCount, skippedCins, updatedCins, new ArrayList<>());
    }

    public long getTotalClientsCount() {
        return clientRepository.count();
    }

    public long getUnassignedClientsCount() {
        return clientRepository.countByAssignedUserIsNull();
    }

    private Date getCellValueAsDate(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
                        return cell.getDateCellValue();
                    }
                    break;
                case STRING:
                    // Si c'est une chaîne, essayer de la parser
                    String dateStr = cell.getStringCellValue().trim();
                    if (!dateStr.isEmpty()) {
                        // Ajouter ici les formats de date que vous utilisez
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                        return sdf.parse(dateStr);
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la conversion de date: " + e.getMessage());
        }

        return null;
    }

    public List<Client> findAllClients() {
        return clientRepository.findAll();
    }


    public List<String> getUniqueDirections() {
        return clientRepository.findAll().stream()
                .map(Client::getNMDIR)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public String validateExcelFile(MultipartFile file) throws IOException {
        StringBuilder validationReport = new StringBuilder();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Vérifier l'en-tête
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                errors.add("Ligne d'en-tête manquante");
                return "ERREUR: " + String.join(", ", errors);
            }

            // Vérifier le nombre de colonnes attendues
            int expectedColumns = 15; // Ajustez selon vos besoins
            if (headerRow.getLastCellNum() < expectedColumns) {
                warnings.add("Nombre de colonnes insuffisant: " + headerRow.getLastCellNum() + " au lieu de " + expectedColumns);
            }

            // Analyser chaque ligne de données
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip header
            }

            int rowNum = 1;
            int validRows = 0;
            int invalidRows = 0;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowNum++;

                try {
                    // Validation ligne par ligne
                    List<String> rowErrors = validateRow(row, rowNum);
                    if (rowErrors.isEmpty()) {
                        validRows++;
                    } else {
                        invalidRows++;
                        errors.addAll(rowErrors);
                    }

                    // Limiter le nombre d'erreurs affichées
                    if (errors.size() > 50) {
                        errors.add("... et " + (sheet.getLastRowNum() - rowNum) + " autres erreurs potentielles");
                        break;
                    }

                } catch (Exception e) {
                    errors.add("Ligne " + rowNum + ": Erreur de traitement - " + e.getMessage());
                    invalidRows++;
                }
            }

            // Construire le rapport
            validationReport.append("=== RAPPORT DE VALIDATION ===\n");
            validationReport.append("Lignes valides: ").append(validRows).append("\n");
            validationReport.append("Lignes invalides: ").append(invalidRows).append("\n");

            if (!warnings.isEmpty()) {
                validationReport.append("\n--- AVERTISSEMENTS ---\n");
                warnings.forEach(w -> validationReport.append("⚠️ ").append(w).append("\n"));
            }

            if (!errors.isEmpty()) {
                validationReport.append("\n--- ERREURS ---\n");
                errors.forEach(e -> validationReport.append("❌ ").append(e).append("\n"));
            }

            return validationReport.toString();
        }
    }

    private List<String> validateRow(Row row, int rowNum) {
        List<String> errors = new ArrayList<>();

        try {
            // Validation CIN (obligatoire)
            String cin = getCellValueAsString(row.getCell(4));
            if (cin == null || cin.trim().isEmpty()) {
                errors.add("Ligne " + rowNum + ": CIN manquant");
            } else if (cin.length() < 6 || cin.length() > 12) {
                errors.add("Ligne " + rowNum + ": CIN invalide (longueur incorrecte): " + cin);
            }

            // Validation Nom (obligatoire)
            String nom = getCellValueAsString(row.getCell(5));
            if (nom == null || nom.trim().isEmpty()) {
                errors.add("Ligne " + rowNum + ": Nom manquant");
            }

            // Validation Prénom (obligatoire)
            String prenom = getCellValueAsString(row.getCell(6));
            if (prenom == null || prenom.trim().isEmpty()) {
                errors.add("Ligne " + rowNum + ": Prénom manquant");
            }

            // Validation Téléphone
            String telephone = getCellValueAsString(row.getCell(7));
            if (telephone != null && !telephone.trim().isEmpty()) {
                telephone = telephone.replaceAll("[^0-9+]", "");
                if (telephone.length() < 10) {
                    errors.add("Ligne " + rowNum + ": Téléphone invalide: " + getCellValueAsString(row.getCell(7)));
                }
            }

            // Validation Date de fin de contrat
            Cell dtfincCell = row.getCell(6);
            if (dtfincCell != null) {
                try {
                    if (dtfincCell.getCellType() == CellType.STRING) {
                        String dateStr = dtfincCell.getStringCellValue().trim();
                        if (!dateStr.isEmpty()) {
                            // Test de parsing de la date
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                            sdf.setLenient(false);
                            sdf.parse(dateStr);
                        }
                    } else if (dtfincCell.getCellType() == CellType.NUMERIC) {
                        if (!DateUtil.isCellDateFormatted(dtfincCell)) {
                            errors.add("Ligne " + rowNum + ": Date de fin de contrat mal formatée");
                        }
                    }
                } catch (ParseException e) {
                    errors.add("Ligne " + rowNum + ": Date de fin de contrat invalide - " + e.getMessage());
                }
            }

            // Validation Direction
            String nmdir = getCellValueAsString(row.getCell(0));
            if (nmdir == null || nmdir.trim().isEmpty()) {
                errors.add("Ligne " + rowNum + ": Direction manquante");
            }

            // Validation Branche
            String nmbraStr = getCellValueAsString(row.getCell(2));
            if (nmbraStr != null && !nmbraStr.trim().isEmpty()) {
                // Vérifier si la branche existe
                String normalizedBrancheCode = normalizeCode(nmbraStr);
                Branche branche = brancheRepository.findByCode(normalizedBrancheCode).orElse(null);
                if (branche == null) {
                    branche = brancheRepository.findByCode(nmbraStr).orElse(null);
                }
                if (branche == null) {
                    errors.add("Ligne " + rowNum + ": Branche non trouvée: " + nmbraStr);
                }
            }

        } catch (Exception e) {
            errors.add("Ligne " + rowNum + ": Erreur de validation - " + e.getMessage());
        }

        return errors;
    }

}