package com.example.projetpfe.service;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.Client;
import com.example.projetpfe.entity.Direction;
import com.example.projetpfe.entity.User;

import java.util.List;

public interface UserService {
    User findByEmail(String email);
    void deleteUser(Long id);
    void updateUser(Long id, UserDto userDto);
    UserDto findUserById(Long id);
    List<UserDto> findAllUsers();
    public User findById(Long id);
    // Création d'utilisateur par un admin régional (associé automatiquement à la région de l'admin)
    void saveUserByAdmin(UserDto userDto, String adminEmail);

    // Création d'un admin régional par un super admin
    void saveAdminBySuper(UserDto adminDto, List<Long> regionIds);

    // Création d'un super admin (utilisé uniquement pour l'initialisation ou par un autre super admin)
    void saveSuperAdmin(UserDto superAdminDto);

    // Méthode générique maintenue pour la compatibilité
    void saveUser(UserDto userDto, String roleName);

    // Obtenir tous les utilisateurs par région
    List<UserDto> findUsersByRegion(Long regionId);

    // Obtenir tous les admins régionaux
    List<UserDto> findAllRegionalAdmins();

    // Obtenir tous les super admins
    List<UserDto> findAllSuperAdmins();

    // Attribuer une région à un admin
    void assignRegionToAdmin(Long adminId, Long regionId);

    // Retirer une région d'un admin
    void removeRegionFromAdmin(Long adminId, Long regionId);

    // Vérifier si l'utilisateur est un super admin
    boolean isSuperAdmin(User user);

    // Vérifier si l'utilisateur est un admin régional
    boolean isRegionalAdmin(User user);

    List<UserDto> findUsersByCreator(Long creatorId);

    boolean isDirectionAdmin(User user);
    boolean isUserManagingDirection(User user, Direction direction);
    boolean canUserManageClient(User user, Client client);
    List<UserDto> findUsersByDirection(Long directionId);
    void saveDirectionAdmin(UserDto adminDto, Long directionId);
}
