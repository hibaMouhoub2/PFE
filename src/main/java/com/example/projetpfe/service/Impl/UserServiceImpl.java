package com.example.projetpfe.service.Impl;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.DirectionRepository;
import com.example.projetpfe.repository.RegionRepository;
import com.example.projetpfe.repository.RoleRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private DirectionRepository directionRepository;
    private RegionRepository regionRepository;
    private boolean passwordChanged = false;
    private AuditService auditService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           DirectionRepository directionRepository,
                           RegionRepository regionRepository,
                           AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.directionRepository = directionRepository;
        this.regionRepository = regionRepository;
        this.auditService = auditService;
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
    }

    /**
     * Méthode générique pour maintenir la compatibilité
     */
    @Override
    @Transactional
    public void saveUser(UserDto userDto, String roleName) {
        User user = new User();
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        Role role;
        switch(roleName) {
            case "SUPER_ADMIN":
                role = checkRoleExist("ROLE_SUPER_ADMIN");
                break;
            case "ADMIN":
                role = checkRoleExist("ROLE_ADMIN");
                break;
            default:
                role = checkRoleExist("ROLE_USER");
        }

        user.setRoles(Arrays.asList(role));

        // Si un ID de région est spécifié et que ce n'est pas un admin
        if (userDto.getRegionId() != null && !roleName.equals("ADMIN") && !roleName.equals("SUPER_ADMIN")) {
            Region region = regionRepository.findById(userDto.getRegionId())
                    .orElseThrow(() -> new RuntimeException("Région non trouvée avec l'ID: " + userDto.getRegionId()));
            user.setRegion(region);
        }

        // Si des IDs de régions sont spécifiés et que c'est un admin
        if (!userDto.getRegionIds().isEmpty() && roleName.equals("ADMIN")) {
            List<Region> regions = regionRepository.findAllById(userDto.getRegionIds());
            user.setRegions(regions);
        }

        User savedUser = userRepository.save(user);

        // Récupérer l'admin qui crée l'utilisateur
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();

        // Audit de la création d'utilisateur
        AuditType auditType;
        if (roleName.equals("SUPER_ADMIN")) {
            auditType = AuditType.ADMIN_CREATED;
        } else if (roleName.equals("ADMIN")) {
            auditType = AuditType.ADMIN_CREATED;
        } else {
            auditType = AuditType.USER_CREATED;
        }

        auditService.auditEvent(auditType,
                "User",
                savedUser.getId(),
                "Utilisateur créé: " + savedUser.getName() + " (" + savedUser.getEmail() + ") avec rôle " + roleName,
                adminEmail);
    }

    /**
     * Création d'un utilisateur par un admin régional
     */
    @Override
    @Transactional
    public void saveUserByAdmin(UserDto userDto, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail);
        if (admin == null) {
            throw new RuntimeException("Administrateur non trouvé");
        }

        if (!isRegionalAdmin(admin)) {
            throw new RuntimeException("Seuls les administrateurs régionaux peuvent créer des utilisateurs");
        }

        // Créer l'utilisateur
        User user = new User();
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        // Attribuer le rôle USER
        Role userRole = checkRoleExist("ROLE_USER");
        user.setRoles(Arrays.asList(userRole));

        // Associer l'utilisateur à la même direction que l'admin
        if (admin.getDirection() != null) {
            user.setDirection(admin.getDirection());
        }

        // Définir l'admin comme créateur
        user.setCreatedByAdmin(admin);

        // Si une branche est spécifiée, l'assigner à l'agent
        if (userDto.getAssignedBranche() != null) {
            user.setAssignedBranche(userDto.getAssignedBranche());
            String regionCode = userDto.getAssignedBranche().getRegionCode();
            Region region = regionRepository.findByCode(regionCode);
            if (region != null) {
                user.setRegion(region);
            }

        }

        userRepository.save(user);

        // Audit
        auditService.auditEvent(
                AuditType.USER_CREATED,
                "User",
                user.getId(),
                "Utilisateur créé: " + user.getName() + " (" + user.getEmail() + ") par l'admin " + admin.getName(),
                adminEmail
        );
    }

    /**
     * Création d'un admin régional par un super admin
     */
    @Override
    @Transactional
    public void saveAdminBySuper(UserDto adminDto, List<Long> regionIds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User superAdmin = userRepository.findByEmail(auth.getName());

        if (superAdmin == null || !isSuperAdmin(superAdmin)) {
            throw new RuntimeException("Seuls les super administrateurs peuvent créer des admins régionaux");
        }

        // Créer l'admin
        User admin = new User();
        admin.setName(adminDto.getFirstName() + " " + adminDto.getLastName());
        admin.setEmail(adminDto.getEmail());
        admin.setEnabled(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setPassword(passwordEncoder.encode(adminDto.getPassword()));

        // Attribuer le rôle ADMIN
        Role adminRole = checkRoleExist("ROLE_ADMIN");
        admin.setRoles(Arrays.asList(adminRole));

        // Définir le super admin comme créateur
        admin.setCreatedByAdmin(superAdmin);

        // Ajouter les régions
        if (regionIds != null && !regionIds.isEmpty()) {
            List<Region> regions = regionRepository.findAllById(regionIds);
            admin.setRegions(regions);
        }

        User savedAdmin = userRepository.save(admin);

        // Audit
        auditService.auditEvent(
                AuditType.ADMIN_CREATED,
                "User",
                savedAdmin.getId(),
                "Admin régional créé: " + savedAdmin.getName() + " (" + savedAdmin.getEmail() + ") par le super admin " + superAdmin.getName(),
                auth.getName()
        );
    }

    /**
     * Création d'un super admin (uniquement pour l'initialisation ou par un autre super admin)
     */
    @Override
    @Transactional
    public void saveSuperAdmin(UserDto superAdminDto) {
        // Vérifier si l'action est réalisée par un super admin existant
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            User currentUser = userRepository.findByEmail(auth.getName());
            if (currentUser != null && !isSuperAdmin(currentUser)) {
                throw new RuntimeException("Seul un super administrateur peut créer un autre super administrateur");
            }
        }

        // Créer le super admin
        User superAdmin = new User();
        superAdmin.setName(superAdminDto.getFirstName() + " " + superAdminDto.getLastName());
        superAdmin.setEmail(superAdminDto.getEmail());
        superAdmin.setEnabled(true);
        superAdmin.setCreatedAt(LocalDateTime.now());
        superAdmin.setPassword(passwordEncoder.encode(superAdminDto.getPassword()));

        // Attribuer le rôle SUPER_ADMIN
        Role superAdminRole = checkRoleExist("ROLE_SUPER_ADMIN");
        superAdmin.setRoles(Arrays.asList(superAdminRole));

        User savedSuperAdmin = userRepository.save(superAdmin);

        // Audit
        String creatorEmail = auth != null && !auth.getName().equals("anonymousUser") ? auth.getName() : "System";

        auditService.auditEvent(
                AuditType.ADMIN_CREATED,
                "User",
                savedSuperAdmin.getId(),
                "Super Admin créé: " + savedSuperAdmin.getName() + " (" + savedSuperAdmin.getEmail() + ")",
                creatorEmail
        );
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> findUsersByRegion(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Région non trouvée avec l'ID: " + regionId));

        List<User> users = userRepository.findByRegion(region);
        return users.stream().map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> findAllRegionalAdmins() {
        List<User> admins = userRepository.findByRolesName("ROLE_ADMIN");
        return admins.stream().map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> findAllSuperAdmins() {
        List<User> superAdmins = userRepository.findByRolesName("ROLE_SUPER_ADMIN");
        return superAdmins.stream().map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    private UserDto convertEntityToDto(User user) {
        UserDto userDto = new UserDto();
        String[] name = user.getName().split(" ");
        userDto.setFirstName(name.length > 0 ? name[0] : "");
        userDto.setLastName(name.length > 1 ? name[1] : "");
        userDto.setEmail(user.getEmail());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setEnabled(user.getEnabled());
        userDto.setId(user.getId());

        // Ajouter les informations de région
        if (user.getRegion() != null) {
            userDto.setRegionId(user.getRegion().getId());
            userDto.setRegionName(user.getRegion().getName());
        }

        // Ajouter les informations d'administrateur créateur
        if (user.getCreatedByAdmin() != null) {
            userDto.setCreatedByAdminId(user.getCreatedByAdmin().getId());
            userDto.setCreatedByAdminName(user.getCreatedByAdmin().getName());
        }

        // Pour les administrateurs, ajouter les régions gérées
        if (isRegionalAdmin(user) && user.getRegions() != null) {
            userDto.setRegionIds(user.getRegions().stream()
                    .map(Region::getId)
                    .collect(Collectors.toList()));
        }

        return userDto;
    }

    private Role checkRoleExist(String roleName) {
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role = roleRepository.save(role);
        }
        return role;
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));

        // Vérifier le rôle de l'utilisateur pour déterminer le type d'audit
        boolean isSuperAdmin = isSuperAdmin(user);
        boolean isAdmin = isRegionalAdmin(user);

        // Vérifier si l'utilisateur a des utilisateurs créés
        if (!user.getCreatedUsers().isEmpty()) {
            throw new RuntimeException("Impossible de supprimer cet utilisateur car il a créé d'autres utilisateurs");
        }

        // Supprimer les associations avec les rôles avant de supprimer l'utilisateur
        user.getRoles().clear();

        // Supprimer les associations avec les régions
        user.getRegions().clear();

        userRepository.save(user); // Enregistrer les changements

        // Maintenant supprimer l'utilisateur
        userRepository.delete(user);

        // Récupérer l'admin qui supprime l'utilisateur
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();

        // Audit de la suppression d'utilisateur
        AuditType auditType;
        if (isSuperAdmin) {
            auditType = AuditType.ADMIN_DELETED;
        } else if (isAdmin) {
            auditType = AuditType.ADMIN_DELETED;
        } else {
            auditType = AuditType.USER_DELETED;
        }

        auditService.auditEvent(auditType,
                "User",
                id,
                "Utilisateur supprimé: " + user.getName() + " (" + user.getEmail() + ")",
                adminEmail);
    }

    @Override
    public UserDto findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        return convertEntityToDto(user);
    }

    @Override
    @Transactional
    public void updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));

        // Sauvegarder les anciennes valeurs pour l'audit
        String oldName = user.getName();
        String oldEmail = user.getEmail();
        Boolean oldEnabled = user.getEnabled();
        Region oldRegion = user.getRegion();
        List<Region> oldRegions = new ArrayList<>(user.getRegions());

        //Mise à jour des informations
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setEnabled(userDto.getEnabled());

        // Mise à jour de la région pour les utilisateurs normaux
        if (userDto.getRegionId() != null && !isRegionalAdmin(user) && !isSuperAdmin(user)) {
            Region region = regionRepository.findById(userDto.getRegionId())
                    .orElseThrow(() -> new RuntimeException("Région non trouvée avec l'ID: " + userDto.getRegionId()));
            user.setRegion(region);
        }

        // Ne mettre à jour le mot de passe que s'il est fourni
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            passwordChanged = true;
        }

        userRepository.save(user);

        // Récupérer l'admin qui modifie l'utilisateur
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();

        // Préparer les détails des modifications pour l'audit
        StringBuilder changes = new StringBuilder();
        if (!oldName.equals(user.getName())) {
            changes.append("Nom modifié: ").append(oldName).append(" -> ").append(user.getName()).append(", ");
        }
        if (!oldEmail.equals(user.getEmail())) {
            changes.append("Email modifié: ").append(oldEmail).append(" -> ").append(user.getEmail()).append(", ");
        }
        if (oldEnabled != user.getEnabled()) {
            changes.append("Statut modifié: ").append(oldEnabled ? "Actif" : "Inactif")
                    .append(" -> ").append(user.getEnabled() ? "Actif" : "Inactif").append(", ");
        }
        if (passwordChanged) {
            changes.append("Mot de passe modifié, ");
        }
        if (oldRegion != user.getRegion()) {
            String oldRegionName = oldRegion != null ? oldRegion.getName() : "aucune";
            String newRegionName = user.getRegion() != null ? user.getRegion().getName() : "aucune";
            changes.append("Région modifiée: ").append(oldRegionName).append(" -> ").append(newRegionName).append(", ");
        }

        // Supprimer la virgule finale s'il y a des changements
        String changesStr = changes.toString();
        if (changesStr.endsWith(", ")) {
            changesStr = changesStr.substring(0, changesStr.length() - 2);
        }

        // Déterminer le type d'audit en fonction du rôle de l'utilisateur
        AuditType auditType;
        if (isSuperAdmin(user)) {
            auditType = AuditType.ADMIN_UPDATED;
        } else if (isRegionalAdmin(user)) {
            auditType = AuditType.ADMIN_UPDATED;
        } else {
            auditType = AuditType.USER_UPDATED;
        }

        // Audit de la mise à jour d'utilisateur
        auditService.auditEvent(auditType,
                "User",
                id,
                "Utilisateur modifié: " + user.getName() +
                        (changesStr.isEmpty() ? "" : " (" + changesStr + ")"),
                adminEmail);
    }

    @Override
    @Transactional
    public void assignRegionToAdmin(Long adminId, Long regionId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Administrateur non trouvé avec l'ID: " + adminId));

        if (!isRegionalAdmin(admin)) {
            throw new RuntimeException("L'utilisateur n'est pas un administrateur régional");
        }

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Région non trouvée avec l'ID: " + regionId));

        // Vérifier si la région est déjà associée à l'admin
        if (admin.getRegions().contains(region)) {
            throw new RuntimeException("Cette région est déjà associée à cet administrateur");
        }

        // Ajouter la région à l'administrateur
        admin.getRegions().add(region);
        userRepository.save(admin);

        // Audit
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.ADMIN_ASSIGNED_REGION,
                "User",
                admin.getId(),
                "Région " + region.getName() + " assignée à l'administrateur " + admin.getName(),
                userEmail
        );
    }

    @Override
    @Transactional
    public void removeRegionFromAdmin(Long adminId, Long regionId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Administrateur non trouvé avec l'ID: " + adminId));

        if (!isRegionalAdmin(admin)) {
            throw new RuntimeException("L'utilisateur n'est pas un administrateur régional");
        }

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Région non trouvée avec l'ID: " + regionId));

        // Vérifier si la région est associée à l'admin
        if (!admin.getRegions().contains(region)) {
            throw new RuntimeException("Cette région n'est pas associée à cet administrateur");
        }

        // Vérifier si c'est la seule région de l'admin
        if (admin.getRegions().size() == 1) {
            throw new RuntimeException("Impossible de retirer la seule région d'un administrateur");
        }

        // Retirer la région de l'administrateur
        admin.getRegions().remove(region);
        userRepository.save(admin);

        // Audit
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.ADMIN_REMOVED_REGION,
                "User",
                admin.getId(),
                "Région " + region.getName() + " retirée de l'administrateur " + admin.getName(),
                userEmail
        );
    }

    @Override
    public boolean isSuperAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_SUPER_ADMIN"));
    }

    @Override
    public boolean isRegionalAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
    }

    @Override
    public List<UserDto> findUsersByCreator(Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Utilisateur créateur non trouvé avec l'ID: " + creatorId));

        List<User> users = userRepository.findByCreatedByAdmin(creator);
        return users.stream()
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si l'utilisateur est un directeur (responsable d'une direction)
     */
    @Override
    public boolean isDirectionAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"))
                && user.getDirection() != null;
    }

    /**
     * Vérifie si l'utilisateur est responsable d'une direction spécifique
     */
    @Override
    public boolean isUserManagingDirection(User user, Direction direction) {
        if (user == null || direction == null) {
            return false;
        }

        // Super admin peut gérer toutes les directions
        if (isSuperAdmin(user)) {
            return true;
        }

        // Vérifier si l'utilisateur est un admin de cette direction
        return isDirectionAdmin(user) &&
                user.getDirection() != null &&
                user.getDirection().getId().equals(direction.getId());
    }

    @Override
    public boolean canUserManageClient(User user, Client client) {
        if (user == null || client == null) {
            return false;
        }

        // Super admin peut gérer tous les clients
        if (isSuperAdmin(user)) {
            return true;
        }

        // Si l'utilisateur est un admin de direction
        if (isDirectionAdmin(user) && user.getDirection() != null && client.getNMDIR() != null) {
            // Vérifier si le client appartient à la direction de l'utilisateur
            return user.getDirection().getCode().equals(client.getNMDIR());
        }

        // Si l'utilisateur est un agent, vérifier s'il est assigné à ce client
        return user.getId().equals(client.getAssignedUser().getId());
    }
    @Override
    public List<UserDto> findUsersByDirection(Long directionId) {
        Direction direction = directionRepository.findById(directionId)
                .orElseThrow(() -> new RuntimeException("Direction non trouvée avec l'ID: " + directionId));

        // Récupérer les utilisateurs par direction
        List<User> users = userRepository.findByDirection(direction);

        // Convertir en DTO
        return users.stream()
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveDirectionAdmin(UserDto adminDto, Long directionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User superAdmin = userRepository.findByEmail(auth.getName());

        if (superAdmin == null || !isSuperAdmin(superAdmin)) {
            throw new RuntimeException("Seuls les super administrateurs peuvent créer des directeurs de division");
        }

        // Créer l'admin
        User admin = new User();
        admin.setName(adminDto.getFirstName() + " " + adminDto.getLastName());
        admin.setEmail(adminDto.getEmail());
        admin.setEnabled(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setPassword(passwordEncoder.encode(adminDto.getPassword()));

        // Attribuer le rôle ADMIN
        Role adminRole = checkRoleExist("ROLE_ADMIN");
        admin.setRoles(Arrays.asList(adminRole));

        // Définir le super admin comme créateur
        admin.setCreatedByAdmin(superAdmin);

        // Ajouter la direction
        Direction direction = directionRepository.findById(directionId)
                .orElseThrow(() -> new RuntimeException("Direction non trouvée avec l'ID: " + directionId));
        admin.setDirection(direction);

        User savedAdmin = userRepository.save(admin);

        // Audit
        auditService.auditEvent(
                AuditType.ADMIN_CREATED,
                "User",
                savedAdmin.getId(),
                "Directeur de division créé: " + savedAdmin.getName() + " (" + savedAdmin.getEmail() + ") pour la direction " + direction.getName(),
                auth.getName()
        );
    }
}