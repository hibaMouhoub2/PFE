package com.example.projetpfe.entity;

public enum RaisonNonRenouvellement {
    TAUX_INTERET_ELEVE("Taux d'intérêt élevé"),
    PROBLEME_SERVICE("Problème avec le service"),
    CONDITIONS_NON_ADAPTEES("Conditions du crédit non adaptées"),
    PROBLEMES_FINANCIERS("Problèmes financiers"),
    DIFFICULTES_ACTIVITE("Difficultés d'activité"),
    MANQUE_INTERET("Manque d'intérêt"),
    TEMPORISER("Souhait de temporiser"),
    OFFRE_NON_ADAPTEE("Offre non adaptée"),
    AUTRES_SOURCES("Autres sources financières"),
    INSATISFAIT("Insatisfait"),
    AUTRES("Autres raisons à préciser");

    private final String displayName;

    RaisonNonRenouvellement(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}