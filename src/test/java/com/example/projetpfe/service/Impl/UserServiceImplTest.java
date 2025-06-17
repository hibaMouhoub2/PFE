package com.example.projetpfe.service.Impl;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.DirectionRepository;
import com.example.projetpfe.repository.RegionRepository;
import com.example.projetpfe.repository.RoleRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DirectionRepository directionRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private User testAdmin;
    private User testSuperAdmin;
    private UserDto testUserDto;
    private Role userRole;
    private Role adminRole;
    private Role superAdminRole;
    private Region testRegion;
    private Direction testDirection;

    @BeforeEach
    void setUp() {
        // Configuration des rôles
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ROLE_ADMIN");

        superAdminRole = new Role();
        superAdminRole.setId(3L);
        superAdminRole.setName("ROLE_SUPER_ADMIN");

        // Configuration de la région de test
        testRegion = new Region();
        testRegion.setId(1L);
        testRegion.setName("Casablanca");
        testRegion.setCode("CASA");

        // Configuration de la direction de test
        testDirection = new Direction();
        testDirection.setId(1L);
        testDirection.setName("Direction Commerciale");
        testDirection.setCode("DIR001");

        // Configuration de l'utilisateur normal
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Jean Dupont");
        testUser.setEmail("jean.dupont@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setRoles(new ArrayList<>(Arrays.asList(userRole))); // Liste mutable


        // Configuration de l'admin régional
        testAdmin = new User();
        testAdmin.setId(2L);
        testAdmin.setName("Admin Regional");
        testAdmin.setEmail("admin.regional@example.com");
        testAdmin.setPassword("encodedPassword");
        testAdmin.setEnabled(true);
        testAdmin.setCreatedAt(LocalDateTime.now());
        testAdmin.setRoles(new ArrayList<>(Arrays.asList(adminRole))); // Liste mutable
        testAdmin.setRegions(new ArrayList<>(Arrays.asList(testRegion))); // Liste mutable

        // Configuration du super admin
        testSuperAdmin = new User();
        testSuperAdmin.setId(3L);
        testSuperAdmin.setName("Super Admin");
        testSuperAdmin.setEmail("super.admin@example.com");
        testSuperAdmin.setPassword("encodedPassword");
        testSuperAdmin.setEnabled(true);
        testSuperAdmin.setCreatedAt(LocalDateTime.now());
        testSuperAdmin.setRoles(new ArrayList<>(Arrays.asList(superAdminRole))); // Liste mutable

        // Configuration du DTO de test
        testUserDto = new UserDto();
        testUserDto.setFirstName("Jean");
        testUserDto.setLastName("Dupont");
        testUserDto.setEmail("jean.dupont@example.com");
        testUserDto.setPassword("password123");
        testUserDto.setEnabled(true);
        testUserDto.setRegionId(1L);
        testUserDto.setRegionIds(new ArrayList<>());
    }

    // Méthode helper pour mocker le SecurityContext
    private void mockSecurityContext(String email) {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(email);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testFindByEmail() {
        // Given
        String email = "jean.dupont@example.com";
        when(userRepository.findByEmail(email)).thenReturn(testUser);

        // When
        User result = userService.findByEmail(email);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testFindById_Success() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.findById(userId);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        verify(userRepository).findById(userId);
    }

    @Test
    void testFindById_NotFound() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.findById(userId));
        assertEquals("Utilisateur non trouvé avec l'ID: 999", exception.getMessage());
        verify(userRepository).findById(userId);
    }

    @Test
    void testFindUserById() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.findUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals("Jean", result.getFirstName());
        assertEquals("Dupont", result.getLastName());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    void testFindAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser, testAdmin);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserDto> result = userService.findAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Jean", result.get(0).getFirstName());
        verify(userRepository).findAll();
    }

    @Test
    void testSaveUser_WithUserRole() {
        // Given
        String roleName = "USER";
        mockSecurityContext("admin@example.com");

        when(roleRepository.findByName("ROLE_USER")).thenReturn(userRole);
        when(regionRepository.findById(1L)).thenReturn(Optional.of(testRegion));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        userService.saveUser(testUserDto, roleName);

        // Then
        verify(userRepository).save(any(User.class));
        verify(auditService).auditEvent(
                eq(AuditType.USER_CREATED),
                eq("User"),
                any(),
                contains("Utilisateur créé"),
                any()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveUser_WithAdminRole() {
        // Given
        String roleName = "ADMIN";
        testUserDto.setRegionIds(Arrays.asList(1L));
        mockSecurityContext("admin@example.com");

        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(adminRole);
        when(regionRepository.findAllById(Arrays.asList(1L))).thenReturn(Arrays.asList(testRegion));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testAdmin);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        userService.saveUser(testUserDto, roleName);

        // Then
        verify(userRepository).save(any(User.class));
        verify(auditService).auditEvent(
                eq(AuditType.ADMIN_CREATED),
                eq("User"),
                any(),
                contains("Utilisateur créé"),
                any()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveSuperAdmin() {
        // Given
        when(roleRepository.findByName("ROLE_SUPER_ADMIN")).thenReturn(superAdminRole);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testSuperAdmin);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        userService.saveSuperAdmin(testUserDto);

        // Then
        verify(userRepository).save(any(User.class));
        verify(auditService).auditEvent(
                eq(AuditType.ADMIN_CREATED),
                eq("User"),
                any(),
                contains("Super Admin créé"),
                any()
        );
    }

    @Test
    void testSaveUserByAdmin() {
        // Given
        String adminEmail = "admin.regional@example.com";
        testUserDto.setAssignedBranche(Branche.CASA_AZHAR);

        when(userRepository.findByEmail(adminEmail)).thenReturn(testAdmin);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(userRole);
        when(regionRepository.findByCode("SUP_BERNOUSSI_ZENATA")).thenReturn(testRegion);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        userService.saveUserByAdmin(testUserDto, adminEmail);

        // Then
        verify(userRepository).save(any(User.class));
        verify(auditService).auditEvent(
                eq(AuditType.USER_CREATED),
                eq("User"),
                any(),
                contains("Utilisateur créé"),
                eq(adminEmail)
        );
    }

    @Test
    void testSaveUserByAdmin_NotAnAdmin() {
        // Given
        String userEmail = "user@example.com";
        when(userRepository.findByEmail(userEmail)).thenReturn(testUser); // Utilisateur normal

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.saveUserByAdmin(testUserDto, userEmail));
        assertEquals("Seuls les administrateurs régionaux peuvent créer des utilisateurs",
                exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testSaveAdminBySuper() {
        // Given
        List<Long> regionIds = Arrays.asList(1L);
        mockSecurityContext("super.admin@example.com");

        when(userRepository.findByEmail("super.admin@example.com")).thenReturn(testSuperAdmin);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(adminRole);
        when(regionRepository.findAllById(regionIds)).thenReturn(Arrays.asList(testRegion));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testAdmin);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        userService.saveAdminBySuper(testUserDto, regionIds);

        // Then
        verify(userRepository).save(any(User.class));
        verify(auditService).auditEvent(
                eq(AuditType.ADMIN_CREATED),
                eq("User"),
                any(),
                contains("Admin régional créé"),
                eq("super.admin@example.com")
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveAdminBySuper_NotSuperAdmin() {
        // Given
        List<Long> regionIds = Arrays.asList(1L);
        mockSecurityContext("admin.regional@example.com");

        when(userRepository.findByEmail("admin.regional@example.com")).thenReturn(testAdmin);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.saveAdminBySuper(testUserDto, regionIds));
        assertEquals("Seuls les super administrateurs peuvent créer des admins régionaux",
                exception.getMessage());
        verify(userRepository, never()).save(any());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testUpdateUser() {
        // Given
        Long userId = 1L;
        mockSecurityContext("admin@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(regionRepository.findById(1L)).thenReturn(Optional.of(testRegion));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        testUserDto.setPassword("newPassword");
        testUserDto.setFirstName("Jean Updated");

        // When
        userService.updateUser(userId, testUserDto);

        // Then
        verify(userRepository).save(any(User.class));
        verify(auditService).auditEvent(
                eq(AuditType.USER_UPDATED),
                eq("User"),
                eq(userId),
                contains("Utilisateur modifié"),
                any()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteUser_Success() {
        // Given
        Long userId = 1L;
        testUser.setCreatedUsers(new ArrayList<>()); // Liste mutable vide
        mockSecurityContext("admin@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).save(testUser); // Pour supprimer les associations
        verify(userRepository).delete(testUser);
        verify(auditService).auditEvent(
                eq(AuditType.USER_DELETED),
                eq("User"),
                eq(userId),
                contains("Utilisateur supprimé"),
                any()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteUser_HasCreatedUsers() {
        // Given
        Long userId = 1L;
        User createdUser = new User();
        createdUser.setId(2L);
        testUser.setCreatedUsers(new ArrayList<>(Arrays.asList(createdUser))); // Liste mutable
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteUser(userId));
        assertEquals("Impossible de supprimer cet utilisateur car il a créé d'autres utilisateurs",
                exception.getMessage());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testIsSuperAdmin_True() {
        // When
        boolean result = userService.isSuperAdmin(testSuperAdmin);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsSuperAdmin_False() {
        // When
        boolean result = userService.isSuperAdmin(testUser);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsRegionalAdmin_True() {
        // When
        boolean result = userService.isRegionalAdmin(testAdmin);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsRegionalAdmin_False() {
        // When
        boolean result = userService.isRegionalAdmin(testUser);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsDirectionAdmin_True() {
        // Given
        testAdmin.setDirection(testDirection);

        // When
        boolean result = userService.isDirectionAdmin(testAdmin);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsDirectionAdmin_False() {
        // When
        boolean result = userService.isDirectionAdmin(testUser);

        // Then
        assertFalse(result);
    }

    @Test
    void testFindAllRegionalAdmins() {
        // Given
        List<User> admins = Arrays.asList(testAdmin);
        when(userRepository.findByRolesName("ROLE_ADMIN")).thenReturn(admins);

        // When
        List<UserDto> result = userService.findAllRegionalAdmins();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAdmin.getEmail(), result.get(0).getEmail());
        verify(userRepository).findByRolesName("ROLE_ADMIN");
    }

    @Test
    void testFindAllSuperAdmins() {
        // Given
        List<User> superAdmins = Arrays.asList(testSuperAdmin);
        when(userRepository.findByRolesName("ROLE_SUPER_ADMIN")).thenReturn(superAdmins);

        // When
        List<UserDto> result = userService.findAllSuperAdmins();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSuperAdmin.getEmail(), result.get(0).getEmail());
        verify(userRepository).findByRolesName("ROLE_SUPER_ADMIN");
    }

    @Test
    void testFindUsersByRegion() {
        // Given
        Long regionId = 1L;
        List<User> users = Arrays.asList(testUser);
        when(regionRepository.findById(regionId)).thenReturn(Optional.of(testRegion));
        when(userRepository.findByRegion(testRegion)).thenReturn(users);

        // When
        List<UserDto> result = userService.findUsersByRegion(regionId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser.getEmail(), result.get(0).getEmail());
        verify(userRepository).findByRegion(testRegion);
    }

    @Test
    void testFindUsersByDirection() {
        // Given
        Long directionId = 1L;
        List<User> users = Arrays.asList(testUser);
        when(directionRepository.findById(directionId)).thenReturn(Optional.of(testDirection));
        when(userRepository.findByDirection(testDirection)).thenReturn(users);

        // When
        List<UserDto> result = userService.findUsersByDirection(directionId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser.getEmail(), result.get(0).getEmail());
        verify(userRepository).findByDirection(testDirection);
    }

    @Test
    void testAssignRegionToAdmin() {
        // Given
        Long adminId = 2L;
        Long regionId = 2L; // Une région différente de celle déjà assignée
        Region newRegion = new Region();
        newRegion.setId(2L);
        newRegion.setName("Rabat");

        mockSecurityContext("admin@example.com");

        when(userRepository.findById(adminId)).thenReturn(Optional.of(testAdmin));
        when(regionRepository.findById(regionId)).thenReturn(Optional.of(newRegion));
        when(userRepository.save(any(User.class))).thenReturn(testAdmin);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        userService.assignRegionToAdmin(adminId, regionId);

        // Then
        verify(userRepository).save(testAdmin);
        verify(auditService).auditEvent(
                eq(AuditType.ADMIN_ASSIGNED_REGION),
                eq("User"),
                eq(adminId),
                contains("Région"),
                any()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testAssignRegionToAdmin_NotAnAdmin() {
        // Given
        Long userId = 1L; // utilisateur normal
        Long regionId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.assignRegionToAdmin(userId, regionId));
        assertEquals("L'utilisateur n'est pas un administrateur régional", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRemoveRegionFromAdmin() {
        // Given
        Long adminId = 2L;
        Long regionId = 2L;

        // Ajouter une deuxième région pour éviter l'erreur "impossible de retirer la seule région"
        Region secondRegion = new Region();
        secondRegion.setId(2L);
        secondRegion.setName("Rabat");
        testAdmin.setRegions(new ArrayList<>(Arrays.asList(testRegion, secondRegion)));

        mockSecurityContext("admin@example.com");

        when(userRepository.findById(adminId)).thenReturn(Optional.of(testAdmin));
        when(regionRepository.findById(regionId)).thenReturn(Optional.of(secondRegion));
        when(userRepository.save(any(User.class))).thenReturn(testAdmin);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        userService.removeRegionFromAdmin(adminId, regionId);

        // Then
        verify(userRepository).save(testAdmin);
        verify(auditService).auditEvent(
                eq(AuditType.ADMIN_REMOVED_REGION),
                eq("User"),
                eq(adminId),
                contains("Région"),
                any()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveDirectionAdmin() {
        // Given
        Long directionId = 1L;
        mockSecurityContext("super.admin@example.com");

        when(userRepository.findByEmail("super.admin@example.com")).thenReturn(testSuperAdmin);
        when(directionRepository.findById(directionId)).thenReturn(Optional.of(testDirection));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(adminRole);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testAdmin);
        when(auditService.auditEvent(any(), any(), any(), any(), any())).thenReturn(new Audit());

        // When
        userService.saveDirectionAdmin(testUserDto, directionId);

        // Then
        verify(userRepository).save(any(User.class));
        verify(auditService).auditEvent(
                eq(AuditType.ADMIN_CREATED),
                eq("User"),
                any(),
                contains("Directeur de division créé"),
                any()
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsUserManagingDirection_SuperAdmin() {
        // When
        boolean result = userService.isUserManagingDirection(testSuperAdmin, testDirection);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsUserManagingDirection_DirectionAdmin() {
        // Given
        testAdmin.setDirection(testDirection);

        // When
        boolean result = userService.isUserManagingDirection(testAdmin, testDirection);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsUserManagingDirection_DifferentDirection() {
        // Given
        Direction otherDirection = new Direction();
        otherDirection.setId(2L);
        testAdmin.setDirection(otherDirection);

        // When
        boolean result = userService.isUserManagingDirection(testAdmin, testDirection);

        // Then
        assertFalse(result);
    }

    @Test
    void testFindUsersByCreator() {
        // Given
        Long creatorId = 2L; // testAdmin ID
        List<User> createdUsers = Arrays.asList(testUser);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(testAdmin));
        when(userRepository.findByCreatedByAdmin(testAdmin)).thenReturn(createdUsers);

        // When
        List<UserDto> result = userService.findUsersByCreator(creatorId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser.getEmail(), result.get(0).getEmail());
        verify(userRepository).findByCreatedByAdmin(testAdmin);
    }

    @Test
    void testCanUserManageClient_SuperAdmin() {
        // Given
        Client testClient = new Client();
        testClient.setId(1L);
        testClient.setNMDIR("DIR001");

        // When
        boolean result = userService.canUserManageClient(testSuperAdmin, testClient);

        // Then
        assertTrue(result);
    }

    @Test
    void testCanUserManageClient_DirectionAdmin() {
        // Given
        testAdmin.setDirection(testDirection);
        Client testClient = new Client();
        testClient.setId(1L);
        testClient.setNMDIR("DIR001"); // Même code que testDirection

        // When
        boolean result = userService.canUserManageClient(testAdmin, testClient);

        // Then
        assertTrue(result);
    }

    @Test
    void testCanUserManageClient_AssignedUser() {
        // Given
        Client testClient = new Client();
        testClient.setId(1L);
        testClient.setAssignedUser(testUser);

        // When
        boolean result = userService.canUserManageClient(testUser, testClient);

        // Then
        assertTrue(result);
    }

    @Test
    void testCanUserManageClient_NoAccess() {
        // Given
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setRoles(new ArrayList<>(Arrays.asList(userRole))); // Liste mutable

        Client testClient = new Client();
        testClient.setId(1L);
        testClient.setAssignedUser(testUser); // Assigné à un autre utilisateur

        // When
        boolean result = userService.canUserManageClient(otherUser, testClient);

        // Then
        assertFalse(result);
    }
}