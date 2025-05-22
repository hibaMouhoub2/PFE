package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.AuditType;
import com.example.projetpfe.entity.Direction;
import com.example.projetpfe.entity.Region;
import com.example.projetpfe.repository.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RegionService {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AuditService auditService;

    public List<Region> findAll() {
        return regionRepository.findAll();
    }

    public Optional<Region> findById(Long id) {
        return regionRepository.findById(id);
    }

    public Region findByName(String name) {
        return regionRepository.findByName(name);
    }

    public Region findByCode(String code) {
        return regionRepository.findByCode(code);
    }

    @Transactional
    public Region createRegion(String name, String code) {
        // Vérifier si une région avec le même nom ou code existe déjà
        if (regionRepository.existsByName(name)) {
            throw new IllegalArgumentException("Une région avec ce nom existe déjà");
        }
        if (regionRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Une région avec ce code existe déjà");
        }

        Region region = new Region();
        region.setName(name);
        region.setCode(code);

        Region savedRegion = regionRepository.save(region);

        // Audit de la création de région
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.REGION_CREATED,
                "Region",
                savedRegion.getId(),
                "Région créée: " + name + " (Code: " + code + ")",
                userEmail
        );

        return savedRegion;
    }

    @Transactional
    public Region updateRegion(Long id, String name, String code) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Région non trouvée avec l'ID: " + id));

        // Vérifier si le nouveau nom est déjà utilisé par une autre région
        Region existingByName = regionRepository.findByName(name);
        if (existingByName != null && !existingByName.getId().equals(id)) {
            throw new IllegalArgumentException("Une région avec ce nom existe déjà");
        }

        // Vérifier si le nouveau code est déjà utilisé par une autre région
        Region existingByCode = regionRepository.findByCode(code);
        if (existingByCode != null && !existingByCode.getId().equals(id)) {
            throw new IllegalArgumentException("Une région avec ce code existe déjà");
        }

        String oldName = region.getName();
        String oldCode = region.getCode();

        region.setName(name);
        region.setCode(code);

        Region updatedRegion = regionRepository.save(region);

        // Audit de la mise à jour de région
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.REGION_UPDATED,
                "Region",
                updatedRegion.getId(),
                "Région modifiée: " + oldName + " -> " + name + " (Code: " + oldCode + " -> " + code + ")",
                userEmail
        );

        return updatedRegion;
    }


    /**
     * Initialise les régions de base dans la base de données si elles n'existent pas
     */
    @Transactional
    public void initializeRegions() {
        // Liste des régions à initialiser avec les nouveaux noms
        String[][] regionsData = {
                {"SUP FIDAA SIDI BELYOUT", "SUP_FIDAA_SIDI_BELYOUT"},
                {"SUP BERNOUSSI ZENATA", "SUP_BERNOUSSI_ZENATA"},
                {"SUP BENMSIK SIDI OTHMANE", "SUP_BENMSIK_SIDI_OTHMANE"}
        };

        for (String[] regionData : regionsData) {
            if (!regionRepository.existsByName(regionData[0])) {
                Region region = new Region();
                region.setName(regionData[0]);
                region.setCode(regionData[1]);
                regionRepository.save(region);
            }
        }
    }

    // Ajoutez ces méthodes dans votre RegionService.java existant :

    /**
     * Créer une région avec une direction associée
     */
    @Transactional
    public Region createRegionWithDirection(String name, String code, Direction direction) {
        // Vérifier si une région avec le même nom ou code existe déjà
        if (regionRepository.existsByName(name)) {
            throw new IllegalArgumentException("Une région avec ce nom existe déjà");
        }
        if (regionRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Une région avec ce code existe déjà");
        }

        Region region = new Region();
        region.setName(name);
        region.setCode(code);
        region.setDirection(direction);

        Region savedRegion = regionRepository.save(region);

        // Audit de la création de région
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        String directionInfo = direction != null ? " (Direction: " + direction.getName() + ")" : "";
        auditService.auditEvent(
                AuditType.REGION_CREATED,
                "Region",
                savedRegion.getId(),
                "Région créée: " + name + " (Code: " + code + ")" + directionInfo,
                userEmail
        );

        return savedRegion;
    }

    /**
     * Mettre à jour une région avec une direction
     */
    @Transactional
    public Region updateRegionWithDirection(Long id, String name, String code, Direction direction) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Région non trouvée avec l'ID: " + id));

        // Vérifier si le nouveau nom est déjà utilisé par une autre région
        Region existingByName = regionRepository.findByName(name);
        if (existingByName != null && !existingByName.getId().equals(id)) {
            throw new IllegalArgumentException("Une région avec ce nom existe déjà");
        }

        // Vérifier si le nouveau code est déjà utilisé par une autre région
        Region existingByCode = regionRepository.findByCode(code);
        if (existingByCode != null && !existingByCode.getId().equals(id)) {
            throw new IllegalArgumentException("Une région avec ce code existe déjà");
        }

        String oldName = region.getName();
        String oldCode = region.getCode();
        Direction oldDirection = region.getDirection();

        region.setName(name);
        region.setCode(code);
        region.setDirection(direction);

        Region updatedRegion = regionRepository.save(region);

        // Audit de la mise à jour de région
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        String oldDirectionName = oldDirection != null ? oldDirection.getName() : "Aucune";
        String newDirectionName = direction != null ? direction.getName() : "Aucune";

        auditService.auditEvent(
                AuditType.REGION_UPDATED,
                "Region",
                updatedRegion.getId(),
                "Région modifiée: " + oldName + " -> " + name +
                        " (Code: " + oldCode + " -> " + code + ")" +
                        " (Direction: " + oldDirectionName + " -> " + newDirectionName + ")",
                userEmail
        );

        return updatedRegion;
    }

    /**
     * Supprimer une région avec vérifications renforcées
     */
    @Transactional
    public void deleteRegion(Long id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Région non trouvée avec l'ID: " + id));

        // Vérifier si la région est associée à des utilisateurs ou admins
        if (!region.getUsers().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer une région associée à " +
                    region.getUsers().size() + " utilisateur(s)");
        }

        if (!region.getAdmins().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer une région associée à " +
                    region.getAdmins().size() + " administrateur(s)");
        }

        String regionName = region.getName();
        String regionCode = region.getCode();
        String directionName = region.getDirection() != null ? region.getDirection().getName() : "Aucune";

        regionRepository.delete(region);

        // Audit de la suppression de région
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.REGION_DELETED,
                "Region",
                id,
                "Région supprimée: " + regionName + " (Code: " + regionCode + ")" +
                        " (Direction: " + directionName + ")",
                userEmail
        );
    }

    /**
     * Trouver les régions par direction
     */
    public List<Region> findByDirection(Direction direction) {
        return regionRepository.findByDirection(direction);
    }
}