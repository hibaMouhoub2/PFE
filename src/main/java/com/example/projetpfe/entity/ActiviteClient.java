package com.example.projetpfe.entity;

public enum ActiviteClient {
    COMMERCE("Commerce"),
    METIER_MANUEL("Métier manuel"),
    SERVICE("Service"),
    SALARIE("Salarié");

    private final String displayName;

    ActiviteClient(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}