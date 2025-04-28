package com.example.projetpfe.repository;

import com.example.projetpfe.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByAssignedUserOrderByUpdatedAtDesc(User user);
    List<Client> findByAssignedUserAndStatusOrderByUpdatedAtDesc(User user, ClientStatus status);
    List<Client> findByAssignedUserIsNullOrderByUpdatedAtDesc();
    @Query("SELECT c FROM Client c WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.cin) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY c.updatedAt DESC")
    List<Client> searchByNomOrPrenomOrCin(@Param("query") String query);
    long countByAssignedUserAndStatus(User user, ClientStatus status);
    List<Client> findByStatusOrderByUpdatedAtDesc(ClientStatus status);
    List<Client> findByStatusAndUpdatedAtBetweenOrderByUpdatedAtDesc(ClientStatus status, LocalDateTime start, LocalDateTime end);
    List<Client> findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenOrderByUpdatedAtDesc
            (LocalDateTime start, LocalDateTime end);
    List<Client> findByRendezVousAgenceTrueAndDateHeureRendezVousBetweenAndNMBRAOrderByUpdatedAtDesc(
            LocalDateTime start, LocalDateTime end, Branche branche);
    long countByStatus(ClientStatus status);
    @Query("SELECT c FROM Client c WHERE c.cin = :query OR c.telephone = :query OR c.telephone2 = :query ORDER BY c.updatedAt DESC")
    List<Client> findByCinOrPhoneOrderByUpdatedAtDesc(@Param("query") String query);
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
    List<Client> findByRaisonNonRenouvellement(RaisonNonRenouvellement raison);
    List<Client> findByQualiteService(QualiteService qualite);
    List<Client> findByInteretNouveauCredit(InteretCredit interet);
    List<Client> findByFacteurInfluence(FacteurInfluence facteur);
    List<Client> findByProfil(Profil profil);
    List<Client> findByActiviteClient(ActiviteClient activite);

    List<Client> findByRendezVousAgence(Boolean rendezVous);
   List<Client> findByNMBRA(Branche branche);
    @Query("SELECT c FROM Client c WHERE " +
            "(:query IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.cin) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:userId IS NULL OR c.assignedUser.id = :userId)" +
            "ORDER BY c.updatedAt DESC"
    )
    List<Client> findByFilters(@Param("query") String query,
                               @Param("status") ClientStatus status,
                               @Param("userId") Long userId);

    @Query("SELECT c FROM Client c WHERE " +
            "(:query IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.cin) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:userId IS NULL OR c.assignedUser.id = :userId) AND " +
            "(:branche IS NULL OR c.NMBRA = :branche)" +
            "ORDER BY c.updatedAt DESC"
    )
    List<Client> findByFiltersWithBranche(@Param("query") String query,
                                          @Param("status") ClientStatus status,
                                          @Param("userId") Long userId,
                                          @Param("branche") Branche branche);



    long countByRaisonNonRenouvellementAndUpdatedAtBetween(RaisonNonRenouvellement raison, LocalDateTime start, LocalDateTime end);
    long countByQualiteServiceAndUpdatedAtBetween(QualiteService qualite, LocalDateTime start, LocalDateTime end);
    long countByInteretNouveauCreditAndUpdatedAtBetween(InteretCredit interet, LocalDateTime start, LocalDateTime end);
    long countByFacteurInfluenceAndUpdatedAtBetween(FacteurInfluence facteur, LocalDateTime start, LocalDateTime end);
    long countByProfilAndUpdatedAtBetween(Profil profil, LocalDateTime start, LocalDateTime end);
    long countByActiviteClientAndUpdatedAtBetween(ActiviteClient activite, LocalDateTime start, LocalDateTime end);
    long countByRendezVousAgenceAndUpdatedAtBetween(Boolean rendezVous, LocalDateTime start, LocalDateTime end);
    long countByNMBRAAndUpdatedAtBetween(Branche branche, LocalDateTime start, LocalDateTime end);

    List<Client> findByStatusAndNMREGInOrderByUpdatedAtDesc(ClientStatus status, List<String> regionCodes);

    long countByStatusAndNMREGIn(ClientStatus status, List<String> regionCodes);

    List<Client> findByAssignedUserIsNullAndNMREGInOrderByUpdatedAtDesc(List<String> regionCodes);
    }