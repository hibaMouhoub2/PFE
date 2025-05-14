package com.example.projetpfe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    // Relation avec les régions (une direction a plusieurs régions)
    @OneToMany(mappedBy = "direction", cascade = CascadeType.ALL)
    private List<Region> regions = new ArrayList<>();

    // Relation avec les administrateurs (une direction peut avoir plusieurs admins)
    @OneToMany(mappedBy = "direction")
    private List<User> directionalAdmins = new ArrayList<>();
}
