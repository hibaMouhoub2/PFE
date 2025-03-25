package com.example.projetpfe.repository;

import com.example.projetpfe.entity.Rappel;
import com.example.projetpfe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RappelRepository extends JpaRepository<Rappel, Long> {
    @Modifying
    @Query("UPDATE Rappel r SET r.completed = true WHERE r.client.id = :clientId AND r.completed = false")
    void completeAllRappelsForClient(@Param("clientId") Long clientId);
    List<Rappel> findByCompletedFalseOrderByDateRappel();
    List<Rappel> findByClientAssignedUserAndCompletedFalseOrderByDateRappel(User user);
    List<Rappel> findByDateRappelBetweenAndCompletedFalse(LocalDateTime start, LocalDateTime end);
}