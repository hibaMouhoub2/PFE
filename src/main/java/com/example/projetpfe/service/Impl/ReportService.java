package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.*;
import com.example.projetpfe.repository.AuditRepository;
import com.example.projetpfe.repository.ClientRepository;
import com.example.projetpfe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final ClientRepository clientRepository;
    @Autowired
    private AuditRepository auditRepository;
    @Autowired
    private UserRepository userRepository;

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

    /**
     * Obtient les statistiques de performance des agents basées sur les logs d'audit
     */
    public Map<String, Map<String, Long>> getAgentPerformanceStats(LocalDateTime start, LocalDateTime end) {
        // Initialiser le résultat
        Map<String, Map<String, Long>> result = new HashMap<>();

        // Récupérer tous les utilisateurs avec rôle USER
        List<User> agents = userRepository.findByRolesName("ROLE_USER");

        for (User agent : agents) {
            Map<String, Long> agentStats = new HashMap<>();

            // Récupérer les actions d'audit pour cet agent
            List<Audit> agentAudits = auditRepository.findByUserAndTimestampBetween(agent, start, end);

            // Compter les différents types d'actions
            long clientsContacted = agentAudits.stream()
                    .filter(a -> a.getType() == AuditType.CLIENT_STATUS_CHANGE &&
                            a.getDetails().contains("CONTACTE"))
                    .count();

            long questionnairesFilled = agentAudits.stream()
                    .filter(a -> a.getType() == AuditType.CLIENT_QUESTIONNAIRE_COMPLETED)
                    .count();

            long questionnairesUpdated = agentAudits.stream()
                    .filter(a -> a.getType() == AuditType.CLIENT_QUESTIONNAIRE_UPDATED)
                    .count();

            long rappelsCreated = agentAudits.stream()
                    .filter(a -> a.getType() == AuditType.RAPPEL_CREATED)
                    .count();

            long rappelsCompleted = agentAudits.stream()
                    .filter(a -> a.getType() == AuditType.RAPPEL_COMPLETED)
                    .count();

            // Ajouter les statistiques à la map
            agentStats.put("clientsContacted", clientsContacted);
            agentStats.put("questionnairesFilled", questionnairesFilled);
            agentStats.put("questionnairesUpdated", questionnairesUpdated);
            agentStats.put("rappelsCreated", rappelsCreated);
            agentStats.put("rappelsCompleted", rappelsCompleted);
            agentStats.put("totalActions", (long) agentAudits.size());

            // Ajouter les statistiques de l'agent au résultat
            result.put(agent.getName(), agentStats);
        }

        return result;
    }

    /**
     * Obtient les actions quotidiennes des agents
     */
    public Map<String, Object> getDailyAgentActivityStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> result = new HashMap<>();

        // Récupérer tous les agents
        List<User> agents = userRepository.findByRolesName("ROLE_USER");

        // Pour chaque jour dans la période
        List<LocalDate> days = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        while (!currentDate.isAfter(end.toLocalDate())) {
            days.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        // Liste des dates pour l'axe X
        List<String> dateLabels = days.stream()
                .map(d -> d.format(DateTimeFormatter.ofPattern("dd/MM")))
                .collect(Collectors.toList());

        // Changement ici - retourner les dates comme des chaînes
        result.put("dates", dateLabels);

        // Pour chaque agent, calculer le nombre d'actions par jour
        for (User agent : agents) {
            List<Long> dailyCounts = new ArrayList<>();

            for (LocalDate day : days) {
                LocalDateTime dayStart = day.atStartOfDay();
                LocalDateTime dayEnd = day.atTime(LocalTime.MAX);

                long count = auditRepository.countByUserAndTimestampBetween(agent, dayStart, dayEnd);
                dailyCounts.add(count);
            }

            result.put(agent.getName(), dailyCounts);
        }

        return result;
    }

    /**
     * Obtient les statistiques des raisons de non-renouvellement filtrées par région
     */
    public Map<String, Long> getRaisonsNonRenouvellementStatsByRegion(Region region) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (RaisonNonRenouvellement raison : RaisonNonRenouvellement.values()) {
            // Compter les clients qui correspondent à cette raison et cette région
            long count = clientRepository.findAll().stream()
                    .filter(client -> raison.equals(client.getRaisonNonRenouvellement())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode))
                    .count();

            stats.put(raison.getDisplayName(), count);
        }

        return stats;
    }

    /**
     * Obtient les statistiques de qualité de service filtrées par région
     */
    public Map<String, Long> getQualiteServiceStatsByRegion(Region region) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (QualiteService qualite : QualiteService.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> qualite.equals(client.getQualiteService())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode))
                    .count();

            stats.put(qualite.getDisplayName(), count);
        }

        return stats;
    }

    /**
     * Obtient les statistiques d'intérêt pour le crédit filtrées par région
     */
    public Map<String, Long> getInteretCreditStatsByRegion(Region region) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (InteretCredit interet : InteretCredit.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> interet.equals(client.getInteretNouveauCredit())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode))
                    .count();

            stats.put(interet.getDisplayName(), count);
        }

        return stats;
    }

    /**
     * Obtient les statistiques des facteurs d'influence filtrées par région
     */
    public Map<String, Long> getFacteurInfluenceStatsByRegion(Region region) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (FacteurInfluence facteur : FacteurInfluence.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> facteur.equals(client.getFacteurInfluence())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode))
                    .count();

            stats.put(facteur.getDisplayName(), count);
        }

        return stats;
    }

    /**
     * Obtient les statistiques de profil client filtrées par région
     */
    public Map<String, Long> getProfilStatsByRegion(Region region) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (Profil profil : Profil.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> profil.equals(client.getProfil())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode))
                    .count();

            stats.put(profil.getDisplayName(), count);
        }

        return stats;
    }

    /**
     * Obtient les statistiques d'activité client filtrées par région
     */
    public Map<String, Long> getActiviteClientStatsByRegion(Region region) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (ActiviteClient activite : ActiviteClient.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> activite.equals(client.getActiviteClient())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode))
                    .count();

            stats.put(activite.getDisplayName(), count);
        }

        return stats;
    }

    /**
     * Obtient les statistiques de rendez-vous filtrées par région
     */
    public Map<String, Long> getRendezVousStatsByRegion(Region region) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        // Pour les clients avec rendez-vous
        long countOui = clientRepository.findAll().stream()
                .filter(client -> Boolean.TRUE.equals(client.getRendezVousAgence())
                        && client.getNMREG() != null
                        && matchesRegion(client.getNMREG(), regionCode))
                .count();

        // Pour les clients sans rendez-vous
        long countNon = clientRepository.findAll().stream()
                .filter(client -> Boolean.FALSE.equals(client.getRendezVousAgence())
                        && client.getNMREG() != null
                        && matchesRegion(client.getNMREG(), regionCode))
                .count();

        stats.put("Oui", countOui);
        stats.put("Non", countNon);

        return stats;
    }

    /**
     * Obtient les statistiques par branche filtrées par région
     */
    public Map<String, Long> getBrancheStatsByRegion(Region region) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (Branche branche : Branche.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> branche.equals(client.getNMBRA())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode))
                    .count();

            stats.put(branche.getDisplayName(), count);
        }

        return stats;
    }

    /**
     * Obtient les statistiques de progression par mois filtrées par région
     */
    public Map<String, Long> getStatusProgressionByMonthAndRegion(Region region) {
        Map<String, Long> stats = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        String regionCode = region.getCode();

        // Pour les 6 derniers mois
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            String monthYear = start.format(DateTimeFormatter.ofPattern("MM/yyyy"));

            long contactedCount = clientRepository.findAll().stream()
                    .filter(client -> client.getStatus() == ClientStatus.CONTACTE
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end)
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode))
                    .count();

            stats.put(monthYear, contactedCount);
        }

        return stats;
    }

    /**
     * Versions avec dates pour chaque méthode
     */

    public Map<String, Long> getRaisonsNonRenouvellementStatsByRegionAndDate(Region region, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (RaisonNonRenouvellement raison : RaisonNonRenouvellement.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> raison.equals(client.getRaisonNonRenouvellement())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();

            stats.put(raison.getDisplayName(), count);
        }

        return stats;
    }

    public Map<String, Long> getQualiteServiceStatsByRegionAndDate(Region region, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (QualiteService qualite : QualiteService.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> qualite.equals(client.getQualiteService())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();

            stats.put(qualite.getDisplayName(), count);
        }

        return stats;
    }

    public Map<String, Long> getInteretCreditStatsByRegionAndDate(Region region, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (InteretCredit interet : InteretCredit.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> interet.equals(client.getInteretNouveauCredit())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();

            stats.put(interet.getDisplayName(), count);
        }

        return stats;
    }

    public Map<String, Long> getFacteurInfluenceStatsByRegionAndDate(Region region, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (FacteurInfluence facteur : FacteurInfluence.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> facteur.equals(client.getFacteurInfluence())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();

            stats.put(facteur.getDisplayName(), count);
        }

        return stats;
    }

    public Map<String, Long> getProfilStatsByRegionAndDate(Region region, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (Profil profil : Profil.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> profil.equals(client.getProfil())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();

            stats.put(profil.getDisplayName(), count);
        }

        return stats;
    }

    public Map<String, Long> getActiviteClientStatsByRegionAndDate(Region region, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (ActiviteClient activite : ActiviteClient.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> activite.equals(client.getActiviteClient())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();

            stats.put(activite.getDisplayName(), count);
        }

        return stats;
    }

    public Map<String, Long> getRendezVousStatsByRegionAndDate(Region region, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        // Pour les clients avec rendez-vous
        long countOui = clientRepository.findAll().stream()
                .filter(client -> Boolean.TRUE.equals(client.getRendezVousAgence())
                        && client.getNMREG() != null
                        && matchesRegion(client.getNMREG(), regionCode)
                        && client.getUpdatedAt() != null
                        && client.getUpdatedAt().isAfter(start)
                        && client.getUpdatedAt().isBefore(end))
                .count();

        // Pour les clients sans rendez-vous
        long countNon = clientRepository.findAll().stream()
                .filter(client -> Boolean.FALSE.equals(client.getRendezVousAgence())
                        && client.getNMREG() != null
                        && matchesRegion(client.getNMREG(), regionCode)
                        && client.getUpdatedAt() != null
                        && client.getUpdatedAt().isAfter(start)
                        && client.getUpdatedAt().isBefore(end))
                .count();

        stats.put("Oui", countOui);
        stats.put("Non", countNon);

        return stats;
    }

    public Map<String, Long> getBrancheStatsByRegionAndDate(Region region, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (Branche branche : Branche.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> branche.equals(client.getNMBRA())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();

            stats.put(branche.getDisplayName(), count);
        }

        return stats;
    }

    public Map<String, Long> getStatusProgressionByMonthAndRegionAndDate(Region region, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

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
            final LocalDateTime finalMonthStart = monthStart;
            final LocalDateTime finalMonthEnd = monthEnd;

            long contactedCount = clientRepository.findAll().stream()
                    .filter(client -> client.getStatus() == ClientStatus.CONTACTE
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(finalMonthStart)
                            && client.getUpdatedAt().isBefore(finalMonthEnd)
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode))
                    .count();

            stats.put(monthYear, contactedCount);

            // Passer au mois précédent
            currentEnd = monthStart.minusDays(1);
        }

        return stats;
    }

    /**
     * Méthode utilitaire pour vérifier si une région client correspond à un code de région
     * avec une correspondance plus souple
     */
    private boolean matchesRegion(String clientRegion, String regionCode) {
        if (clientRegion == null || regionCode == null) {
            return false;
        }

        // Normaliser les deux côtés pour une comparaison plus souple
        String normalizedClientRegion = clientRegion.toUpperCase().replace(" ", "");
        String normalizedRegionCode = regionCode.toUpperCase().replace("_", "");

        // Vérifier si l'une contient l'autre (comparaison partielle)
        return normalizedClientRegion.contains(normalizedRegionCode) ||
                normalizedRegionCode.contains(normalizedClientRegion) ||
                // Traiter les variantes orthographiques potentielles
                (normalizedClientRegion.contains("SIDI") && normalizedRegionCode.contains("SEDY")) ||
                (normalizedClientRegion.contains("SEDY") && normalizedRegionCode.contains("SIDI")) ||
                (normalizedClientRegion.contains("FIDAA") && normalizedRegionCode.contains("FIDA")) ||
                (normalizedClientRegion.contains("FIDA") && normalizedRegionCode.contains("FIDAA"));
    }
    /**
     * Obtient les statistiques filtrées par branche
     */
    public Map<String, Long> getRaisonsNonRenouvellementStatsByBranche(Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (RaisonNonRenouvellement raison : RaisonNonRenouvellement.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> raison.equals(client.getRaisonNonRenouvellement())
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(raison.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getQualiteServiceStatsByBranche(Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (QualiteService qualite : QualiteService.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> qualite.equals(client.getQualiteService())
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(qualite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getInteretCreditStatsByBranche(Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (InteretCredit interet : InteretCredit.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> interet.equals(client.getInteretNouveauCredit())
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(interet.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getFacteurInfluenceStatsByBranche(Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (FacteurInfluence facteur : FacteurInfluence.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> facteur.equals(client.getFacteurInfluence())
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(facteur.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getProfilStatsByBranche(Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Profil profil : Profil.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> profil.equals(client.getProfil())
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(profil.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getActiviteClientStatsByBranche(Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (ActiviteClient activite : ActiviteClient.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> activite.equals(client.getActiviteClient())
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(activite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getRendezVousStatsByBranche(Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        long countOui = clientRepository.findAll().stream()
                .filter(client -> Boolean.TRUE.equals(client.getRendezVousAgence())
                        && branche.equals(client.getNMBRA()))
                .count();
        long countNon = clientRepository.findAll().stream()
                .filter(client -> Boolean.FALSE.equals(client.getRendezVousAgence())
                        && branche.equals(client.getNMBRA()))
                .count();
        stats.put("Oui", countOui);
        stats.put("Non", countNon);
        return stats;
    }

    public Map<String, Long> getStatusProgressionByMonthAndBranche(Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // Pour les 6 derniers mois
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            String monthYear = start.format(DateTimeFormatter.ofPattern("MM/yyyy"));

            long contactedCount = clientRepository.findAll().stream()
                    .filter(client -> client.getStatus() == ClientStatus.CONTACTE
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end)
                            && branche.equals(client.getNMBRA()))
                    .count();

            stats.put(monthYear, contactedCount);
        }

        return stats;
    }
    /**
     * Obtient les statistiques filtrées par région et branche
     */
    public Map<String, Long> getRaisonsNonRenouvellementStatsByRegionAndBranche(Region region, Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (RaisonNonRenouvellement raison : RaisonNonRenouvellement.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> raison.equals(client.getRaisonNonRenouvellement())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(raison.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getQualiteServiceStatsByRegionAndBranche(Region region, Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (QualiteService qualite : QualiteService.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> qualite.equals(client.getQualiteService())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(qualite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getInteretCreditStatsByRegionAndBranche(Region region, Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (InteretCredit interet : InteretCredit.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> interet.equals(client.getInteretNouveauCredit())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(interet.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getFacteurInfluenceStatsByRegionAndBranche(Region region, Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (FacteurInfluence facteur : FacteurInfluence.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> facteur.equals(client.getFacteurInfluence())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(facteur.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getProfilStatsByRegionAndBranche(Region region, Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (Profil profil : Profil.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> profil.equals(client.getProfil())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(profil.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getActiviteClientStatsByRegionAndBranche(Region region, Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (ActiviteClient activite : ActiviteClient.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> activite.equals(client.getActiviteClient())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA()))
                    .count();
            stats.put(activite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getRendezVousStatsByRegionAndBranche(Region region, Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        // Pour les clients avec rendez-vous
        long countOui = clientRepository.findAll().stream()
                .filter(client -> Boolean.TRUE.equals(client.getRendezVousAgence())
                        && client.getNMREG() != null
                        && matchesRegion(client.getNMREG(), regionCode)
                        && branche.equals(client.getNMBRA()))
                .count();

        // Pour les clients sans rendez-vous
        long countNon = clientRepository.findAll().stream()
                .filter(client -> Boolean.FALSE.equals(client.getRendezVousAgence())
                        && client.getNMREG() != null
                        && matchesRegion(client.getNMREG(), regionCode)
                        && branche.equals(client.getNMBRA()))
                .count();

        stats.put("Oui", countOui);
        stats.put("Non", countNon);
        return stats;
    }
    public Map<String, Long> getRaisonsNonRenouvellementStatsByRegionAndBrancheAndDate(Region region, Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (RaisonNonRenouvellement raison : RaisonNonRenouvellement.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> raison.equals(client.getRaisonNonRenouvellement())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(raison.getDisplayName(), count);
        }
        return stats;
    }
    public Map<String, Long> getActiviteClientStatsByRegionAndBrancheAndDate(Region region, Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (ActiviteClient activite : ActiviteClient.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> activite.equals(client.getActiviteClient())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(activite.getDisplayName(), count);
        }
        return stats;
    }
    public Map<String, Long> getRendezVousStatsByRegionAndBrancheAndDate(Region region, Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        // Pour les clients avec rendez-vous
        long countOui = clientRepository.findAll().stream()
                .filter(client -> Boolean.TRUE.equals(client.getRendezVousAgence())
                        && client.getNMREG() != null
                        && matchesRegion(client.getNMREG(), regionCode)
                        && branche.equals(client.getNMBRA())
                        && client.getUpdatedAt() != null
                        && client.getUpdatedAt().isAfter(start)
                        && client.getUpdatedAt().isBefore(end))
                .count();

        // Pour les clients sans rendez-vous
        long countNon = clientRepository.findAll().stream()
                .filter(client -> Boolean.FALSE.equals(client.getRendezVousAgence())
                        && client.getNMREG() != null
                        && matchesRegion(client.getNMREG(), regionCode)
                        && branche.equals(client.getNMBRA())
                        && client.getUpdatedAt() != null
                        && client.getUpdatedAt().isAfter(start)
                        && client.getUpdatedAt().isBefore(end))
                .count();

        stats.put("Oui", countOui);
        stats.put("Non", countNon);
        return stats;
    }
    public Map<String, Long> getStatusProgressionByMonthAndRegionAndBrancheAndDate(Region region, Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

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
            final LocalDateTime finalMonthStart = monthStart;
            final LocalDateTime finalMonthEnd = monthEnd;

            long contactedCount = clientRepository.findAll().stream()
                    .filter(client -> client.getStatus() == ClientStatus.CONTACTE
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(finalMonthStart)
                            && client.getUpdatedAt().isBefore(finalMonthEnd)
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA()))
                    .count();

            stats.put(monthYear, contactedCount);

            // Passer au mois précédent
            currentEnd = monthStart.minusDays(1);
        }

        return stats;
    }
    public Map<String, Long> getInteretCreditStatsByRegionAndBrancheAndDate(Region region, Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (InteretCredit interet : InteretCredit.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> interet.equals(client.getInteretNouveauCredit())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(interet.getDisplayName(), count);
        }
        return stats;
    }
    public Map<String, Long> getBrancheStatsByDate(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();

        for (Branche branche : Branche.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();

            stats.put(branche.getDisplayName(), count);
        }

        return stats;
    }

    public Map<String, Long> getProfilStatsByRegionAndBrancheAndDate(Region region, Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (Profil profil : Profil.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> profil.equals(client.getProfil())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(profil.getDisplayName(), count);
        }
        return stats;
    }
    public Map<String, Long> getFacteurInfluenceStatsByRegionAndBrancheAndDate(Region region, Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (FacteurInfluence facteur : FacteurInfluence.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> facteur.equals(client.getFacteurInfluence())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(facteur.getDisplayName(), count);
        }
        return stats;
    }
    public Map<String, Long> getQualiteServiceStatsByRegionAndBrancheAndDate(Region region, Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();

        for (QualiteService qualite : QualiteService.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> qualite.equals(client.getQualiteService())
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(qualite.getDisplayName(), count);
        }
        return stats;
    }


    public Map<String, Long> getStatusProgressionByMonthAndRegionAndBranche(Region region, Branche branche) {
        Map<String, Long> stats = new LinkedHashMap<>();
        String regionCode = region.getCode();
        LocalDateTime now = LocalDateTime.now();

        // Pour les 6 derniers mois
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            String monthYear = start.format(DateTimeFormatter.ofPattern("MM/yyyy"));

            long contactedCount = clientRepository.findAll().stream()
                    .filter(client -> client.getStatus() == ClientStatus.CONTACTE
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end)
                            && client.getNMREG() != null
                            && matchesRegion(client.getNMREG(), regionCode)
                            && branche.equals(client.getNMBRA()))
                    .count();

            stats.put(monthYear, contactedCount);
        }

        return stats;
    }
    /**
     * Obtient les statistiques filtrées par branche et date
     */
    public Map<String, Long> getRaisonsNonRenouvellementStatsByBrancheAndDate(Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (RaisonNonRenouvellement raison : RaisonNonRenouvellement.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> raison.equals(client.getRaisonNonRenouvellement())
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(raison.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getQualiteServiceStatsByBrancheAndDate(Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (QualiteService qualite : QualiteService.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> qualite.equals(client.getQualiteService())
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(qualite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getInteretCreditStatsByBrancheAndDate(Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (InteretCredit interet : InteretCredit.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> interet.equals(client.getInteretNouveauCredit())
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(interet.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getFacteurInfluenceStatsByBrancheAndDate(Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (FacteurInfluence facteur : FacteurInfluence.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> facteur.equals(client.getFacteurInfluence())
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(facteur.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getProfilStatsByBrancheAndDate(Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Profil profil : Profil.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> profil.equals(client.getProfil())
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(profil.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getActiviteClientStatsByBrancheAndDate(Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (ActiviteClient activite : ActiviteClient.values()) {
            long count = clientRepository.findAll().stream()
                    .filter(client -> activite.equals(client.getActiviteClient())
                            && branche.equals(client.getNMBRA())
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(start)
                            && client.getUpdatedAt().isBefore(end))
                    .count();
            stats.put(activite.getDisplayName(), count);
        }
        return stats;
    }

    public Map<String, Long> getRendezVousStatsByBrancheAndDate(Branche branche, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> stats = new LinkedHashMap<>();

        // Pour les clients avec rendez-vous
        long countOui = clientRepository.findAll().stream()
                .filter(client -> Boolean.TRUE.equals(client.getRendezVousAgence())
                        && branche.equals(client.getNMBRA())
                        && client.getUpdatedAt() != null
                        && client.getUpdatedAt().isAfter(start)
                        && client.getUpdatedAt().isBefore(end))
                .count();

        // Pour les clients sans rendez-vous
        long countNon = clientRepository.findAll().stream()
                .filter(client -> Boolean.FALSE.equals(client.getRendezVousAgence())
                        && branche.equals(client.getNMBRA())
                        && client.getUpdatedAt() != null
                        && client.getUpdatedAt().isAfter(start)
                        && client.getUpdatedAt().isBefore(end))
                .count();

        stats.put("Oui", countOui);
        stats.put("Non", countNon);
        return stats;
    }

    public Map<String, Long> getStatusProgressionByMonthAndBrancheAndDate(Branche branche, LocalDateTime start, LocalDateTime end) {
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
            final LocalDateTime finalMonthStart = monthStart;
            final LocalDateTime finalMonthEnd = monthEnd;

            long contactedCount = clientRepository.findAll().stream()
                    .filter(client -> client.getStatus() == ClientStatus.CONTACTE
                            && client.getUpdatedAt() != null
                            && client.getUpdatedAt().isAfter(finalMonthStart)
                            && client.getUpdatedAt().isBefore(finalMonthEnd)
                            && branche.equals(client.getNMBRA()))
                    .count();

            stats.put(monthYear, contactedCount);

            // Passer au mois précédent
            currentEnd = monthStart.minusDays(1);
        }

        return stats;
    }


}
