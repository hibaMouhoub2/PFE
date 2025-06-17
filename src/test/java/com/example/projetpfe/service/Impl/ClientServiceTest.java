package com.example.projetpfe.service.Impl;

import com.example.projetpfe.dto.ClientDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.RappelRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RappelRepository rappelRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ClientService clientService;

    private User testUser;
    private Client testClient;
    private ClientDto testClientDto;

    @BeforeEach
    void setUp() {
        // Configuration de l'utilisateur de test
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        // Configuration du client de test avec tous les champs nécessaires
        testClient = new Client();
        testClient.setId(1L);
        testClient.setNom("Dupont");
        testClient.setPrenom("Jean");
        testClient.setCin("AB123456");
        testClient.setTelephone("0123456789");
        testClient.setTelephone2("0987654321");
        testClient.setNMDIR("DIR001");
        testClient.setNMREG("REG001");
        testClient.setNMBRA(Branche.CASA_AZHAR);
        testClient.setActiviteActuelle("Professeur");
        testClient.setBAREM("BAREME001");
        testClient.setMNTDEB(50000.0);
        testClient.setNBINC(2);
        testClient.setNBPRETS(1);
        testClient.setAgeClient(35);
        testClient.setStatus(ClientStatus.NON_TRAITE);
        testClient.setCreatedBy(testUser);
        testClient.setUpdatedBy(testUser);
        testClient.setCreatedAt(LocalDateTime.now());
        testClient.setUpdatedAt(LocalDateTime.now());

        // Initialiser les champs du questionnaire
        testClient.setRaisonNonRenouvellement(RaisonNonRenouvellement.TAUX_INTERET_ELEVE);
        testClient.setQualiteService(QualiteService.EXCELLENTE);
        testClient.setActiviteClient(ActiviteClient.SALARIE);
        testClient.setProfil(Profil.AMBULANT);
        testClient.setADifficultesRencontrees(false);
        testClient.setInteretNouveauCredit(InteretCredit.OUI);
        testClient.setRendezVousAgence(true);
        testClient.setFacteurInfluence(FacteurInfluence.SERVICE_CLIENT);

        // Configuration du DTO de test avec tous les champs nécessaires
        testClientDto = new ClientDto();
        testClientDto.setId(1L);
        testClientDto.setNom("Dupont");
        testClientDto.setPrenom("Jean");
        testClientDto.setCin("AB123456");
        testClientDto.setTelephone("0123456789");
        testClientDto.setTelephone2("0987654321");
        testClientDto.setNMDIR("DIR001");
        testClientDto.setNMREG("REG001");
        testClientDto.setNMBRA(Branche.CASA_AZHAR);
        testClientDto.setActiviteActuelle("Professeur");
        testClientDto.setBAREM("BAREME001");
        testClientDto.setMNTDEB(50000.0);
        testClientDto.setNBINC(2);
        testClientDto.setNBPRETS(1);
        testClientDto.setAgeClient(35);
        testClientDto.setStatus(ClientStatus.CONTACTE);
        testClientDto.setRaisonNonRenouvellement(RaisonNonRenouvellement.TAUX_INTERET_ELEVE);
        testClientDto.setQualiteService(QualiteService.EXCELLENTE);
        testClientDto.setActiviteClient(ActiviteClient.SALARIE);
        testClientDto.setProfil(Profil.AMBULANT);
        testClientDto.setADifficultesRencontrees(false);
        testClientDto.setInteretNouveauCredit(InteretCredit.OUI);
        testClientDto.setRendezVousAgence(true);
        testClientDto.setFacteurInfluence(FacteurInfluence.SERVICE_CLIENT);
    }

    @Test
    void testFindAll() {
        // Given
        List<Client> expectedClients = Arrays.asList(testClient);
        when(clientRepository.findAll()).thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findAll();

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findAll();
    }

    @Test
    void testFindContactedOnDate() {
        // Given
        LocalDate testDate = LocalDate.now();
        LocalDateTime start = testDate.atStartOfDay();
        LocalDateTime end = testDate.atTime(LocalTime.MAX);
        List<Client> expectedClients = Arrays.asList(testClient);

        when(clientRepository.findByStatusAndUpdatedAtBetweenOrderByUpdatedAtDesc(
                ClientStatus.CONTACTE, start, end)).thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findContactedOnDate(testDate);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByStatusAndUpdatedAtBetweenOrderByUpdatedAtDesc(
                ClientStatus.CONTACTE, start, end);
    }

    @Test
    void testGetById_Success() {
        // Given
        Long clientId = 1L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));

        // When
        Client result = clientService.getById(clientId);

        // Then
        assertEquals(testClient, result);
        verify(clientRepository).findById(clientId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        Long clientId = 999L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> clientService.getById(clientId));
        assertEquals("Client non trouvé avec l'ID: 999", exception.getMessage());
        verify(clientRepository).findById(clientId);
    }

    @Test
    void testCreate() {
        // Given
        String userEmail = "test@example.com";
        Client newClient = new Client();
        // Simuler la création avec un ID généré
        newClient.setId(1L);
        newClient.setNom("Dupont");
        newClient.setPrenom("Jean");
        newClient.setStatus(ClientStatus.NON_TRAITE);
        newClient.setCreatedBy(testUser);
        newClient.setUpdatedBy(testUser);

        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client client = invocation.getArgument(0);
            // Simuler que l'ID est généré et que les champs sont bien copiés
            client.setId(1L);
            return client;
        });
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Client result = clientService.create(testClientDto, userEmail);

        // Then
        assertNotNull(result);
        verify(userRepository).findByEmail(userEmail);
        verify(clientRepository).save(any(Client.class));
        verify(auditService).auditEvent(
                eq(AuditType.CLIENT_CREATED),
                eq("Client"),
                any(),
                contains("Client créé"),
                eq(userEmail)
        );
    }

    @Test
    void testUpdateStatus() {
        // Given
        Long clientId = 1L;
        ClientStatus oldStatus = ClientStatus.NON_TRAITE;
        ClientStatus newStatus = ClientStatus.CONTACTE;
        String notes = "Client contacté avec succès";
        String userEmail = "test@example.com";

        // Créer une copie du client avec le nouveau statut pour le retour du save
        Client updatedClient = new Client();
        updatedClient.setId(testClient.getId());
        updatedClient.setNom(testClient.getNom());
        updatedClient.setPrenom(testClient.getPrenom());
        updatedClient.setStatus(newStatus);
        updatedClient.setNotes(notes);
        updatedClient.setUpdatedBy(testUser);
        updatedClient.setUpdatedAt(LocalDateTime.now());

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenReturn(updatedClient);
        doNothing().when(rappelRepository).completeAllRappelsForClient(clientId);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Client result = clientService.updateStatus(clientId, newStatus, notes, userEmail);

        // Then
        assertNotNull(result);
        verify(clientRepository).findById(clientId);
        verify(userRepository).findByEmail(userEmail);
        verify(clientRepository).save(any(Client.class));
        verify(rappelRepository).completeAllRappelsForClient(clientId);
        verify(auditService).auditEvent(
                eq(AuditType.CLIENT_STATUS_CHANGE),
                eq("Client"),
                eq(clientId),
                contains("Statut modifié"),
                eq(userEmail)
        );
    }

    @Test
    void testUpdateStatusAndPhone() {
        // Given
        Long clientId = 1L;
        ClientStatus newStatus = ClientStatus.CONTACTE;
        String notes = "Client contacté";
        String newTelephone = "0987654321";
        String userEmail = "test@example.com";

        // Créer une copie mise à jour pour le retour du save
        Client updatedClient = new Client();
        updatedClient.setId(testClient.getId());
        updatedClient.setNom(testClient.getNom());
        updatedClient.setPrenom(testClient.getPrenom());
        updatedClient.setStatus(newStatus);
        updatedClient.setTelephone(newTelephone);
        updatedClient.setNotes(notes);
        updatedClient.setUpdatedBy(testUser);
        updatedClient.setNMBRA(testClient.getNMBRA());

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenReturn(updatedClient);
        doNothing().when(rappelRepository).completeAllRappelsForClient(clientId);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Client result = clientService.updateStatusAndPhone(clientId, newStatus, notes, newTelephone, userEmail);

        // Then
        assertNotNull(result);
        verify(clientRepository).findById(clientId);
        verify(userRepository).findByEmail(userEmail);
        verify(clientRepository).save(any(Client.class));
        verify(rappelRepository).completeAllRappelsForClient(clientId);
    }

    @Test
    void testUpdateClientAndQuestionnaire() {
        // Given
        Long clientId = 1L;
        String userEmail = "test@example.com";

        // Créer une copie mise à jour pour le retour du save
        Client updatedClient = new Client();
        updatedClient.setId(testClient.getId());
        updatedClient.setNom(testClient.getNom());
        updatedClient.setPrenom(testClient.getPrenom());
        updatedClient.setStatus(testClientDto.getStatus());
        updatedClient.setRaisonNonRenouvellement(testClientDto.getRaisonNonRenouvellement());
        updatedClient.setQualiteService(testClientDto.getQualiteService());
        updatedClient.setUpdatedBy(testUser);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenReturn(updatedClient);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Client result = clientService.updateClientAndQuestionnaire(clientId, testClientDto, userEmail);

        // Then
        assertNotNull(result);
        verify(clientRepository).findById(clientId);
        verify(userRepository).findByEmail(userEmail);
        verify(clientRepository).save(any(Client.class));
        verify(auditService).auditEvent(
                eq(AuditType.CLIENT_QUESTIONNAIRE_UPDATED),
                eq("Client"),
                eq(clientId),
                contains("Questionnaire modifié"),
                eq(userEmail)
        );
    }

    @Test
    void testFindClientsByUserAndStatus() {
        // Given
        ClientStatus status = ClientStatus.NON_TRAITE;
        List<Client> expectedClients = Arrays.asList(testClient);

        when(clientRepository.findByAssignedUserAndStatusOrderByUpdatedAtDesc(testUser, status))
                .thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findClientsByUserAndStatus(testUser, status);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByAssignedUserAndStatusOrderByUpdatedAtDesc(testUser, status);
    }

    @Test
    void testFindByUser() {
        // Given
        List<Client> expectedClients = Arrays.asList(testClient);
        when(clientRepository.findByAssignedUserOrderByUpdatedAtDesc(testUser))
                .thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findByUser(testUser);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByAssignedUserOrderByUpdatedAtDesc(testUser);
    }

    @Test
    void testFindUnassignedClients() {
        // Given
        List<Client> expectedClients = Arrays.asList(testClient);
        when(clientRepository.findByAssignedUserIsNullOrderByUpdatedAtDesc())
                .thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findUnassignedClients();

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByAssignedUserIsNullOrderByUpdatedAtDesc();
    }

    @Test
    void testFindByStatus() {
        // Given
        ClientStatus status = ClientStatus.CONTACTE;
        List<Client> expectedClients = Arrays.asList(testClient);
        when(clientRepository.findByStatusOrderByUpdatedAtDesc(status))
                .thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findByStatus(status);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByStatusOrderByUpdatedAtDesc(status);
    }

    @Test
    void testFindWithRendezVousOnDate() {
        // Given
        LocalDate testDate = LocalDate.now();
        LocalDateTime start = testDate.atStartOfDay();
        LocalDateTime end = testDate.atTime(LocalTime.MAX);
        List<Client> expectedClients = Arrays.asList(testClient);

        when(clientRepository.findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenOrderByUpdatedAtDesc(start, end))
                .thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findWithRendezVousOnDate(testDate);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenOrderByUpdatedAtDesc(start, end);
    }

    @Test
    void testFindClientsWithRendezVousForDateAndBranche_WithBranche() {
        // Given
        LocalDate testDate = LocalDate.now();
        Branche branche = Branche.CASA_AZHAR;
        LocalDateTime startOfDay = testDate.atStartOfDay();
        LocalDateTime endOfDay = testDate.atTime(LocalTime.MAX);
        List<Client> expectedClients = Arrays.asList(testClient);

        when(clientRepository.findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenAndNMBRAOrderByUpdatedAtDesc(
                startOfDay, endOfDay, branche)).thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findClientsWithRendezVousForDateAndBranche(testDate, branche);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenAndNMBRAOrderByUpdatedAtDesc(
                startOfDay, endOfDay, branche);
    }

    @Test
    void testFindClientsWithRendezVousForDateAndBranche_WithoutBranche() {
        // Given
        LocalDate testDate = LocalDate.now();
        LocalDateTime startOfDay = testDate.atStartOfDay();
        LocalDateTime endOfDay = testDate.atTime(LocalTime.MAX);
        List<Client> expectedClients = Arrays.asList(testClient);

        when(clientRepository.findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenOrderByUpdatedAtDesc(
                startOfDay, endOfDay)).thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findClientsWithRendezVousForDateAndBranche(testDate, null);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenOrderByUpdatedAtDesc(
                startOfDay, endOfDay);
    }

    @Test
    void testSearchClients() {
        // Given
        String query = "Dupont";
        List<Client> expectedClients = Arrays.asList(testClient);
        when(clientRepository.searchByNomOrPrenomOrCin(query)).thenReturn(expectedClients);

        // When
        List<Client> result = clientService.searchClients(query);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).searchByNomOrPrenomOrCin(query);
    }

    @Test
    void testFindByStatusAndRegions() {
        // Given
        ClientStatus status = ClientStatus.CONTACTE;
        List<String> regionCodes = Arrays.asList("REG001", "REG002");
        List<Client> expectedClients = Arrays.asList(testClient);

        when(clientRepository.findByStatusAndNMREGInOrderByUpdatedAtDesc(status, regionCodes))
                .thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findByStatusAndRegions(status, regionCodes);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByStatusAndNMREGInOrderByUpdatedAtDesc(status, regionCodes);
    }

    @Test
    void testFindUnassignedClientsByRegions() {
        // Given
        List<String> regionCodes = Arrays.asList("REG001", "REG002");
        List<Client> expectedClients = Arrays.asList(testClient);

        when(clientRepository.findByAssignedUserIsNullAndNMREGInOrderByUpdatedAtDesc(regionCodes))
                .thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findUnassignedClientsByRegions(regionCodes);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByAssignedUserIsNullAndNMREGInOrderByUpdatedAtDesc(regionCodes);
    }

    @Test
    void testFilterClientsByDirection() {
        // Given
        String directionCode = "DIR001";
        testClient.setNMDIR(directionCode);
        List<Client> allClients = Arrays.asList(testClient);

        when(clientRepository.findAll()).thenReturn(allClients);

        // When
        List<Client> result = clientService.filterClientsByDirection(directionCode);

        // Then
        assertEquals(1, result.size());
        assertEquals(testClient, result.get(0));
        verify(clientRepository).findAll();
    }

    @Test
    void testFilterClientsByDirection_NullCode() {
        // When
        List<Client> result = clientService.filterClientsByDirection(null);

        // Then
        assertTrue(result.isEmpty());
        verify(clientRepository, never()).findAll();
    }

    @Test
    void testFindClientsByDirectionAndStatus() {
        // Given
        String directionCode = "DIR001";
        ClientStatus status = ClientStatus.CONTACTE;
        List<Client> expectedClients = Arrays.asList(testClient);

        when(clientRepository.findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, status))
                .thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findClientsByDirectionAndStatus(directionCode, status);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findByNMDIRAndStatusOrderByUpdatedAtDesc(directionCode, status);
    }

    @Test
    void testFindClientsByDirectionAndStatus_NullDirection() {
        // Given
        ClientStatus status = ClientStatus.CONTACTE;

        // When
        List<Client> result = clientService.findClientsByDirectionAndStatus(null, status);

        // Then
        assertTrue(result.isEmpty());
        verify(clientRepository, never()).findByNMDIRAndStatusOrderByUpdatedAtDesc(any(), any());
    }

    @Test
    void testFindClientsWithPhoneChanges() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<Client> expectedClients = Arrays.asList(testClient);

        when(clientRepository.findClientsWithPhoneChanges(startDate, endDate))
                .thenReturn(expectedClients);

        // When
        List<Client> result = clientService.findClientsWithPhoneChanges(startDate, endDate);

        // Then
        assertEquals(expectedClients, result);
        verify(clientRepository).findClientsWithPhoneChanges(startDate, endDate);
    }

    @Test
    void testConvertToDto() {
        // When
        ClientDto result = clientService.convertToDto(testClient);

        // Then
        assertNotNull(result);
        assertEquals(testClient.getNom(), result.getNom());
        assertEquals(testClient.getPrenom(), result.getPrenom());
        assertEquals(testClient.getCin(), result.getCin());
        assertEquals(testClient.getTelephone(), result.getTelephone());
        assertEquals(testClient.getStatus(), result.getStatus());
    }

    @Test
    void testUpdateStatus_WithContacteStatus_ShouldCompleteRappels() {
        // Given
        Long clientId = 1L;
        ClientStatus newStatus = ClientStatus.CONTACTE;
        String notes = "Client contacté";
        String userEmail = "test@example.com";

        // Créer une copie mise à jour
        Client updatedClient = new Client();
        updatedClient.setId(testClient.getId());
        updatedClient.setNom(testClient.getNom());
        updatedClient.setPrenom(testClient.getPrenom());
        updatedClient.setStatus(newStatus);
        updatedClient.setNotes(notes);
        updatedClient.setUpdatedBy(testUser);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenReturn(updatedClient);
        doNothing().when(rappelRepository).completeAllRappelsForClient(clientId);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        clientService.updateStatus(clientId, newStatus, notes, userEmail);

        // Then
        verify(rappelRepository).completeAllRappelsForClient(clientId);
    }

    @Test
    void testUpdateStatus_WithNonContacteStatus_ShouldNotCompleteRappels() {
        // Given
        Long clientId = 1L;
        ClientStatus newStatus = ClientStatus.REFUS;
        String notes = "Client a refusé";
        String userEmail = "test@example.com";

        // Créer une copie mise à jour
        Client updatedClient = new Client();
        updatedClient.setId(testClient.getId());
        updatedClient.setNom(testClient.getNom());
        updatedClient.setPrenom(testClient.getPrenom());
        updatedClient.setStatus(newStatus);
        updatedClient.setNotes(notes);
        updatedClient.setUpdatedBy(testUser);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenReturn(updatedClient);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        clientService.updateStatus(clientId, newStatus, notes, userEmail);

        // Then
        verify(rappelRepository, never()).completeAllRappelsForClient(clientId);
    }
}