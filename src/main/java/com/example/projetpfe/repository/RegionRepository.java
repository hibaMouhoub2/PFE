package com.example.projetpfe.repository;

import com.example.projetpfe.entity.Direction;
import com.example.projetpfe.entity.Region;
import com.example.projetpfe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    Region findByName(String name);
    Region findByCode(String code);
    boolean existsByName(String name);
    boolean existsByCode(String code);
    List<Region> findByDirection(Direction direction);
}