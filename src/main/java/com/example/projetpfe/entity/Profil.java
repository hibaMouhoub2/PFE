package com.example.projetpfe.entity;

public enum Profil {
    AUTO_ENTREPRENEUR("Auto-entrepreneur"),
    TPE("TPE"),
    PROFESSION_LIBERALE("Profession Libérale"),
    SALARIE_SECTEUR_PRIVE("Salarié secteur privé"),
    FONCTIONNAIRE("Fonctionnaire"),
    COOPERATIVE_ADHERANT("Coopérative ou adhérant"),
    AMBULANT("ambulant"),
    TRAVAIL_A_DOMICILE("Travail à domicile"),
    EXPLOITANT_EMPLACEMENT_FIXE("Exploitant emplacement fixe"),
    SANS("Sans");

    private final String displayName;

    Profil(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
