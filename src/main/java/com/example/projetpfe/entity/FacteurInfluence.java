package com.example.projetpfe.entity;

public enum FacteurInfluence {
    RAPIDITE_DEMARCHES("Rapidité des démarches"),
    SERVICE_CLIENT("Service client"),
    TAUX_INTERET("Taux d'intérêt"),
    OFFRES_SPECIALES("Offres spéciales"),
    DUREE_REMBOURSEMENT("Durée du remboursement"),
    CONDITIONS_CREDIT("Conditions du crédit"),
    REPUTATION("Réputation de l'établissement"),
    AUTRES("Autres critères");

    private final String displayName;

    FacteurInfluence(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}