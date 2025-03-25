package com.example.projetpfe.controller;

import com.example.projetpfe.entity.Rappel;
import com.example.projetpfe.service.Impl.RappelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rappels")
public class RappelController {

    private final RappelService rappelService;

    @Autowired
    public RappelController(RappelService rappelService) {
        this.rappelService = rappelService;
    }

    @GetMapping("/{id}")
    public String viewRappel(@PathVariable Long id, Model model) {
        Rappel rappel = rappelService.getById(id);
        model.addAttribute("rappel", rappel);
        return "rappels/detail";
    }

    @PostMapping("/{id}/complete")
    public String completeRappel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rappelService.completeRappel(id);
            redirectAttributes.addFlashAttribute("success", "Rappel marqué comme terminé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/agenda";
    }
}