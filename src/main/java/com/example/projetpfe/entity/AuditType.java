package com.example.projetpfe.entity;

public enum AuditType {
    // Actions sur les clients
    CLIENT_STATUS_CHANGE("Changement de statut client"),
    CLIENT_QUESTIONNAIRE_COMPLETED("Questionnaire client complété"),
    CLIENT_QUESTIONNAIRE_UPDATED("Questionnaire client modifié"),
    CLIENT_CREATED("Création de client"),
    CLIENT_DELETED("Suppression de client"),

    // Actions d'assignation
    CLIENT_ASSIGNED("Client assigné"),
    CLIENT_REASSIGNED("Client réassigné"),
    CLIENTS_BULK_ASSIGNED("Assignation multiple de clients"),

    // Actions d'importation/exportation
    EXCEL_IMPORT("Importation Excel"),
    EXCEL_EXPORT("Exportation Excel"),
    EMAIL_EXPORT("Envoi d'exportation par email"),

    // Actions utilisateurs
    USER_CREATED("Création d'utilisateur"),
    USER_UPDATED("Mise à jour d'utilisateur"),
    USER_DELETED("Suppression d'utilisateur"),

    // Actions de rappel
    RAPPEL_CREATED("Rappel créé"),
    RAPPEL_COMPLETED("Rappel complété");

    private final String displayName;

    AuditType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
