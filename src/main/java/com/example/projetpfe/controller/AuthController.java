package com.example.projetpfe.controller;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.security.LoginAttemptService; // Ajout de l'import manquant
import com.example.projetpfe.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired; // Ajout de l'import manquant
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private UserService userService;

    @Autowired // Ajout de l'injection manquante
    private LoginAttemptService loginAttemptService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("index")
    public String home(){
        return "index";
    }

    // handler method to handle user registration request
    @GetMapping("register")
    public String showRegistrationForm(Model model){
        UserDto user = new UserDto();
        model.addAttribute("user", user);
        return "register";
    }

    // handler method to handle register user form submit request
    @PostMapping("/register/save")
    public String registration(@Valid @ModelAttribute("user") UserDto user,
                               BindingResult result,
                               Model model){
        User existing = userService.findByEmail(user.getEmail());
        if (existing != null) {
            result.rejectValue("email", null, "There is already an account registered with that email");
        }
        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "register";
        }
        userService.saveUser(user);
        return "redirect:/users";
    }

    @GetMapping("/users")
    public String listRegisteredUsers(Model model){
        List<UserDto> users = userService.findAllUsers();
        model.addAttribute("users", users);
        return "users";
    }

    @GetMapping("/login-success")
    public String loginPageRedirect(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        if (roles.contains("ROLE_ADMIN")) {
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
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "L'utilisateur a été supprimé avec succès");
            return "redirect:/users?success=userDeleted";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression: " + e.getMessage());
            return "redirect:/users?error=deleteFailed";
        }
    }

    @GetMapping("/edit-user/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            UserDto user = userService.findUserById(id);
            model.addAttribute("user", user);
            return "edit-user";
        } catch (Exception e) {
            return "redirect:/users?error=userNotFound";
        }
    }

    @PostMapping("/edit-user/{id}")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("user") UserDto userDto,
                             BindingResult result,
                             Model model) {
        if (result.hasErrors()) {
            return "edit-user";
        }

        try {
            userService.updateUser(id, userDto);
            return "redirect:/users?success=userUpdated";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors de la mise à jour: " + e.getMessage());
            return "edit-user";
        }
    }
}