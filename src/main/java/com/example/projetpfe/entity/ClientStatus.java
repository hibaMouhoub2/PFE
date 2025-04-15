package com.example.projetpfe.entity;

public enum ClientStatus {
    NON_TRAITE("Non traité"),
    CONTACTE("Contacté"),
    ABSENT("Ne répond pas"),
    REFUS("Refus"),
    INJOIGNABLE("Injoignable"),
    NUMERO_ERRONE("Numéro erroné");

    private final String displayName;

    ClientStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}