package com.example.projetpfe.repository;

import com.example.projetpfe.entity.Audit;
import com.example.projetpfe.entity.AuditType;
import com.example.projetpfe.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Long> {
    // Recherche par type d'audit
    List<Audit> findByType(AuditType type);

    // Recherche par utilisateur
    List<Audit> findByUser(User user);

    // Recherche par entité
    List<Audit> findByEntityTypeAndEntityId(String entityType, Long entityId);

    // Recherche par période
    List<Audit> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Recherche paginée avec filtres
    Page<Audit> findByTypeAndTimestampBetween(
            AuditType type,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable);

    // Recherche des dernières entrées d'audit pour une entité spécifique
    @Query("SELECT a FROM Audit a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<Audit> findLatestByEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            Pageable pageable);
    // Recherche par période avec pagination
    Page<Audit> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    // Comptage par utilisateur et période
    long countByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);

    // Recherche par utilisateur et période
    List<Audit> findByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);
}

