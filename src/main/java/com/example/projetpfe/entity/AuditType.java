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
    ADMIN_CREATED("Création d'administrateur régional"),
    ADMIN_UPDATED("Mise à jour d'administrateur régional"),
    ADMIN_DELETED("Suppression d'administrateur régional"),
    ADMIN_ASSIGNED_REGION("Association admin-région"),
    ADMIN_REMOVED_REGION("Dissociation admin-région"),
    CLIENT_PHONE_CHANGED("Modification du téléphone client"),

    // Actions sur les régions
    REGION_CREATED("Création de région"),
    REGION_UPDATED("Mise à jour de région"),
    REGION_DELETED("Suppression de région"),

    // Actions sur les directions
    DIRECTION_CREATED("Création de direction"),
    DIRECTION_UPDATED("Mise à jour de direction"),
    DIRECTION_DELETED("Suppression de direction"),

    // Actions sur les branches - NOUVEAU
    BRANCHE_CREATED("Création de branche"),
    BRANCHE_UPDATED("Mise à jour de branche"),
    BRANCHE_DELETED("Suppression de branche"),

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