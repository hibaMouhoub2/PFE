package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.AuditType;
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

    @Transactional
    public void deleteRegion(Long id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Région non trouvée avec l'ID: " + id));

        // Vérifier si la région est associée à des utilisateurs ou admins
        if (!region.getUsers().isEmpty() || !region.getAdmins().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer une région associée à des utilisateurs ou administrateurs");
        }

        String regionName = region.getName();
        String regionCode = region.getCode();

        regionRepository.delete(region);

        // Audit de la suppression de région
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.REGION_DELETED,
                "Region",
                id,
                "Région supprimée: " + regionName + " (Code: " + regionCode + ")",
                userEmail
        );
    }

    /**
     * Initialise les régions de base dans la base de données si elles n'existent pas
     */
    @Transactional
    public void initializeRegions() {
        // Liste des régions à initialiser
        String[][] regionsData = {
                {"Casablanca-Settat", "CASA_SETTAT"},
                {"Rabat-Salé-Kénitra", "RABAT_SALE"},
                {"Fès-Meknès", "FES_MEKNES"},
                {"Tanger-Tétouan-Al Hoceima", "TANGER_TETOUAN"},
                {"Marrakech-Safi", "MARRAKECH_SAFI"},
                {"Souss-Massa", "SOUSS_MASSA"},
                {"Oriental", "ORIENTAL"},
                {"Béni Mellal-Khénifra", "BENI_MELLAL"},
                {"Drâa-Tafilalet", "DRAA_TAFILALET"},
                {"Guelmim-Oued Noun", "GUELMIM"},
                {"Laâyoune-Sakia El Hamra", "LAAYOUNE"},
                {"Dakhla-Oued Ed-Dahab", "DAKHLA"}
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
}