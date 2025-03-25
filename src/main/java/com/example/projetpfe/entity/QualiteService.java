package com.example.projetpfe.entity;

public enum QualiteService {
    EXCELLENTE("Excellente"),
    BONNE("Bonne"),
    MOYENNE("Moyenne"),
    MAUVAISE("Mauvaise");

    private final String displayName;

    QualiteService(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}