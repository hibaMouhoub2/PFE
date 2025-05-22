package com.example.projetpfe.controller;

import com.example.projetpfe.entity.Direction;
import com.example.projetpfe.service.Impl.DirectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/directions")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DirectionController {

    @Autowired
    private DirectionService directionService;

    @GetMapping
    public String listDirections(Model model) {
        List<Direction> directions = directionService.findAll();
        model.addAttribute("directions", directions);
        return "admin/directions/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("direction", new Direction());
        return "admin/directions/create";
    }

    @PostMapping("/create")
    public String createDirection(@RequestParam String name,
                                  @RequestParam String code,
                                  RedirectAttributes redirectAttributes) {
        try {
            directionService.createDirection(name, code);
            redirectAttributes.addFlashAttribute("success", "Direction créée avec succès");
            return "redirect:/admin/directions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
            return "redirect:/admin/directions/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Direction direction = directionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Direction non trouvée"));
            model.addAttribute("direction", direction);
            return "admin/directions/edit";
        } catch (Exception e) {
            return "redirect:/admin/directions?error=" + e.getMessage();
        }
    }

    @PostMapping("/edit/{id}")
    public String updateDirection(@PathVariable Long id,
                                  @RequestParam String name,
                                  @RequestParam String code,
                                  RedirectAttributes redirectAttributes) {
        try {
            directionService.updateDirection(id, name, code);
            redirectAttributes.addFlashAttribute("success", "Direction mise à jour avec succès");
            return "redirect:/admin/directions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/admin/directions/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteDirection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            directionService.deleteDirection(id);
            redirectAttributes.addFlashAttribute("success", "Direction supprimée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/directions";
    }
}