package com.example.projetpfe.repository;

import com.example.projetpfe.entity.Client;
import com.example.projetpfe.entity.ClientStatus;
import com.example.projetpfe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByAssignedUser(User user);
    List<Client> findByAssignedUserAndStatus(User user, ClientStatus status);
    List<Client> findByAssignedUserIsNull();
    @Query("SELECT c FROM Client c WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Client> searchByNomOrPrenom(@Param("query") String query);
    long countByAssignedUserAndStatus(User user, ClientStatus status);
    List<Client> findByStatus(ClientStatus status);
    List<Client> findByStatusAndUpdatedAtBetween(ClientStatus status, LocalDateTime start, LocalDateTime end);
    List<Client> findByRendezVousAgenceTrueAndDateHeureRendezVousBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(ClientStatus status);
}