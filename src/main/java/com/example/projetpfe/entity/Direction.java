package com.example.projetpfe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "directions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Direction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String code; // NMDIR code


    @OneToMany(mappedBy = "direction", cascade = CascadeType.ALL)
    private List<Region> regions = new ArrayList<>();

    // Relation avec les administrateurs (une direction peut avoir plusieurs admins)
    @OneToMany(mappedBy = "direction")
    private List<User> directionalAdmins = new ArrayList<>();

    /**
     * Retourne uniquement les utilisateurs avec le rôle ADMIN pour cette direction
     */
    public List<User> getAdministrators() {
        return this.directionalAdmins.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> "ROLE_ADMIN".equals(role.getName())))
                .collect(Collectors.toList());
    }

    /**
     * Retourne le nombre d'administrateurs (rôle ADMIN) pour cette direction
     */
    public int getAdministratorsCount() {
        return (int) this.directionalAdmins.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> "ROLE_ADMIN".equals(role.getName())))
                .count();
    }
}
