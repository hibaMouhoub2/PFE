// src/test/java/com/example/projetpfe/config/TestConfiguration.java

package com.example.projetpfe.config;

import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.RoleRepository;
import com.example.projetpfe.repository.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

/**
 * Configuration de test pour créer des données de test réutilisables
 */
@org.springframework.boot.test.context.TestConfiguration
public class TestData {

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Crée un utilisateur de test standard
     */
    public static User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("$2a$10$encoded.password");
        user.setCreatedAt(LocalDateTime.now());
//        user.setUpdatedAt(LocalDateTime.now());
//        user.setPasswordChanged(false);

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");
        user.setRoles(Collections.singletonList(userRole));

        return user;
    }

    /**
     * Crée un utilisateur admin de test
     */
    public static User createTestAdmin() {
        User admin = createTestUser();
        admin.setId(2L);
        admin.setEmail("admin@example.com");
        admin.setName("Test Admin");

        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ROLE_ADMIN");
        admin.setRoles(Collections.singletonList(adminRole));

        return admin;
    }

    /**
     * Crée un client de test
     */
    public static Client createTestClient() {
        Client client = new Client();
        client.setId(1L);
        client.setNom("Test Client");
        client.setPrenom("Jean");
        client.setTelephone("0612345678");
        client.setStatus(ClientStatus.NON_TRAITE);
        client.setNMBRA(Branche.CASA_AZHAR);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());

        return client;
    }

    /**
     * Crée un rappel de test
     */
    public static Rappel createTestRappel() {
        Rappel rappel = new Rappel();
        rappel.setId(1L);
        rappel.setClient(createTestClient());
        //rappel.setUser(createTestUser());
        rappel.setDateRappel(LocalDateTime.now().plusDays(1));
        rappel.setNotes("Test rappel");
        rappel.setCompleted(false);
        rappel.setCreatedAt(LocalDateTime.now());

        return rappel;
    }

    /**
     * Crée un audit de test
     */
    public static Audit createTestAudit() {
        Audit audit = new Audit();
        audit.setId(1L);
        audit.setUser(createTestUser());
        audit.setType(AuditType.CLIENT_STATUS_CHANGE);
        audit.setEntityType("Client");
        audit.setEntityId(1L);
        audit.setDetails("Test audit details");
        audit.setTimestamp(LocalDateTime.now());

        return audit;
    }
}