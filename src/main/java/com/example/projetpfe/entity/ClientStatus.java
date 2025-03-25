package com.example.projetpfe.entity;

public enum ClientStatus {
    NON_TRAITE("Non traité"),
    CONTACTE("Contacté"),
    ABSENT("Absent"),
    REFUS("Refus");

    private final String displayName;

    ClientStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}