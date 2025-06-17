package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.RappelRepository;
import com.example.projetpfe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RappelServiceTest {

    @Mock
    private RappelRepository rappelRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RappelService rappelService;

    private User testUser;
    private Client testClient;
    private Rappel testRappel;

    @BeforeEach
    void setUp() {
        // Initialiser l'utilisateur de test
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test");


        // Initialiser le client de test
        testClient = new Client();
        testClient.setId(100L);
        testClient.setNom("Dupont");
        testClient.setPrenom("Jean");
        testClient.setCin("AB123456");
        testClient.setTelephone("0600000000");
        testClient.setNMREG("REG001");
        testClient.setStatus(ClientStatus.NON_TRAITE);
        testClient.setAssignedUser(testUser);

        // Initialiser le rappel de test
        testRappel = new Rappel();
        testRappel.setId(1L);
        testRappel.setClient(testClient);
        testRappel.setDateRappel(LocalDateTime.now().plusDays(1));
        testRappel.setNotes("Rappel de test");
        testRappel.setCreatedBy(testUser);
        testRappel.setCompleted(false);

        // Nettoyer le contexte de sécurité
        SecurityContextHolder.clearContext();
    }

    // ===============================
    // Tests pour createRappel()
    // ===============================

    @Test
    void testCreateRappel_Success() {
        // Given
        Long clientId = 100L;
        LocalDateTime dateRappel = LocalDateTime.now().plusDays(1);
        String notes = "Rappel important";
        String userEmail = "test@example.com";

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);
        when(rappelRepository.save(any(Rappel.class))).thenReturn(testRappel);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Rappel result = rappelService.createRappel(clientId, dateRappel, notes, userEmail);

        // Then
        assertNotNull(result);
        assertEquals(testRappel.getId(), result.getId());
        assertEquals(testRappel.getClient(), result.getClient());
        assertEquals(testRappel.getDateRappel(), result.getDateRappel());
        assertEquals(testRappel.getNotes(), result.getNotes());
        assertEquals(testRappel.getCreatedBy(), result.getCreatedBy());
        assertFalse(result.getCompleted());

        verify(clientRepository).findById(clientId);
        verify(userRepository).findByEmail(userEmail);
        verify(rappelRepository).save(any(Rappel.class));
        verify(auditService).auditEvent(
                eq(AuditType.RAPPEL_CREATED),
                eq("Rappel"),
                eq(testRappel.getId()),
                contains("Rappel créé pour le client"),
                eq(userEmail)
        );
    }

    @Test
    void testCreateRappel_ClientNotFound() {
        // Given
        Long clientId = 999L;
        LocalDateTime dateRappel = LocalDateTime.now().plusDays(1);
        String notes = "Rappel important";
        String userEmail = "test@example.com";

        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rappelService.createRappel(clientId, dateRappel, notes, userEmail));
        assertEquals("Client non trouvé", exception.getMessage());

        verify(clientRepository).findById(clientId);
        verify(userRepository, never()).findByEmail(any());
        verify(rappelRepository, never()).save(any());
        verify(auditService, never()).auditEvent(any(), any(), any(), any(), any());
    }

    @Test
    void testCreateRappel_WithNullUser() {
        // Given
        Long clientId = 100L;
        LocalDateTime dateRappel = LocalDateTime.now().plusDays(1);
        String notes = "Rappel important";
        String userEmail = "nonexistent@example.com";

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail(userEmail)).thenReturn(null);

        Rappel rappelWithoutUser = new Rappel();
        rappelWithoutUser.setId(1L);
        rappelWithoutUser.setClient(testClient);
        rappelWithoutUser.setDateRappel(dateRappel);
        rappelWithoutUser.setNotes(notes);
        rappelWithoutUser.setCreatedBy(null);
        rappelWithoutUser.setCompleted(false);

        when(rappelRepository.save(any(Rappel.class))).thenReturn(rappelWithoutUser);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Rappel result = rappelService.createRappel(clientId, dateRappel, notes, userEmail);

        // Then
        assertNotNull(result);
        assertNull(result.getCreatedBy());
        assertEquals(testClient, result.getClient());
        assertEquals(dateRappel, result.getDateRappel());
        assertEquals(notes, result.getNotes());
        assertFalse(result.getCompleted());

        verify(clientRepository).findById(clientId);
        verify(userRepository).findByEmail(userEmail);
        verify(rappelRepository).save(any(Rappel.class));
        verify(auditService).auditEvent(any(), any(), any(), any(), eq(userEmail));
    }

    // ===============================
    // Tests pour getById()
    // ===============================

    @Test
    void testGetById_Success() {
        // Given
        Long rappelId = 1L;
        when(rappelRepository.findById(rappelId)).thenReturn(Optional.of(testRappel));

        // When
        Rappel result = rappelService.getById(rappelId);

        // Then
        assertEquals(testRappel, result);
        verify(rappelRepository).findById(rappelId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        Long rappelId = 999L;
        when(rappelRepository.findById(rappelId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rappelService.getById(rappelId));
        assertEquals("Rappel non trouvé avec l'ID: 999", exception.getMessage());
        verify(rappelRepository).findById(rappelId);
    }

    // ===============================
    // Tests pour getRappelsForUser()
    // ===============================

    @Test
    void testGetRappelsForUser() {
        // Given
        List<Rappel> expectedRappels = Arrays.asList(testRappel);
        when(rappelRepository.findByClientAssignedUserAndCompletedFalseOrderByDateRappel(testUser))
                .thenReturn(expectedRappels);

        // When
        List<Rappel> result = rappelService.getRappelsForUser(testUser);

        // Then
        assertEquals(expectedRappels, result);
        verify(rappelRepository).findByClientAssignedUserAndCompletedFalseOrderByDateRappel(testUser);
    }

    @Test
    void testGetRappelsForUser_EmptyResult() {
        // Given
        when(rappelRepository.findByClientAssignedUserAndCompletedFalseOrderByDateRappel(testUser))
                .thenReturn(Arrays.asList());

        // When
        List<Rappel> result = rappelService.getRappelsForUser(testUser);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rappelRepository).findByClientAssignedUserAndCompletedFalseOrderByDateRappel(testUser);
    }

    // ===============================
    // Tests pour getAllActiveRappelsByRegions()
    // ===============================

    @Test
    void testGetAllActiveRappelsByRegions() {
        // Given
        List<String> regionCodes = Arrays.asList("REG001", "REG002");
        List<Rappel> expectedRappels = Arrays.asList(testRappel);
        when(rappelRepository.findByCompletedFalseAndClientNMREGInOrderByDateRappel(regionCodes))
                .thenReturn(expectedRappels);

        // When
        List<Rappel> result = rappelService.getAllActiveRappelsByRegions(regionCodes);

        // Then
        assertEquals(expectedRappels, result);
        verify(rappelRepository).findByCompletedFalseAndClientNMREGInOrderByDateRappel(regionCodes);
    }

    @Test
    void testGetAllActiveRappelsByRegions_EmptyRegions() {
        // Given
        List<String> regionCodes = Arrays.asList();
        when(rappelRepository.findByCompletedFalseAndClientNMREGInOrderByDateRappel(regionCodes))
                .thenReturn(Arrays.asList());

        // When
        List<Rappel> result = rappelService.getAllActiveRappelsByRegions(regionCodes);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rappelRepository).findByCompletedFalseAndClientNMREGInOrderByDateRappel(regionCodes);
    }

    // ===============================
    // Tests pour completeRappel()
    // ===============================

    @Test
    void testCompleteRappel_Success() {
        // Given
        Long rappelId = 1L;
        mockSecurityContext("test@example.com");

        Rappel completedRappel = new Rappel();
        completedRappel.setId(testRappel.getId());
        completedRappel.setClient(testRappel.getClient());
        completedRappel.setDateRappel(testRappel.getDateRappel());
        completedRappel.setNotes(testRappel.getNotes());
        completedRappel.setCreatedBy(testRappel.getCreatedBy());
        completedRappel.setCompleted(true);

        when(rappelRepository.findById(rappelId)).thenReturn(Optional.of(testRappel));
        when(rappelRepository.save(any(Rappel.class))).thenReturn(completedRappel);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Rappel result = rappelService.completeRappel(rappelId);

        // Then
        assertNotNull(result);
        assertTrue(result.getCompleted());
        assertEquals(testRappel.getId(), result.getId());
        assertEquals(testRappel.getClient(), result.getClient());

        verify(rappelRepository).findById(rappelId);
        verify(rappelRepository).save(any(Rappel.class));
        verify(auditService).auditEvent(
                eq(AuditType.RAPPEL_COMPLETED),
                eq("Rappel"),
                eq(testRappel.getId()),
                contains("Rappel marqué comme terminé"),
                eq("test@example.com")
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCompleteRappel_NotFound() {
        // Given
        Long rappelId = 999L;
        when(rappelRepository.findById(rappelId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rappelService.completeRappel(rappelId));
        assertEquals("Rappel non trouvé", exception.getMessage());

        verify(rappelRepository).findById(rappelId);
        verify(rappelRepository, never()).save(any());
        verify(auditService, never()).auditEvent(any(), any(), any(), any(), any());
    }

    @Test
    void testCompleteRappel_WithAnonymousUser() {
        // Given
        Long rappelId = 1L;
        // Mock d'un utilisateur anonyme
        mockSecurityContext("anonymousUser");

        Rappel completedRappel = new Rappel();
        completedRappel.setId(testRappel.getId());
        completedRappel.setClient(testRappel.getClient());
        completedRappel.setDateRappel(testRappel.getDateRappel());
        completedRappel.setNotes(testRappel.getNotes());
        completedRappel.setCreatedBy(testRappel.getCreatedBy());
        completedRappel.setCompleted(true);

        when(rappelRepository.findById(rappelId)).thenReturn(Optional.of(testRappel));
        when(rappelRepository.save(any(Rappel.class))).thenReturn(completedRappel);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Rappel result = rappelService.completeRappel(rappelId);

        // Then
        assertNotNull(result);
        assertTrue(result.getCompleted());

        verify(rappelRepository).findById(rappelId);
        verify(rappelRepository).save(any(Rappel.class));
        verify(auditService).auditEvent(
                eq(AuditType.RAPPEL_COMPLETED),
                eq("Rappel"),
                eq(testRappel.getId()),
                contains("Rappel marqué comme terminé"),
                eq("anonymousUser")
        );

        SecurityContextHolder.clearContext();
    }

    // ===============================
    // Tests pour countTodayRappels()
    // ===============================

    @Test
    void testCountTodayRappels() {
        // Given
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long expectedCount = 3L;

        when(rappelRepository.countByClientAssignedUserAndDateRappelBetweenAndCompletedFalse(
                testUser, startOfDay, endOfDay)).thenReturn(expectedCount);

        // When
        long result = rappelService.countTodayRappels(testUser);

        // Then
        assertEquals(expectedCount, result);
        verify(rappelRepository).countByClientAssignedUserAndDateRappelBetweenAndCompletedFalse(
                testUser, startOfDay, endOfDay);
    }

    @Test
    void testCountTodayRappels_NoRappels() {
        // Given
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        when(rappelRepository.countByClientAssignedUserAndDateRappelBetweenAndCompletedFalse(
                testUser, startOfDay, endOfDay)).thenReturn(0L);

        // When
        long result = rappelService.countTodayRappels(testUser);

        // Then
        assertEquals(0L, result);
        verify(rappelRepository).countByClientAssignedUserAndDateRappelBetweenAndCompletedFalse(
                testUser, startOfDay, endOfDay);
    }

    // ===============================
    // Tests pour findTodayRappelsForUser()
    // ===============================

    @Test
    void testFindTodayRappelsForUser() {
        // Given
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        List<Rappel> expectedRappels = Arrays.asList(testRappel);

        when(rappelRepository.findByClientAssignedUserAndDateRappelBetweenAndCompletedFalseOrderByDateRappel(
                testUser, startOfDay, endOfDay)).thenReturn(expectedRappels);

        // When
        List<Rappel> result = rappelService.findTodayRappelsForUser(testUser);

        // Then
        assertEquals(expectedRappels, result);
        verify(rappelRepository).findByClientAssignedUserAndDateRappelBetweenAndCompletedFalseOrderByDateRappel(
                testUser, startOfDay, endOfDay);
    }

    @Test
    void testFindTodayRappelsForUser_EmptyResult() {
        // Given
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        when(rappelRepository.findByClientAssignedUserAndDateRappelBetweenAndCompletedFalseOrderByDateRappel(
                testUser, startOfDay, endOfDay)).thenReturn(Arrays.asList());

        // When
        List<Rappel> result = rappelService.findTodayRappelsForUser(testUser);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rappelRepository).findByClientAssignedUserAndDateRappelBetweenAndCompletedFalseOrderByDateRappel(
                testUser, startOfDay, endOfDay);
    }

    // ===============================
    // Tests pour findByDate()
    // ===============================

    @Test
    void testFindByDate() {
        // Given
        LocalDate testDate = LocalDate.now().plusDays(1);
        LocalDateTime start = testDate.atStartOfDay();
        LocalDateTime end = testDate.atTime(LocalTime.MAX);
        List<Rappel> expectedRappels = Arrays.asList(testRappel);

        when(rappelRepository.findByDateRappelBetweenAndCompletedFalse(start, end))
                .thenReturn(expectedRappels);

        // When
        List<Rappel> result = rappelService.findByDate(testDate);

        // Then
        assertEquals(expectedRappels, result);
        verify(rappelRepository).findByDateRappelBetweenAndCompletedFalse(start, end);
    }

    @Test
    void testFindByDate_EmptyResult() {
        // Given
        LocalDate testDate = LocalDate.now().plusDays(1);
        LocalDateTime start = testDate.atStartOfDay();
        LocalDateTime end = testDate.atTime(LocalTime.MAX);

        when(rappelRepository.findByDateRappelBetweenAndCompletedFalse(start, end))
                .thenReturn(Arrays.asList());

        // When
        List<Rappel> result = rappelService.findByDate(testDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rappelRepository).findByDateRappelBetweenAndCompletedFalse(start, end);
    }

    // ===============================
    // Tests pour getAllActiveRappels()
    // ===============================

    @Test
    void testGetAllActiveRappels() {
        // Given
        List<Rappel> expectedRappels = Arrays.asList(testRappel);
        when(rappelRepository.findByCompletedFalseOrderByDateRappel()).thenReturn(expectedRappels);

        // When
        List<Rappel> result = rappelService.getAllActiveRappels();

        // Then
        assertEquals(expectedRappels, result);
        verify(rappelRepository).findByCompletedFalseOrderByDateRappel();
    }

    @Test
    void testGetAllActiveRappels_EmptyResult() {
        // Given
        when(rappelRepository.findByCompletedFalseOrderByDateRappel()).thenReturn(Arrays.asList());

        // When
        List<Rappel> result = rappelService.getAllActiveRappels();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rappelRepository).findByCompletedFalseOrderByDateRappel();
    }

    // ===============================
    // Tests des cas limites et edge cases
    // ===============================

    @Test
    void testCreateRappel_WithNullNotes() {
        // Given
        Long clientId = 100L;
        LocalDateTime dateRappel = LocalDateTime.now().plusDays(1);
        String notes = null;
        String userEmail = "test@example.com";

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);

        Rappel rappelWithNullNotes = new Rappel();
        rappelWithNullNotes.setId(1L);
        rappelWithNullNotes.setClient(testClient);
        rappelWithNullNotes.setDateRappel(dateRappel);
        rappelWithNullNotes.setNotes(null);
        rappelWithNullNotes.setCreatedBy(testUser);
        rappelWithNullNotes.setCompleted(false);

        when(rappelRepository.save(any(Rappel.class))).thenReturn(rappelWithNullNotes);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Rappel result = rappelService.createRappel(clientId, dateRappel, notes, userEmail);

        // Then
        assertNotNull(result);
        assertNull(result.getNotes());
        assertEquals(testClient, result.getClient());
        assertEquals(dateRappel, result.getDateRappel());
        assertEquals(testUser, result.getCreatedBy());
        assertFalse(result.getCompleted());

        verify(clientRepository).findById(clientId);
        verify(userRepository).findByEmail(userEmail);
        verify(rappelRepository).save(any(Rappel.class));
        verify(auditService).auditEvent(any(), any(), any(), any(), eq(userEmail));
    }

    @Test
    void testCreateRappel_WithPastDate() {
        // Given
        Long clientId = 100L;
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        String notes = "Rappel en retard";
        String userEmail = "test@example.com";

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);

        Rappel pastRappel = new Rappel();
        pastRappel.setId(1L);
        pastRappel.setClient(testClient);
        pastRappel.setDateRappel(pastDate);
        pastRappel.setNotes(notes);
        pastRappel.setCreatedBy(testUser);
        pastRappel.setCompleted(false);

        when(rappelRepository.save(any(Rappel.class))).thenReturn(pastRappel);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Rappel result = rappelService.createRappel(clientId, pastDate, notes, userEmail);

        // Then
        assertNotNull(result);
        assertEquals(pastDate, result.getDateRappel());
        assertTrue(result.getDateRappel().isBefore(LocalDateTime.now()));
        assertEquals(testClient, result.getClient());
        assertEquals(testUser, result.getCreatedBy());
        assertFalse(result.getCompleted());

        verify(clientRepository).findById(clientId);
        verify(userRepository).findByEmail(userEmail);
        verify(rappelRepository).save(any(Rappel.class));
        verify(auditService).auditEvent(any(), any(), any(), any(), eq(userEmail));
    }

    @Test
    void testCompleteRappel_AlreadyCompleted() {
        // Given
        Long rappelId = 1L;
        mockSecurityContext("test@example.com");

        Rappel alreadyCompletedRappel = new Rappel();
        alreadyCompletedRappel.setId(1L);
        alreadyCompletedRappel.setClient(testClient);
        alreadyCompletedRappel.setDateRappel(LocalDateTime.now().plusDays(1));
        alreadyCompletedRappel.setNotes("Rappel déjà complété");
        alreadyCompletedRappel.setCreatedBy(testUser);
        alreadyCompletedRappel.setCompleted(true);

        when(rappelRepository.findById(rappelId)).thenReturn(Optional.of(alreadyCompletedRappel));
        when(rappelRepository.save(any(Rappel.class))).thenReturn(alreadyCompletedRappel);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        Rappel result = rappelService.completeRappel(rappelId);

        // Then
        assertNotNull(result);
        assertTrue(result.getCompleted()); // Reste completed
        assertEquals(alreadyCompletedRappel.getId(), result.getId());

        verify(rappelRepository).findById(rappelId);
        verify(rappelRepository).save(any(Rappel.class));
        verify(auditService).auditEvent(
                eq(AuditType.RAPPEL_COMPLETED),
                eq("Rappel"),
                eq(alreadyCompletedRappel.getId()),
                contains("Rappel marqué comme terminé"),
                eq("test@example.com")
        );

        SecurityContextHolder.clearContext();
    }

    // ===============================
    // Méthodes utilitaires
    // ===============================

    private void mockSecurityContext(String username) {
        when(authentication.getName()).thenReturn(username);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}