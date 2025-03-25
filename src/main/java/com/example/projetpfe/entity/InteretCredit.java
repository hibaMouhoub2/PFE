package com.example.projetpfe.entity;

public enum InteretCredit {
    OUI("OUI"),
    NON("NON"),
    PEUT_ETRE("PEUT ÊTRE DANS LE FUTUR");

    private final String displayName;

    InteretCredit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}