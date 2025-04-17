package com.example.projetpfe.service.Impl;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.AuditType;
import com.example.projetpfe.entity.Role;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.RoleRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;



@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    boolean passwordChanged = false;

    @Autowired
    private AuditService auditService;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
    }

    @Override
    public void saveUser(UserDto userDto, String roleName) {
        User user = new User();
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        //encrypt the password once we integrate spring security
        //user.setPassword(userDto.getPassword());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        Role role;
        if ("ADMIN".equals(roleName)) {
            role = roleRepository.findByName("ROLE_ADMIN");
            if(role == null){
                role = new Role();
                role.setName("ROLE_ADMIN");
                role = roleRepository.save(role);
            }
        } else {
            role = roleRepository.findByName("ROLE_USER");
            if(role == null){
                role = checkUserRoleExist();
            }
        }

        user.setRoles(Arrays.asList(role));
        User savedUser = userRepository.save(user);

        // Récupérer l'admin qui crée l'utilisateur
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();

        // Audit de la création d'utilisateur
        auditService.auditEvent(AuditType.USER_CREATED,
                "User",
                savedUser.getId(),
                "Utilisateur créé: " + savedUser.getName() + " (" + savedUser.getEmail() + ") avec rôle " + roleName,
                adminEmail);
    }
    private Role checkUserRoleExist() {
        Role role = new Role();
        role.setName("ROLE_USER");
        return roleRepository.save(role);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map((user) -> convertEntityToDto(user))
                .collect(Collectors.toList());
    }

    private UserDto convertEntityToDto(User user){
        UserDto userDto = new UserDto();
        String[] name = user.getName().split(" ");
        userDto.setFirstName(name[0]);
        userDto.setLastName(name[1]);
        userDto.setEmail(user.getEmail());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setEnabled(user.getEnabled());
        userDto.setId(user.getId());
        return userDto;
    }

    //private Role checkRoleExist() {
      //  Role role = new Role();
        //role.setName("ROLE_ADMIN");
        //return roleRepository.save(role);
    //}
    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));

        // Vérifier que l'utilisateur n'est pas l'admin par défaut
//        if ("admin@example.com".equals(user.getEmail())) {
//            throw new RuntimeException("Impossible de supprimer l'administrateur par défaut");
//        }

        // Supprimer les associations avec les rôles avant de supprimer l'utilisateur
        user.getRoles().clear();
        userRepository.save(user); // Enregistrer les changements

        // Maintenant supprimer l'utilisateur
        userRepository.delete(user);

        // Récupérer l'admin qui supprime l'utilisateur
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();

        // Audit de la suppression d'utilisateur
        auditService.auditEvent(AuditType.USER_DELETED,
                "User",
                id,
                "Utilisateur supprimé: " + user.getName() + " (" + user.getEmail() + ")",
                adminEmail);
    }

    @Override
    public UserDto findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        return convertEntityToDto(user);
    }

    @Override
    public void updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));

        // Sauvegarder les anciennes valeurs pour l'audit
        String oldName = user.getName();
        String oldEmail = user.getEmail();
        Boolean oldEnabled = user.getEnabled();

        //Mise à jour des informations
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setEnabled(userDto.getEnabled());

        // Ne mettre à jour le mot de passe que s'il est fourni
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        userRepository.save(user);
        // Récupérer l'admin qui modifie l'utilisateur
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();

        // Préparer les détails des modifications pour l'audit
        StringBuilder changes = new StringBuilder();
        if (!oldName.equals(user.getName())) {
            changes.append("Nom modifié: ").append(oldName).append(" -> ").append(user.getName()).append(", ");
        }
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            passwordChanged = true;
        }
        if (!oldEmail.equals(user.getEmail())) {
            changes.append("Email modifié: ").append(oldEmail).append(" -> ").append(user.getEmail()).append(", ");
        }
        if (oldEnabled != user.getEnabled()) {
            changes.append("Statut modifié: ").append(oldEnabled ? "Actif" : "Inactif")
                    .append(" -> ").append(user.getEnabled() ? "Actif" : "Inactif").append(", ");
        }
        if (passwordChanged) {
            changes.append("Mot de passe modifié, ");
        }

        // Supprimer la virgule finale s'il y a des changements
        String changesStr = changes.toString();
        if (changesStr.endsWith(", ")) {
            changesStr = changesStr.substring(0, changesStr.length() - 2);
        }

        // Audit de la mise à jour d'utilisateur
        auditService.auditEvent(AuditType.USER_UPDATED,
                "User",
                id,
                "Utilisateur modifié: " + user.getName() +
                        (changesStr.isEmpty() ? "" : " (" + changesStr + ")"),
                adminEmail);
    }
}
