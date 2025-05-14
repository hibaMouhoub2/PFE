package com.example.projetpfe.repository;

import com.example.projetpfe.entity.Direction;
import com.example.projetpfe.entity.Region;
import com.example.projetpfe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    long countByRolesName(String roleName);
    List<User> findByRolesName(String roleName);
    // Trouver les utilisateurs par région
    List<User> findByRegion(Region region);

    // Trouver les admins qui gèrent une région spécifique
    @Query("SELECT u FROM User u JOIN u.regions r WHERE r.id = :regionId AND 'ROLE_ADMIN' IN (SELECT role.name FROM u.roles role)")
    List<User> findAdminsByRegionId(@Param("regionId") Long regionId);

    // Trouver les utilisateurs créés par un admin spécifique
    List<User> findByCreatedByAdmin(User admin);

    // Vérifier si un utilisateur gère une région spécifique
    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.regions r WHERE u.id = :userId AND r.id = :regionId")
    boolean isUserManagingRegion(@Param("userId") Long userId, @Param("regionId") Long regionId);

    List<User> findByDirection(Direction direction);

    @Query("SELECT u FROM User u JOIN u.regions r WHERE r.direction.id = :directionId AND 'ROLE_ADMIN' IN (SELECT role.name FROM u.roles role)")
    List<User> findAdminsByDirectionId(@Param("directionId") Long directionId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.id = :userId AND u.direction.id = :directionId")
    boolean isUserManagingDirection(@Param("userId") Long userId, @Param("directionId") Long directionId);
}
