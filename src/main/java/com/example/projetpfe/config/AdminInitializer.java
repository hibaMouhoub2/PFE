package com.example.projetpfe.config;

import com.example.projetpfe.entity.Role;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.RoleRepository;
import com.example.projetpfe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;


@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // créer les rôles
        long AdminCount = userRepository.countByRolesName("ROLE_ADMIN");
        if (AdminCount == 0) {
            Role adminRole = createRoleIfNotFound("ROLE_ADMIN");
            Role userRole = createRoleIfNotFound("ROLE_USER");
            User admin = new User();
            admin.setName("Admin User");
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("adminfondation"));
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());

            // Utiliser le rôle déjà persisté
            admin.setRoles(Arrays.asList(adminRole));
            userRepository.save(admin);
        }

    }
    private Role createRoleIfNotFound(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role();
            role.setName(name);
            role = roleRepository.save(role);
        }
        return role;
    }
}
