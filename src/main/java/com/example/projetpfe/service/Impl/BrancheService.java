package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.Branche;
import com.example.projetpfe.repository.BrancheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrancheService {

    @Autowired
    private BrancheRepository brancheRepository;

    /**
     * Initialise les branches de base dans la base de données si elles n'existent pas
     * Remplace l'ancien enum Branche
     */
    @Transactional
    public void initializeBranches() {
        // Liste des branches à initialiser (basée sur votre ancien enum)
        String[][] branchesData = {
                {"SUP_FIDAA_SIDI_BELYOUT", "SUP FIDAA SIDI BELYOUT", "SUP_FIDAA_SIDI_BELYOUT"},
                {"SUP_BERNOUSSI_ZENATA", "SUP BERNOUSSI ZENATA", "SUP_BERNOUSSI_ZENATA"},
                {"SUP_BENMSIK_SIDI_OTHMANE", "SUP BENMSIK SIDI OTHMANE", "SUP_BENMSIK_SIDI_OTHMANE"},

        };

        for (String[] brancheData : branchesData) {
            if (brancheRepository.findByCode(brancheData[0]).isEmpty()) {
                Branche branche = new Branche();
                branche.setCode(brancheData[0]);
                branche.setDisplayname(brancheData[1]);
                branche.setRegionCode(brancheData[2]);
                brancheRepository.save(branche);
                System.out.println("Branche initialisée: " + brancheData[1]);
            }
        }
    }


    public Branche findByCode(String code) {
        return brancheRepository.findByCode(code).orElse(null);
    }


}