package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.AuditType;
import com.example.projetpfe.entity.Branche;
import com.example.projetpfe.entity.Region;
import com.example.projetpfe.repository.BrancheRepository;
import com.example.projetpfe.repository.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BrancheService {

    @Autowired
    private BrancheRepository brancheRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Initialise les branches de base dans la base de données si elles n'existent pas
     * Basé sur vos vraies données de branches
     */
    @Transactional
    public void initializeBranches() {
        // Vos vraies branches selon la capture d'écran
        String[][] branchesData = {
                {"CASA_AZHAR", "Casa Azhar", "SUP_BERNOUSSI_ZENATA"},
                {"CASA_DIAR_EL_JADIDA", "Casa Diar El Jadida", "SUP_FIDAA_SIDI_BELYOUT"},
                {"CASA_HAY_FARAH", "Casa Hay Farah", "SUP_BENMSIK_SIDI_OTHMANE"},
                {"CASA_KOREA", "Casa Korea", "SUP_FIDAA_SIDI_BELYOUT"}
        };

        for (String[] brancheData : branchesData) {
            if (brancheRepository.findByCode(brancheData[0]).isEmpty()) {
                Branche branche = new Branche();
                branche.setCode(brancheData[0]);
                branche.setDisplayname(brancheData[1]);

                // CHANGEMENT: Utiliser la relation Region au lieu de regionCode
                Region region = regionRepository.findByCode(brancheData[2]);
                if (region != null) {
                    branche.setRegion(region);
                }

                brancheRepository.save(branche);
                System.out.println("Branche initialisée: " + brancheData[1]);
            }
        }
    }

    public Branche findByCode(String code) {
        return brancheRepository.findByCode(code).orElse(null);
    }

    public List<Branche> findAll() {
        return brancheRepository.findAll();
    }

    public Optional<Branche> findById(Long id) {
        return brancheRepository.findById(id);
    }

    /**
     * Créer une branche avec une région associée
     */
    @Transactional
    public Branche createBrancheWithRegion(String code, String displayname, Region region) {
        // Vérifier si une branche avec le même code existe déjà
        if (brancheRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Une branche avec ce code existe déjà");
        }

        Branche branche = new Branche();
        branche.setCode(code);
        branche.setDisplayname(displayname);
        branche.setRegion(region);

        Branche savedBranche = brancheRepository.save(branche);

        // Audit de la création de branche
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        String regionInfo = region != null ? " (Région: " + region.getName() + ")" : "";
        auditService.auditEvent(
                AuditType.BRANCHE_CREATED,
                "Branche",
                savedBranche.getId(),
                "Branche créée: " + displayname + " (Code: " + code + ")" + regionInfo,
                userEmail
        );

        return savedBranche;
    }

    /**
     * Mettre à jour une branche avec une région
     */
    @Transactional
    public Branche updateBrancheWithRegion(Long id, String code, String displayname, Region region) {
        Branche branche = brancheRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branche non trouvée avec l'ID: " + id));

        // Vérifier si le nouveau code est déjà utilisé par une autre branche
        Optional<Branche> existingByCode = brancheRepository.findByCode(code);
        if (existingByCode.isPresent() && !existingByCode.get().getId().equals(id)) {
            throw new IllegalArgumentException("Une branche avec ce code existe déjà");
        }

        String oldCode = branche.getCode();
        String oldDisplayname = branche.getDisplayname();
        Region oldRegion = branche.getRegion();

        branche.setCode(code);
        branche.setDisplayname(displayname);
        branche.setRegion(region);

        Branche updatedBranche = brancheRepository.save(branche);

        // Audit de la mise à jour de branche
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        String oldRegionName = oldRegion != null ? oldRegion.getName() : "Aucune";
        String newRegionName = region != null ? region.getName() : "Aucune";

        auditService.auditEvent(
                AuditType.BRANCHE_UPDATED,
                "Branche",
                updatedBranche.getId(),
                "Branche mise à jour: " + oldDisplayname + " -> " + displayname +
                        ", Code: " + oldCode + " -> " + code +
                        ", Région: " + oldRegionName + " -> " + newRegionName,
                userEmail
        );

        return updatedBranche;
    }

    /**
     * Supprimer une branche
     */
    @Transactional
    public void deleteBranche(Long id) {
        Branche branche = brancheRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branche non trouvée avec l'ID: " + id));

        // Ici vous pourriez ajouter des vérifications pour s'assurer qu'aucun client n'est assigné à cette branche
        // Par exemple: if (clientRepository.countByNMBRA(branche) > 0) { throw new RuntimeException("Impossible de supprimer une branche avec des clients assignés"); }

        String brancheName = branche.getDisplayname();
        String brancheCode = branche.getCode();

        brancheRepository.delete(branche);

        // Audit de la suppression
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        auditService.auditEvent(
                AuditType.BRANCHE_DELETED,
                "Branche",
                id,
                "Branche supprimée: " + brancheName + " (Code: " + brancheCode + ")",
                userEmail
        );
    }

    /**
     * Trouver toutes les branches d'une région
     */
    public List<Branche> findByRegion(Region region) {
        return brancheRepository.findByRegion(region);
    }
}