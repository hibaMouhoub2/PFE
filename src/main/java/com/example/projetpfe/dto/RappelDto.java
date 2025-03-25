package com.example.projetpfe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RappelDto {
    private Long id;

    @NotNull(message = "L'ID du client est requis")
    private Long clientId;

    @NotNull(message = "La date de rappel est requise")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dateRappel;

    private String notes;
    private Boolean completed = false;
}