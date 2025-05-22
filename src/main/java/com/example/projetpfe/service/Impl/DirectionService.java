package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.AuditType;
import com.example.projetpfe.entity.Direction;
import com.example.projetpfe.repository.DirectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DirectionService {

    @Autowired
    private DirectionRepository directionRepository;

    @Autowired
    private AuditService auditService;

    public List<Direction> findAll() {
        return directionRepository.findAll();
    }

    public Optional<Direction> findById(Long id) {
        return directionRepository.findById(id);
    }

    public Direction findByName(String name) {
        return directionRepository.findByName(name);
    }

    public Direction findByCode(String code) {
        return directionRepository.findByCode(code);
    }

    @Transactional
    public Direction createDirection(String name, String code) {
        // Vérifier si une direction avec le même nom ou code existe déjà
        if (directionRepository.existsByName(name)) {
            throw new IllegalArgumentException("Une direction avec ce nom existe déjà");
        }
        if (directionRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Une direction avec ce code existe déjà");
        }

        Direction direction = new Direction();
        direction.setName(name);
        direction.setCode(code);

        Direction savedDirection = directionRepository.save(direction);

        // Audit de la création de direction
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.DIRECTION_CREATED,
                "Direction",
                savedDirection.getId(),
                "Direction créée: " + name + " (Code: " + code + ")",
                userEmail
        );

        return savedDirection;
    }

    @Transactional
    public Direction updateDirection(Long id, String name, String code) {
        Direction direction = directionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Direction non trouvée avec l'ID: " + id));

        // Vérifier si le nouveau nom est déjà utilisé par une autre direction
        Direction existingByName = directionRepository.findByName(name);
        if (existingByName != null && !existingByName.getId().equals(id)) {
            throw new IllegalArgumentException("Une direction avec ce nom existe déjà");
        }

        // Vérifier si le nouveau code est déjà utilisé par une autre direction
        Direction existingByCode = directionRepository.findByCode(code);
        if (existingByCode != null && !existingByCode.getId().equals(id)) {
            throw new IllegalArgumentException("Une direction avec ce code existe déjà");
        }

        String oldName = direction.getName();
        String oldCode = direction.getCode();

        direction.setName(name);
        direction.setCode(code);

        Direction updatedDirection = directionRepository.save(direction);

        // Audit de la mise à jour de direction
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.DIRECTION_UPDATED,
                "Direction",
                updatedDirection.getId(),
                "Direction modifiée: " + oldName + " -> " + name + " (Code: " + oldCode + " -> " + code + ")",
                userEmail
        );

        return updatedDirection;
    }

    @Transactional
    public void deleteDirection(Long id) {
        Direction direction = directionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Direction non trouvée avec l'ID: " + id));

        // Vérifier si la direction est associée à des régions ou des utilisateurs
        if (!direction.getRegions().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer une direction associée à des régions");
        }

        if (!direction.getDirectionalAdmins().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer une direction associée à des administrateurs");
        }

        String directionName = direction.getName();
        String directionCode = direction.getCode();

        directionRepository.delete(direction);

        // Audit de la suppression de direction
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.DIRECTION_DELETED,
                "Direction",
                id,
                "Direction supprimée: " + directionName + " (Code: " + directionCode + ")",
                userEmail
        );
    }
}