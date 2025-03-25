package com.example.projetpfe.dto;

import com.example.projetpfe.entity.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {
    private Long id;

    @NotEmpty(message = "Le nom est requis")
    private String nom;

    @NotEmpty(message = "Le prénom est requis")
    private String prenom;

    @NotEmpty(message = "Le téléphone est requis")
    private String telephone;

    private String email;

    private ClientStatus status;

    private Long assignedUserId;

    // Champs du questionnaire
    private RaisonNonRenouvellement raisonNonRenouvellement;
    private QualiteService qualiteService;
    private Boolean aDifficultesRencontrees;
    private String precisionDifficultes;
    private InteretCredit interetNouveauCredit;
    private Boolean rendezVousAgence;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dateHeureRendezVous;

    private FacteurInfluence facteurInfluence;
    private String autresFacteurs;
}