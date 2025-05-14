package com.example.projetpfe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "regions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Region {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    // une région appartient à une direction
    @ManyToOne
    @JoinColumn(name = "direction_id", nullable = true)
    private Direction direction;

    // Un admin régional peut être responsable de plusieurs régions
    @ManyToMany(mappedBy = "regions")
    private List<User> admins = new ArrayList<>();

    // Les utilisateurs appartenant à cette région
    @OneToMany(mappedBy = "region")
    private List<User> users = new ArrayList<>();
}