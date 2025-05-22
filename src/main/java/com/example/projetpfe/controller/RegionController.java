package com.example.projetpfe.controller;

import com.example.projetpfe.entity.Direction;
import com.example.projetpfe.entity.Region;
import com.example.projetpfe.service.Impl.DirectionService;
import com.example.projetpfe.service.Impl.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/regions")
public class RegionController {

    @Autowired
    private RegionService regionService;

    @Autowired
    private DirectionService directionService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String listRegions(Model model) {
        List<Region> regions = regionService.findAll();
        model.addAttribute("regions", regions);
        return "admin/regions/index";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String showCreateForm(Model model) {
        model.addAttribute("region", new Region());

        // Ajouter la liste des directions disponibles
        List<Direction> directions = directionService.findAll();
        model.addAttribute("directions", directions);

        return "admin/regions/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String createRegion(@RequestParam String name,
                               @RequestParam String code,
                               @RequestParam(required = false) Long directionId,
                               RedirectAttributes redirectAttributes) {
        try {
            // Si une direction est sélectionnée, l'associer à la région
            Direction direction = null;
            if (directionId != null) {
                direction = directionService.findById(directionId)
                        .orElseThrow(() -> new RuntimeException("Direction non trouvée"));
            }

            Region region = regionService.createRegionWithDirection(name, code, direction);
            redirectAttributes.addFlashAttribute("success", "Région créée avec succès");
            return "redirect:/admin/regions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
            return "redirect:/admin/regions/create";
        }
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Region region = regionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Région non trouvée"));
            model.addAttribute("region", region);

            // Ajouter la liste des directions pour modification
            List<Direction> directions = directionService.findAll();
            model.addAttribute("directions", directions);

            return "admin/regions/edit";
        } catch (Exception e) {
            return "redirect:/admin/regions?error=" + e.getMessage();
        }
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String updateRegion(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam String code,
                               @RequestParam(required = false) Long directionId,
                               RedirectAttributes redirectAttributes) {
        try {
            Direction direction = null;
            if (directionId != null) {
                direction = directionService.findById(directionId)
                        .orElseThrow(() -> new RuntimeException("Direction non trouvée"));
            }

            regionService.updateRegionWithDirection(id, name, code, direction);
            redirectAttributes.addFlashAttribute("success", "Région mise à jour avec succès");
            return "redirect:/admin/regions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/admin/regions/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String deleteRegion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            regionService.deleteRegion(id);
            redirectAttributes.addFlashAttribute("success", "Région supprimée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/regions";
    }
}