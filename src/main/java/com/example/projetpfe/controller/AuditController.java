package com.example.projetpfe.controller;

import com.example.projetpfe.entity.Audit;
import com.example.projetpfe.entity.AuditType;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.service.Impl.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String viewAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            Model model) {

        // Définir des dates par défaut si non fournies
        LocalDateTime start = startDate != null ?
                startDate.atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ?
                endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        // Paramètres de pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        // Recherche avec filtres
        Page<Audit> auditLogs;
        if (type != null && !type.isEmpty()) {
            try {
                AuditType auditType = AuditType.valueOf(type);
                auditLogs = auditService.findByTypeAndDateRange(auditType, start, end, pageable);
            } catch (IllegalArgumentException e) {
                // Si le type n'est pas valide, ignorer le filtre
                auditLogs = auditService.findAllPaged(start, end, pageable);
            }
        } else {
            auditLogs = auditService.findAllPaged(start, end, pageable);
        }

        // Filtrer par utilisateur si spécifié
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                List<Audit> userLogs = auditService.findByUser(user);
                // Note: cette approche n'est pas optimale pour la pagination, mais suffisante pour un exemple
                auditLogs = filterAndPage(userLogs, pageable);
            }
        }

        // Filtrer par entité si spécifiée
        if (entityType != null && !entityType.isEmpty() && entityId != null) {
            List<Audit> entityLogs = auditService.findByEntity(entityType, entityId);
            // Même remarque sur la pagination
            auditLogs = filterAndPage(entityLogs, pageable);
        }

        // Préparer le modèle
        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("entityType", entityType);
        model.addAttribute("entityId", entityId);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("auditTypes", Arrays.asList(AuditType.values()));
        model.addAttribute("entityTypes", getAvailableEntityTypes());

        return "admin/audit/index";
    }

    private Page<Audit> filterAndPage(List<Audit> logs, Pageable pageable) {
        // Implémentation simplifiée de la pagination manuelle
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), logs.size());

        List<Audit> pageContent = new ArrayList<>();
        if (start <= end) {
            pageContent = logs.subList(start, end);
        }

        return new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, logs.size());
    }

    private List<String> getAvailableEntityTypes() {
        // Liste des types d'entités disponibles dans votre système
        return List.of("Client", "User", "Rappel", "System");
    }
}