package com.example.projetpfe.config;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.Role;
import com.example.projetpfe.repository.RoleRepository;
import com.example.projetpfe.service.Impl.RegionService;
import com.example.projetpfe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initialise les régions et les utilisateurs administrateurs au démarrage de l'application
 */
@Component
@Order(1) // Exécuter avant AdminInitializer
public class RegionAndAdminInitializer implements CommandLineRunner {

    @Autowired
    private RegionService regionService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Initialiser les régions
        regionService.initializeRegions();

        // Créer le rôle SUPER_ADMIN s'il n'existe pas
        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN");
        if (superAdminRole == null) {
            superAdminRole = new Role();
            superAdminRole.setName("ROLE_SUPER_ADMIN");
            roleRepository.save(superAdminRole);
        }

        // Vérifier s'il existe déjà un super admin
        long superAdminCount = userService.findAllSuperAdmins().size();

        if (superAdminCount == 0) {
            // Créer un super admin par défaut
            UserDto superAdmin = new UserDto();
            superAdmin.setFirstName("Super");
            superAdmin.setLastName("Admin");
            superAdmin.setEmail("superadmin@example.com");
            superAdmin.setPassword("superadmin123");
            superAdmin.setEnabled(true);

            // Enregistrer le super admin
            userService.saveSuperAdmin(superAdmin);

            System.out.println("Super Admin créé avec succès !");
            System.out.println("Email: superadmin@example.com");
            System.out.println("Mot de passe: superadmin123");
        }
    }
}