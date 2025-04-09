package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReportService {
    private final ClientRepository clientRepository;

    @Autowired
    public ReportService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Map<String, Long> getRaisonsNonRenouvellementStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (RaisonNonRenouvellement raison : RaisonNonRenouvellement.values()) {
            long count = clientRepository.countByRaisonNonRenouvellement(raison);
            stats.put(raison.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getQualiteServiceStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (QualiteService qualite : QualiteService.values()) {
            long count = clientRepository.countByQualiteService(qualite);
            stats.put(qualite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getInteretCreditStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (InteretCredit interet : InteretCredit.values()) {
            long count = clientRepository.countByInteretNouveauCredit(interet);
            stats.put(interet.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getFacteurInfluenceStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (FacteurInfluence facteur : FacteurInfluence.values()) {
            long count = clientRepository.countByFacteurInfluence(facteur);
            stats.put(facteur.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getProfilStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Profil profil : Profil.values()) {
            long count = clientRepository.countByProfil(profil);
            stats.put(profil.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getActiviteClientStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (ActiviteClient activite : ActiviteClient.values()) {
            long count = clientRepository.countByActiviteClient(activite);
            stats.put(activite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getRendezVousStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("Oui", clientRepository.countByRendezVousAgence(true));
        stats.put("Non", clientRepository.countByRendezVousAgence(false));
        return stats;
    }

    public Map<String, Long> getBrancheStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Branche branche : Branche.values()) {
            long count = clientRepository.countByNMBRA(branche);
            stats.put(branche.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getStatusProgressionByMonth() {
        Map<String, Long> stats = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // Obtenir les données pour les 6 derniers mois
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);

            String monthYear = start.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            long contactedCount = clientRepository.countByStatusAndUpdatedAtBetween(ClientStatus.CONTACTE, start, end);

            stats.put(monthYear, contactedCount);
        }

        return stats;
    }



    public Map<String, Long> getRaisonsNonRenouvellementStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (RaisonNonRenouvellement raison : RaisonNonRenouvellement.values()) {
            long count = clientRepository.countByRaisonNonRenouvellementAndUpdatedAtBetween(raison, start, end);
            stats.put(raison.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getQualiteServiceStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (QualiteService qualite : QualiteService.values()) {
            long count = clientRepository.countByQualiteServiceAndUpdatedAtBetween(qualite, start, end);
            stats.put(qualite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getInteretCreditStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (InteretCredit interet : InteretCredit.values()) {
            long count = clientRepository.countByInteretNouveauCreditAndUpdatedAtBetween(interet, start, end);
            stats.put(interet.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getFacteurInfluenceStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (FacteurInfluence facteur : FacteurInfluence.values()) {
            long count = clientRepository.countByFacteurInfluenceAndUpdatedAtBetween(facteur, start, end);
            stats.put(facteur.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getProfilStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Profil profil : Profil.values()) {
            long count = clientRepository.countByProfilAndUpdatedAtBetween(profil, start, end);
            stats.put(profil.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getActiviteClientStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (ActiviteClient activite : ActiviteClient.values()) {
            long count = clientRepository.countByActiviteClientAndUpdatedAtBetween(activite, start, end);
            stats.put(activite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getRendezVousStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("Oui", clientRepository.countByRendezVousAgenceAndUpdatedAtBetween(true, start, end));
        stats.put("Non", clientRepository.countByRendezVousAgenceAndUpdatedAtBetween(false, start, end));
        return stats;
    }

    public Map<String, Long> getBrancheStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Branche branche : Branche.values()) {
            long count = clientRepository.countByNMBRAAndUpdatedAtBetween(branche, start, end);
            stats.put(branche.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getStatusProgressionByMonth(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();

        // Calculer le nombre de mois entre start et end
        long monthsBetween = ChronoUnit.MONTHS.between(
                start.toLocalDate().withDayOfMonth(1),
                end.toLocalDate().withDayOfMonth(1)
        ) + 1;

        // Limiter à 6 mois maximum pour l'affichage
        int monthsToShow = (int) Math.min(monthsBetween, 6);

        LocalDateTime currentEnd = end;
        for (int i = 0; i < monthsToShow; i++) {
            LocalDateTime monthStart = currentEnd.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

            // Si le mois dépasse la date de début, ajuster
            if (monthStart.isBefore(start)) {
                monthStart = start;
            }

            String monthYear = monthStart.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            long contactedCount = clientRepository.countByStatusAndUpdatedAtBetween(
                    ClientStatus.CONTACTE, monthStart, monthEnd);

            stats.put(monthYear, contactedCount);

            // Passer au mois précédent
            currentEnd = monthStart.minusDays(1);
        }

        return stats;
    }
}
