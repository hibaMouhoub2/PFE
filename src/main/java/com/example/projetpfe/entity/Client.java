package com.example.projetpfe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;
    private String telephone;
    private String email;

    @Enumerated(EnumType.STRING)
    private ClientStatus status = ClientStatus.NON_TRAITE;

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    // Champs du questionnaire
    @Enumerated(EnumType.STRING)
    private RaisonNonRenouvellement raisonNonRenouvellement;

    @Enumerated(EnumType.STRING)
    private QualiteService qualiteService;

    private Boolean aDifficultesRencontrees;
    private String precisionDifficultes;

    @Enumerated(EnumType.STRING)
    private InteretCredit interetNouveauCredit;

    private Boolean rendezVousAgence;
    private LocalDateTime dateHeureRendezVous;

    @Enumerated(EnumType.STRING)
    private FacteurInfluence facteurInfluence;

    private String autresFacteurs;

    // Audit fields
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}