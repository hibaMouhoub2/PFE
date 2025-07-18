package com.example.projetpfe.repository;

import com.example.projetpfe.entity.Branche;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrancheRepository extends JpaRepository<Branche, Long> {
    Optional<Branche> findByCode(String code);
    List<Branche> findByRegionCode(String regionCode);
}

