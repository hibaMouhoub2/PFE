package com.example.projetpfe.entity;

import jakarta.persistence.*;
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
@Entity
@Table(name="users")
public class User
{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false, unique=true)
    private String email;

    @Column(nullable=false)
    private String password;

    private LocalDateTime createdAt;

    private Boolean enabled;

    // Pour les administrateurs régionaux, ils peuvent être responsables de plusieurs régions
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "admin_regions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "region_id")
    )
    private List<Region> regions = new ArrayList<>();

    // Pour les utilisateurs normaux, ils appartiennent à une seule région
    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    // L'administrateur qui a créé cet utilisateur
    @ManyToOne
    @JoinColumn(name = "created_by_admin_id")
    private User createdByAdmin;

    // Les utilisateurs créés par cet administrateur
    @OneToMany(mappedBy = "createdByAdmin")
    private List<User> createdUsers = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
    @JoinTable(
            name="users_roles",
            joinColumns={@JoinColumn(name="USER_ID", referencedColumnName="ID")},
            inverseJoinColumns={@JoinColumn(name="ROLE_ID", referencedColumnName="ID")})
    private List<Role> roles = new ArrayList<>();

}