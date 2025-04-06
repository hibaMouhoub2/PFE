package com.example.projetpfe.repository;

import com.example.projetpfe.entity.*;
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
    @Query("SELECT c FROM Client c WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.cin) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Client> searchByNomOrPrenomOrCin(@Param("query") String query);
    long countByAssignedUserAndStatus(User user, ClientStatus status);
    List<Client> findByStatus(ClientStatus status);
    List<Client> findByStatusAndUpdatedAtBetween(ClientStatus status, LocalDateTime start, LocalDateTime end);
    List<Client> findByRendezVousAgenceTrueAndDateHeureRendezVousBetween(LocalDateTime start, LocalDateTime end);
    List<Client> findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenAndNMBRA(
            LocalDateTime start, LocalDateTime end, Branche branche);
    long countByStatus(ClientStatus status);
    @Query("SELECT c FROM Client c WHERE c.cin = :query OR c.telephone = :query OR c.telephone2 = :query")
    List<Client> findByCinOrPhone(@Param("query") String query);
    @Query("SELECT c FROM Client c WHERE " +
            "(:query IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.cin) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:userId IS NULL OR c.assignedUser.id = :userId)")
    List<Client> findByFilters(@Param("query") String query,
                               @Param("status") ClientStatus status,
                               @Param("userId") Long userId);
    boolean existsByCin(String cin);
    long countByRaisonNonRenouvellement(RaisonNonRenouvellement raison);
    long countByQualiteService(QualiteService qualite);
    long countByInteretNouveauCredit(InteretCredit interet);
    long countByFacteurInfluence(FacteurInfluence facteur);
    long countByProfil(Profil profil);
    long countByActiviteClient(ActiviteClient activite);
    long countByRendezVousAgence(Boolean rendezVous);
    long countByNMBRA(Branche branche);

    // Pour les tendances temporelles
    @Query("SELECT COUNT(c) FROM Client c WHERE c.status = :status AND c.updatedAt BETWEEN :start AND :end")
    long countByStatusAndUpdatedAtBetween(@Param("status") ClientStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    }