package com.example.projetpfe.entity;

public enum Branche {
    CASA_AZHAR("Casa Azhar"),
    CASA_DIAR_EL_JADIDA("Casa Diar El jadida"),
    CASA_HAY_FARAH("Casa Hay Farah"),
    CASA_KOREA("Casa Koréa"),;

    private final String displayName;

    Branche(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
