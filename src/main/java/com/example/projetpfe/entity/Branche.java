package com.example.projetpfe.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@NoArgsConstructor
@Table(name = "branches")
@AllArgsConstructor
@Getter
@Setter
public class Branche {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String displayname;
    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

}
