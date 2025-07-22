package com.example.projetpfe.controller;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.Branche;
import com.example.projetpfe.entity.Direction;
import com.example.projetpfe.entity.Region;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.BrancheRepository;
import com.example.projetpfe.repository.DirectionRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.security.LoginAttemptService;
import com.example.projetpfe.service.Impl.RegionService;
import com.example.projetpfe.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private UserService userService;

    @Autowired
    private DirectionRepository directionRepository;

    @Autowired
    private RegionService regionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrancheRepository brancheRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("index")
    public String home(){
        return "index";
    }

    // Méthode pour enregistrer un utilisateur standard - accessible uniquement aux admins régionaux
    @GetMapping("register")
    @PreAuthorize("hasRole('ADMIN')")
    public String showRegistrationForm(Model model){
        UserDto user = new UserDto();
        model.addAttribute("user", user);

        // Récupérer l'authentification correctement
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User admin = userRepository.findByEmail(auth.getName());

        // Filtrer les branches selon la direction de l'admin
        List<Branche> branches;
        if (admin.getDirection() != null) {
            // Récupérer les codes de régions de la direction
            List<String> regionCodes = admin.getDirection().getRegions().stream()
                    .map(Region::getCode)
                    .collect(Collectors.toList());

            // Filtrer les branches par ces codes de régions
            branches = brancheRepository.findAll().stream()
                    .filter(branche -> {
                        String brancheRegionCode = branche.getRegion().getCode();
                        return brancheRegionCode != null && regionCodes.contains(brancheRegionCode);
                    })
                    .collect(Collectors.toList());
        } else {
            // Si pas de direction, donner toutes les branches
            branches = brancheRepository.findAll();
        }

        model.addAttribute("branches", branches);

        // Régions gérées par l'admin
        List<Region> adminRegions = admin.getRegions();
        model.addAttribute("regions", adminRegions);

        return "register";
    }
    // Méthode pour enregistrer un admin régional - accessible uniquement aux super admins
    @GetMapping("register-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String showAdminRegistrationForm(Model model) {
        UserDto admin = new UserDto();
        model.addAttribute("admin", admin);

        // Au lieu de charger les régions, charger les directions
        List<Direction> directions = directionRepository.findAll();
        model.addAttribute("directions", directions);

        return "register-admin";
    }

    // Méthode pour enregistrer un super admin - accessible uniquement aux super admins
    @GetMapping("register-super-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String showSuperAdminRegistrationForm(Model model){
        UserDto superAdmin = new UserDto();
        model.addAttribute("superAdmin", superAdmin);
        return "register-super-admin";
    }

    // Traitement de l'enregistrement d'un utilisateur standard
    @PostMapping("/register/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String registerUser(@Valid @ModelAttribute("user") UserDto user,
                               BindingResult result,
                               Model model,
                               Authentication authentication) {

        User existing = userService.findByEmail(user.getEmail());
        if (existing != null) {
            result.rejectValue("email", null, "Il existe déjà un compte avec cet email");
        }

        if (result.hasErrors()) {
            // Récupérer les régions gérées par l'admin connecté en cas d'erreur
            User admin = userService.findByEmail(authentication.getName());
            List<Region> adminRegions = admin.getRegions();
            model.addAttribute("regions", adminRegions);
            return "register";
        }

        // Enregistrer l'utilisateur via l'admin connecté
        userService.saveUserByAdmin(user, authentication.getName());
        return "redirect:/users";
    }

    // Traitement de l'enregistrement d'un admin régional
    @PostMapping("/register-admin/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String registerAdmin(@Valid @ModelAttribute("admin") UserDto admin,
                                BindingResult result,
                                @RequestParam Long selectedDirection,
                                Model model) {

        User existing = userService.findByEmail(admin.getEmail());
        if (existing != null) {
            result.rejectValue("email", null, "Il existe déjà un compte avec cet email");
        }

        if (selectedDirection == null) {
            model.addAttribute("directionError", "Veuillez sélectionner une direction");
            model.addAttribute("directions", directionRepository.findAll());
            return "register-admin";
        }

        if (result.hasErrors()) {
            model.addAttribute("directions", directionRepository.findAll());
            return "register-admin";
        }

        // Utiliser la direction au lieu de la région
        userService.saveDirectionAdmin(admin, selectedDirection);
        return "redirect:/users";
    }

    // Traitement de l'enregistrement d'un super admin
    @PostMapping("/register-super-admin/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String registerSuperAdmin(@Valid @ModelAttribute("superAdmin") UserDto superAdmin,
                                     BindingResult result,
                                     Model model) {

        User existing = userService.findByEmail(superAdmin.getEmail());
        if (existing != null) {
            result.rejectValue("email", null, "Il existe déjà un compte avec cet email");
        }

        if (result.hasErrors()) {
            return "register-super-admin";
        }

        // Enregistrer le super admin
        userService.saveSuperAdmin(superAdmin);
        return "redirect:/users";
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String listRegisteredUsers(Model model, Authentication authentication){
        // Différencier l'affichage en fonction du rôle
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (isSuperAdmin) {
            // Le super admin voit tous les utilisateurs
            List<UserDto> users = userService.findAllUsers();
            model.addAttribute("users", users);
        } else {
            // L'admin régional voit ses utilisateurs créés
            User admin = userService.findByEmail(authentication.getName());
            List<User> createdUsers = admin.getCreatedUsers();
            List<UserDto> userDtos = createdUsers.stream()
                    .map(user -> {
                        UserDto dto = new UserDto();
                        String[] name = user.getName().split(" ");
                        dto.setFirstName(name.length > 0 ? name[0] : "");
                        dto.setLastName(name.length > 1 ? name[1] : "");
                        dto.setEmail(user.getEmail());
                        dto.setCreatedAt(user.getCreatedAt());
                        dto.setEnabled(user.getEnabled());
                        dto.setId(user.getId());
                        return dto;
                    })
                    .collect(Collectors.toList());
            model.addAttribute("users", userDtos);
        }

        return "users";
    }

    @GetMapping("/login-success")
    public String loginPageRedirect(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        if (roles.contains("ROLE_SUPER_ADMIN")) {
            return "redirect:/admin/agenda";
        } else if (roles.contains("ROLE_ADMIN")) {
            return "redirect:/admin/agenda";
        } else {
            return "redirect:/agenda/index";
        }
    }

    @GetMapping("/login")
    public String loginForm(Model model, HttpServletRequest request, Authentication authentication) {
        // Si déjà authentifié, rediriger
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/index";
        }

        String ip = getClientIP(request);

        logger.debug("Accès à la page de login - Query string: {}", request.getQueryString());
        logger.debug("Vérification du blocage pour l'IP: {}", ip);

        if (loginAttemptService.isBlocked(ip)) {
            long remainingMinutes = loginAttemptService.getRemainingBlockTimeInMinutes(ip);
            logger.debug("IP bloquée, temps restant: {} minutes", remainingMinutes);

            model.addAttribute("isBlocked", true);
            model.addAttribute("remainingMinutes", remainingMinutes);
        } else {
            logger.debug("IP non bloquée");
            model.addAttribute("isBlocked", false);
        }

        return "login";
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return xfHeader != null ? xfHeader.split(",")[0] : request.getRemoteAddr();
    }

    // Supprimer un utilisateur
    @GetMapping("/delete-user/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            // Vérifier si l'utilisateur a le droit de supprimer cet utilisateur
            User userToDelete = userService.findById(id);
            User currentUser = userService.findByEmail(authentication.getName());

            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

            // Un super admin peut supprimer n'importe qui sauf un autre super admin
            if (isSuperAdmin) {
                if (userService.isSuperAdmin(userToDelete) && !userToDelete.getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas supprimer un autre super administrateur");
                    return "redirect:/users?error=deleteFailed";
                }
            }
            // Un admin régional ne peut supprimer que ses propres utilisateurs
            else if (userService.isRegionalAdmin(currentUser)) {
                if (userToDelete.getCreatedByAdmin() == null || !userToDelete.getCreatedByAdmin().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez supprimer que les utilisateurs que vous avez créés");
                    return "redirect:/users?error=deleteFailed";
                }
            }

            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "L'utilisateur a été supprimé avec succès");
            return "redirect:/users?success=userDeleted";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression: " + e.getMessage());
            return "redirect:/users?error=deleteFailed";
        }
    }

    @GetMapping("/edit-user/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            UserDto user = userService.findUserById(id);
            User currentUser = userService.findByEmail(authentication.getName());

            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

            // Si c'est un admin régional qui veut modifier un utilisateur
            if (!isSuperAdmin && userService.isRegionalAdmin(currentUser)) {
                User userToEdit = userService.findById(id);
                // Vérifier si l'utilisateur est créé par cet admin ou s'il essaie de modifier un autre admin
                if ((userToEdit.getCreatedByAdmin() == null || !userToEdit.getCreatedByAdmin().getId().equals(currentUser.getId()))
                        && userService.isRegionalAdmin(userToEdit)) {
                    return "redirect:/users?error=unauthorizedEdit";
                }

                // Ajouter les régions gérées par l'admin
                model.addAttribute("regions", currentUser.getRegions());
            } else if (isSuperAdmin) {
                // Si c'est un super admin, ajouter toutes les régions
                model.addAttribute("regions", regionService.findAll());
                model.addAttribute("isSuperAdmin", true);
            }

            model.addAttribute("user", user);
            return "edit-user";
        } catch (Exception e) {
            return "redirect:/users?error=userNotFound";
        }
    }

    @PostMapping("/edit-user/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("user") UserDto userDto,
                             BindingResult result,
                             @RequestParam(required = false) List<Long> selectedRegions,
                             Model model,
                             Authentication authentication) {

        if (result.hasErrors()) {
            // En cas d'erreur, réafficher le formulaire avec les régions appropriées
            User currentUser = userService.findByEmail(authentication.getName());
            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

            if (isSuperAdmin) {
                model.addAttribute("regions", regionService.findAll());
                model.addAttribute("isSuperAdmin", true);
            } else {
                model.addAttribute("regions", currentUser.getRegions());
            }

            return "edit-user";
        }

        try {
            // Vérifier l'autorisation de modification
            User userToEdit = userService.findById(id);
            User currentUser = userService.findByEmail(authentication.getName());

            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

            // Un admin régional ne peut modifier que ses propres utilisateurs
            if (!isSuperAdmin && userService.isRegionalAdmin(currentUser)) {
                if ((userToEdit.getCreatedByAdmin() == null || !userToEdit.getCreatedByAdmin().getId().equals(currentUser.getId()))
                        && userService.isRegionalAdmin(userToEdit)) {
                    return "redirect:/users?error=unauthorizedEdit";
                }
            }

            // Si c'est un super admin et qu'il modifie un admin régional, mettre à jour les régions
            if (isSuperAdmin && userService.isRegionalAdmin(userToEdit) && selectedRegions != null) {
                // Mettre à jour la liste des régions pour l'admin régional
                userToEdit.getRegions().clear();
                List<Region> regions = regionService.findAll().stream()
                        .filter(region -> selectedRegions.contains(region.getId()))
                        .collect(Collectors.toList());
                userToEdit.getRegions().addAll(regions);
            }

            userService.updateUser(id, userDto);
            return "redirect:/users?success=userUpdated";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors de la mise à jour: " + e.getMessage());
            return "edit-user";
        }
    }
}