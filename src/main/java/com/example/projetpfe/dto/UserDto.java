package com.example.projetpfe.dto;

import com.example.projetpfe.entity.Branche;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto
{
    private Long id;
    @NotEmpty
    private String firstName;
    @NotEmpty
    private String lastName;
    @NotEmpty(message = "Email should not be empty")
    @Email
    private String email;
    @NotEmpty(message = "Password should not be empty")
    private String password;
    private LocalDateTime createdAt;
    private Boolean enabled;

    // Région unique pour les utilisateurs standard
    private Long regionId;
    private String regionName;
    private Long assignedBrancheId;
    private Branche assignedBranche;

    // Liste de régions pour les administrateurs régionaux
    private List<Long> regionIds = new ArrayList<>();

    // ID de l'administrateur qui a créé cet utilisateur
    private Long createdByAdminId;
    private String createdByAdminName;

}