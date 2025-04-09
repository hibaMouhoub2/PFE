package com.example.projetpfe.service.Impl;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.Role;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.RoleRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.UserService;
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
        userRepository.save(user);
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

        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setEnabled(userDto.getEnabled());

        // Ne mettre à jour le mot de passe que s'il est fourni
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        userRepository.save(user);
    }
}
