package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.Audit;
import com.example.projetpfe.entity.AuditType;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.AuditRepository;
import com.example.projetpfe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuditService auditService;

    private User testUser;
    private Audit testAudit;

    @BeforeEach
    void setUp() {
        // Initialiser l'utilisateur de test
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test");

        // Initialiser l'audit de test
        testAudit = new Audit();
        testAudit.setId(1L);
        testAudit.setType(AuditType.CLIENT_CREATED);
        testAudit.setEntityType("Client");
        testAudit.setEntityId(100L);
        testAudit.setDetails("Client créé: Jean Dupont");
        testAudit.setUser(testUser);
        testAudit.setTimestamp(LocalDateTime.now());

        // Nettoyer le contexte de sécurité
        SecurityContextHolder.clearContext();
    }

    // ===============================
    // Tests pour auditEvent() avec utilisateur actuel
    // ===============================

    @Test
    void testAuditEvent_WithCurrentUser_Success() {
        // Given
        String entityType = "Client";
        Long entityId = 100L;
        String details = "Client créé: Jean Dupont";
        AuditType type = AuditType.CLIENT_CREATED;

        mockSecurityContext("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(auditRepository.save(any(Audit.class))).thenReturn(testAudit);

        // When
        Audit result = auditService.auditEvent(type, entityType, entityId, details);

        // Then
        assertNotNull(result);
        assertEquals(testAudit.getId(), result.getId());
        assertEquals(testAudit.getType(), result.getType());
        assertEquals(testAudit.getEntityType(), result.getEntityType());
        assertEquals(testAudit.getEntityId(), result.getEntityId());
        assertEquals(testAudit.getDetails(), result.getDetails());
        assertEquals(testAudit.getUser(), result.getUser());

        verify(userRepository).findByEmail("test@example.com");
        verify(auditRepository).save(any(Audit.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testAuditEvent_WithCurrentUser_NoAuthentication() {
        // Given - Pas d'authentification
        String entityType = "Client";
        Long entityId = 100L;
        String details = "Client créé: Jean Dupont";
        AuditType type = AuditType.CLIENT_CREATED;

        Audit auditWithoutUser = new Audit();
        auditWithoutUser.setId(1L);
        auditWithoutUser.setType(type);
        auditWithoutUser.setEntityType(entityType);
        auditWithoutUser.setEntityId(entityId);
        auditWithoutUser.setDetails(details);
        auditWithoutUser.setUser(null);
        auditWithoutUser.setTimestamp(LocalDateTime.now());

        when(auditRepository.save(any(Audit.class))).thenReturn(auditWithoutUser);

        // When
        Audit result = auditService.auditEvent(type, entityType, entityId, details);

        // Then
        assertNotNull(result);
        assertNull(result.getUser());
        assertEquals(type, result.getType());
        assertEquals(entityType, result.getEntityType());
        assertEquals(entityId, result.getEntityId());
        assertEquals(details, result.getDetails());

        verify(auditRepository).save(any(Audit.class));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void testAuditEvent_WithCurrentUser_AnonymousUser() {
        // Given
        String entityType = "Client";
        Long entityId = 100L;
        String details = "Client créé: Jean Dupont";
        AuditType type = AuditType.CLIENT_CREATED;

        mockSecurityContext("anonymousUser");

        Audit auditWithoutUser = new Audit();
        auditWithoutUser.setId(1L);
        auditWithoutUser.setType(type);
        auditWithoutUser.setEntityType(entityType);
        auditWithoutUser.setEntityId(entityId);
        auditWithoutUser.setDetails(details);
        auditWithoutUser.setUser(null);
        auditWithoutUser.setTimestamp(LocalDateTime.now());

        when(auditRepository.save(any(Audit.class))).thenReturn(auditWithoutUser);

        // When
        Audit result = auditService.auditEvent(type, entityType, entityId, details);

        // Then
        assertNotNull(result);
        assertNull(result.getUser());
        verify(auditRepository).save(any(Audit.class));
        verify(userRepository, never()).findByEmail(any());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testAuditEvent_WithCurrentUser_ExceptionHandling() {
        // Given
        String entityType = "Client";
        Long entityId = 100L;
        String details = "Client créé: Jean Dupont";
        AuditType type = AuditType.CLIENT_CREATED;

        mockSecurityContext("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(auditRepository.save(any(Audit.class))).thenThrow(new RuntimeException("Database error"));

        // When
        Audit result = auditService.auditEvent(type, entityType, entityId, details);

        // Then
        assertNull(result); // En cas d'erreur, retourne null
        verify(userRepository).findByEmail("test@example.com");
        verify(auditRepository).save(any(Audit.class));

        SecurityContextHolder.clearContext();
    }

    // ===============================
    // Tests pour auditEvent() avec email explicite
    // ===============================

    @Test
    void testAuditEvent_WithExplicitUser_Success() {
        // Given
        String entityType = "Client";
        Long entityId = 100L;
        String details = "Client créé: Jean Dupont";
        AuditType type = AuditType.CLIENT_CREATED;
        String userEmail = "test@example.com";

        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);
        when(auditRepository.save(any(Audit.class))).thenReturn(testAudit);

        // When
        Audit result = auditService.auditEvent(type, entityType, entityId, details, userEmail);

        // Then
        assertNotNull(result);
        assertEquals(testAudit.getId(), result.getId());
        assertEquals(testAudit.getType(), result.getType());
        assertEquals(testAudit.getEntityType(), result.getEntityType());
        assertEquals(testAudit.getEntityId(), result.getEntityId());
        assertEquals(testAudit.getDetails(), result.getDetails());
        assertEquals(testAudit.getUser(), result.getUser());

        verify(userRepository).findByEmail(userEmail);
        verify(auditRepository).save(any(Audit.class));
    }

    @Test
    void testAuditEvent_WithExplicitUser_NullEmail() {
        // Given
        String entityType = "Client";
        Long entityId = 100L;
        String details = "Client créé: Jean Dupont";
        AuditType type = AuditType.CLIENT_CREATED;
        String userEmail = null;

        Audit auditWithoutUser = new Audit();
        auditWithoutUser.setId(1L);
        auditWithoutUser.setType(type);
        auditWithoutUser.setEntityType(entityType);
        auditWithoutUser.setEntityId(entityId);
        auditWithoutUser.setDetails(details);
        auditWithoutUser.setUser(null);
        auditWithoutUser.setTimestamp(LocalDateTime.now());

        when(auditRepository.save(any(Audit.class))).thenReturn(auditWithoutUser);

        // When
        Audit result = auditService.auditEvent(type, entityType, entityId, details, userEmail);

        // Then
        assertNotNull(result);
        assertNull(result.getUser());
        assertEquals(type, result.getType());
        assertEquals(entityType, result.getEntityType());
        assertEquals(entityId, result.getEntityId());
        assertEquals(details, result.getDetails());

        verify(auditRepository).save(any(Audit.class));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void testAuditEvent_WithExplicitUser_EmptyEmail() {
        // Given
        String entityType = "Client";
        Long entityId = 100L;
        String details = "Client créé: Jean Dupont";
        AuditType type = AuditType.CLIENT_CREATED;
        String userEmail = "";

        Audit auditWithoutUser = new Audit();
        auditWithoutUser.setId(1L);
        auditWithoutUser.setType(type);
        auditWithoutUser.setEntityType(entityType);
        auditWithoutUser.setEntityId(entityId);
        auditWithoutUser.setDetails(details);
        auditWithoutUser.setUser(null);
        auditWithoutUser.setTimestamp(LocalDateTime.now());

        when(auditRepository.save(any(Audit.class))).thenReturn(auditWithoutUser);

        // When
        Audit result = auditService.auditEvent(type, entityType, entityId, details, userEmail);

        // Then
        assertNotNull(result);
        assertNull(result.getUser());
        verify(auditRepository).save(any(Audit.class));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void testAuditEvent_WithExplicitUser_UserNotFound() {
        // Given
        String entityType = "Client";
        Long entityId = 100L;
        String details = "Client créé: Jean Dupont";
        AuditType type = AuditType.CLIENT_CREATED;
        String userEmail = "nonexistent@example.com";

        when(userRepository.findByEmail(userEmail)).thenReturn(null);

        Audit auditWithoutUser = new Audit();
        auditWithoutUser.setId(1L);
        auditWithoutUser.setType(type);
        auditWithoutUser.setEntityType(entityType);
        auditWithoutUser.setEntityId(entityId);
        auditWithoutUser.setDetails(details);
        auditWithoutUser.setUser(null);
        auditWithoutUser.setTimestamp(LocalDateTime.now());

        when(auditRepository.save(any(Audit.class))).thenReturn(auditWithoutUser);

        // When
        Audit result = auditService.auditEvent(type, entityType, entityId, details, userEmail);

        // Then
        assertNotNull(result);
        assertNull(result.getUser());
        verify(userRepository).findByEmail(userEmail);
        verify(auditRepository).save(any(Audit.class));
    }

    @Test
    void testAuditEvent_WithExplicitUser_ExceptionHandling() {
        // Given
        String entityType = "Client";
        Long entityId = 100L;
        String details = "Client créé: Jean Dupont";
        AuditType type = AuditType.CLIENT_CREATED;
        String userEmail = "test@example.com";

        when(userRepository.findByEmail(userEmail)).thenReturn(testUser);
        when(auditRepository.save(any(Audit.class))).thenThrow(new RuntimeException("Database error"));

        // When
        Audit result = auditService.auditEvent(type, entityType, entityId, details, userEmail);

        // Then
        assertNull(result); // En cas d'erreur, retourne null
        verify(userRepository).findByEmail(userEmail);
        verify(auditRepository).save(any(Audit.class));
    }

    // ===============================
    // Tests pour les méthodes de recherche
    // ===============================

    @Test
    void testFindByUser() {
        // Given
        List<Audit> expectedAudits = Arrays.asList(testAudit);
        when(auditRepository.findByUser(testUser)).thenReturn(expectedAudits);

        // When
        List<Audit> result = auditService.findByUser(testUser);

        // Then
        assertEquals(expectedAudits, result);
        verify(auditRepository).findByUser(testUser);
    }

    @Test
    void testFindByType() {
        // Given
        AuditType type = AuditType.CLIENT_CREATED;
        List<Audit> expectedAudits = Arrays.asList(testAudit);
        when(auditRepository.findByType(type)).thenReturn(expectedAudits);

        // When
        List<Audit> result = auditService.findByType(type);

        // Then
        assertEquals(expectedAudits, result);
        verify(auditRepository).findByType(type);
    }

    @Test
    void testFindByEntity() {
        // Given
        String entityType = "Client";
        Long entityId = 100L;
        List<Audit> expectedAudits = Arrays.asList(testAudit);
        when(auditRepository.findByEntityTypeAndEntityId(entityType, entityId)).thenReturn(expectedAudits);

        // When
        List<Audit> result = auditService.findByEntity(entityType, entityId);

        // Then
        assertEquals(expectedAudits, result);
        verify(auditRepository).findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Test
    void testFindByDateRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<Audit> expectedAudits = Arrays.asList(testAudit);
        when(auditRepository.findByTimestampBetween(start, end)).thenReturn(expectedAudits);

        // When
        List<Audit> result = auditService.findByDateRange(start, end);

        // Then
        assertEquals(expectedAudits, result);
        verify(auditRepository).findByTimestampBetween(start, end);
    }

    @Test
    void testFindByTypeAndDateRange() {
        // Given
        AuditType type = AuditType.CLIENT_CREATED;
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Audit> expectedPage = new PageImpl<>(Arrays.asList(testAudit));

        when(auditRepository.findByTypeAndTimestampBetween(type, start, end, pageable))
                .thenReturn(expectedPage);

        // When
        Page<Audit> result = auditService.findByTypeAndDateRange(type, start, end, pageable);

        // Then
        assertEquals(expectedPage, result);
        verify(auditRepository).findByTypeAndTimestampBetween(type, start, end, pageable);
    }

    @Test
    void testFindAllPaged() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Audit> expectedPage = new PageImpl<>(Arrays.asList(testAudit));

        when(auditRepository.findByTimestampBetween(start, end, pageable)).thenReturn(expectedPage);

        // When
        Page<Audit> result = auditService.findAllPaged(start, end, pageable);

        // Then
        assertEquals(expectedPage, result);
        verify(auditRepository).findByTimestampBetween(start, end, pageable);
    }

    @Test
    void testFindByTimestampBetween() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Audit> expectedPage = new PageImpl<>(Arrays.asList(testAudit));

        when(auditRepository.findByTimestampBetween(start, end, pageable)).thenReturn(expectedPage);

        // When
        Page<Audit> result = auditService.findByTimestampBetween(start, end, pageable);

        // Then
        assertEquals(expectedPage, result);
        verify(auditRepository).findByTimestampBetween(start, end, pageable);
    }

    // ===============================
    // Tests pour les cas limites et edge cases
    // ===============================

    @Test
    void testAuditEvent_WithNullParameters() {
        // Given
        when(auditRepository.save(any(Audit.class))).thenAnswer(invocation -> {
            Audit audit = invocation.getArgument(0);
            audit.setId(1L);
            return audit;
        });

        // When
        Audit result = auditService.auditEvent(null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(auditRepository).save(any(Audit.class));
    }

    @Test
    void testFindByUser_EmptyResult() {
        // Given
        when(auditRepository.findByUser(testUser)).thenReturn(Arrays.asList());

        // When
        List<Audit> result = auditService.findByUser(testUser);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(auditRepository).findByUser(testUser);
    }

    @Test
    void testFindByType_EmptyResult() {
        // Given
        AuditType type = AuditType.CLIENT_DELETED;
        when(auditRepository.findByType(type)).thenReturn(Arrays.asList());

        // When
        List<Audit> result = auditService.findByType(type);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(auditRepository).findByType(type);
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