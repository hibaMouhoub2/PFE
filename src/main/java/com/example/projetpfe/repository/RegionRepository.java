package com.example.projetpfe.repository;

import com.example.projetpfe.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    Region findByName(String name);
    Region findByCode(String code);
    boolean existsByName(String name);
    boolean existsByCode(String code);
}