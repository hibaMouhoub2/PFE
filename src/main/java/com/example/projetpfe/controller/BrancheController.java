package com.example.projetpfe.controller;

import com.example.projetpfe.entity.Branche;
import com.example.projetpfe.entity.Region;
import com.example.projetpfe.service.Impl.BrancheService;
import com.example.projetpfe.service.Impl.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/branches")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BrancheController {

    @Autowired
    private BrancheService brancheService;

    @Autowired
    private RegionService regionService;

    @GetMapping
    public String listBranches(Model model) {
        List<Branche> branches = brancheService.findAll();
        model.addAttribute("branches", branches);
        return "admin/branches/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("branche", new Branche());

        // Ajouter la liste des régions disponibles
        List<Region> regions = regionService.findAll();
        model.addAttribute("regions", regions);

        return "admin/branches/create";
    }

    @PostMapping("/create")
    public String createBranche(@RequestParam String code,
                                @RequestParam String displayname,
                                @RequestParam Long regionId,
                                RedirectAttributes redirectAttributes) {
        try {
            // Récupérer la région sélectionnée
            Region region = regionService.findById(regionId)
                    .orElseThrow(() -> new RuntimeException("Région non trouvée"));

            Branche branche = brancheService.createBrancheWithRegion(code, displayname, region);
            redirectAttributes.addFlashAttribute("success", "Branche créée avec succès");
            return "redirect:/admin/branches";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
            return "redirect:/admin/branches/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Branche branche = brancheService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Branche non trouvée"));
            model.addAttribute("branche", branche);

            // Ajouter la liste des régions pour modification
            List<Region> regions = regionService.findAll();
            model.addAttribute("regions", regions);

            return "admin/branches/edit";
        } catch (Exception e) {
            return "redirect:/admin/branches?error=" + e.getMessage();
        }
    }

    @PostMapping("/edit/{id}")
    public String updateBranche(@PathVariable Long id,
                                @RequestParam String code,
                                @RequestParam String displayname,
                                @RequestParam Long regionId,
                                RedirectAttributes redirectAttributes) {
        try {
            Region region = regionService.findById(regionId)
                    .orElseThrow(() -> new RuntimeException("Région non trouvée"));

            brancheService.updateBrancheWithRegion(id, code, displayname, region);
            redirectAttributes.addFlashAttribute("success", "Branche mise à jour avec succès");
            return "redirect:/admin/branches";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/admin/branches/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteBranche(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            brancheService.deleteBranche(id);
            redirectAttributes.addFlashAttribute("success", "Branche supprimée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/branches";
    }
}