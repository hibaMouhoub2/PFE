package com.example.projetpfe.service.Impl;


import com.example.projetpfe.entity.Audit;
import com.example.projetpfe.entity.AuditType;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.AuditRepository;
import com.example.projetpfe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Crée une entrée d'audit en utilisant l'utilisateur actuellement connecté
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Audit auditEvent(AuditType type, String entityType, Long entityId, String details) {
        try {
            // Récupérer l'utilisateur connecté
            User user = getCurrentUser();

            // Créer l'entrée d'audit
            Audit audit = Audit.create(type, entityType, entityId, details, user);

            // Sauvegarder dans la base de données
            audit = auditRepository.save(audit);

            // Logger l'événement
            logger.debug("Audit créé: [{}] {} (ID: {}) par utilisateur {}: {}",
                    type, entityType, entityId, user != null ? user.getEmail() : "System", details);

            return audit;
        } catch (Exception e) {
            // En cas d'erreur, on ne veut pas que l'opération principale échoue à cause de l'audit
            logger.error("Erreur lors de la création d'un audit: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Crée une entrée d'audit avec un utilisateur explicite (utile pour les opérations système)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Audit auditEvent(AuditType type, String entityType, Long entityId, String details, String userEmail) {
        try {
            User user = null;
            if (userEmail != null && !userEmail.isEmpty()) {
                user = userRepository.findByEmail(userEmail);
            }

            Audit audit = Audit.create(type, entityType, entityId, details, user);
            audit = auditRepository.save(audit);

            logger.debug("Audit créé manuellement: [{}] {} (ID: {}) par utilisateur {}: {}",
                    type, entityType, entityId, userEmail, details);

            return audit;
        } catch (Exception e) {
            logger.error("Erreur lors de la création d'un audit manuel: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Recherche les entrées d'audit par utilisateur
     */
    public List<Audit> findByUser(User user) {
        return auditRepository.findByUser(user);
    }

    /**
     * Recherche les entrées d'audit par type
     */
    public List<Audit> findByType(AuditType type) {
        return auditRepository.findByType(type);
    }

    /**
     * Recherche les entrées d'audit par entité
     */
    public List<Audit> findByEntity(String entityType, Long entityId) {
        return auditRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Recherche les entrées d'audit par période
     */
    public List<Audit> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditRepository.findByTimestampBetween(start, end);
    }

    /**
     * Recherche paginée des entrées d'audit par type et période
     */
    public Page<Audit> findByTypeAndDateRange(AuditType type, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditRepository.findByTypeAndTimestampBetween(type, start, end, pageable);
    }

    /**
     * Utilitaire pour récupérer l'utilisateur actuel depuis le contexte de sécurité
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName());
        }
        return null;
    }


    public Page<Audit> findAllPaged(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditRepository.findByTimestampBetween(start, end, pageable);
    }

    /**
     * Recherche les entrées d'audit par période avec pagination
     */
    public Page<Audit> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditRepository.findByTimestampBetween(start, end, pageable);
    }


}
