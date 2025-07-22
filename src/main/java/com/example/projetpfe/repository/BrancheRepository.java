package com.example.projetpfe.repository;

import com.example.projetpfe.entity.Branche;
import com.example.projetpfe.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrancheRepository extends JpaRepository<Branche, Long> {
    Optional<Branche> findByCode(String code);

    // CHANGEMENT: Remplacer findByRegionCode par findByRegion
    List<Branche> findByRegion(Region region);

    // Méthodes utiles supplémentaires
    List<Branche> findByRegionId(Long regionId);

    // Pour vérifier l'unicité du code
    boolean existsByCode(String code);

    // Pour vérifier l'unicité du nom d'affichage
    boolean existsByDisplayname(String displayname);

    // Trouver par nom d'affichage
    Optional<Branche> findByDisplayname(String displayname);
}