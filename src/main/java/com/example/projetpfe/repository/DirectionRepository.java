package com.example.projetpfe.repository;

import com.example.projetpfe.entity.Direction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectionRepository extends JpaRepository<Direction, Long> {
    Direction findByName(String name);
    Direction findByCode(String code);
    boolean existsByName(String name);
    boolean existsByCode(String code);
}
