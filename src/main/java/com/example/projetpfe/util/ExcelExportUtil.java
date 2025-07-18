package com.example.projetpfe.util;

import com.example.projetpfe.entity.Client;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Component
public class ExcelExportUtil {

    public byte[] exportClientsToExcel(List<Client> clients) throws IOException {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Clients");


            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);


            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "ID", "Nom", "Prénom", "CIN", "Téléphone", "Téléphone 2",
                    "Direction", "Région", "Branche",
                    "Date Fin Crédit", "Date Début Crédit",
                    "Activité Actuelle", "Barème", "Montant Début",
                    "Nombre Incidents", "Age Client", "Nombre Prêts",
                    "Statut", "Activité Client", "Profil",
                    "Raison Non Renouvellement", "Qualité Service",
                    "Difficultés Rencontrées", "Précision Difficultés",
                    "Intérêt Nouveau Crédit", "Rendez-vous Agence",
                    "Date Rendez-vous", "Facteur Influence",
                    "Autres Facteurs", "Autres Raisons",
                     "Date Modification",
                    "Utilisateur Assigné"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4500);
            }

            // Remplir les données
            int rowNum = 1;
            for (Client client : clients) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;

                // Informations de base
                setCellValue(row.createCell(colNum++), client.getId());
                setCellValue(row.createCell(colNum++), client.getNom());
                setCellValue(row.createCell(colNum++), client.getPrenom());
                setCellValue(row.createCell(colNum++), client.getCin());
                setCellValue(row.createCell(colNum++), client.getTelephone());
                setCellValue(row.createCell(colNum++), client.getTelephone2());

                // Informations d'agence
                setCellValue(row.createCell(colNum++), client.getNMDIR());
                setCellValue(row.createCell(colNum++), client.getNMREG());
                setCellValue(row.createCell(colNum++), client.getNMBRA() != null ? client.getNMBRA().getDisplayname() : null);

                // Informations de crédit
                setCellValue(row.createCell(colNum++), client.getDTFINC());
                setCellValue(row.createCell(colNum++), client.getDTDEBC());
                setCellValue(row.createCell(colNum++), client.getActiviteActuelle());
                setCellValue(row.createCell(colNum++), client.getBAREM());
                setCellValue(row.createCell(colNum++), client.getMNTDEB());
                setCellValue(row.createCell(colNum++), client.getNBINC());
                setCellValue(row.createCell(colNum++), client.getAgeClient());
                setCellValue(row.createCell(colNum++), client.getNBPRETS());

                // Informations de statut et activité
                setCellValue(row.createCell(colNum++), client.getStatus() != null ? client.getStatus().getDisplayName() : null);
                setCellValue(row.createCell(colNum++), client.getActiviteClient() != null ? client.getActiviteClient().getDisplayName() : null);
                setCellValue(row.createCell(colNum++), client.getProfil() != null ? client.getProfil().getDisplayName() : null);

                // Informations du questionnaire
                setCellValue(row.createCell(colNum++), client.getRaisonNonRenouvellement() != null ? client.getRaisonNonRenouvellement().getDisplayName() : null);
                setCellValue(row.createCell(colNum++), client.getQualiteService() != null ? client.getQualiteService().getDisplayName() : null);
                setCellValue(row.createCell(colNum++), client.getADifficultesRencontrees() != null ? (client.getADifficultesRencontrees() ? "Oui" : "Non") : null);
                setCellValue(row.createCell(colNum++), client.getPrecisionDifficultes());
                setCellValue(row.createCell(colNum++), client.getInteretNouveauCredit() != null ? client.getInteretNouveauCredit().getDisplayName() : null);
                setCellValue(row.createCell(colNum++), client.getRendezVousAgence() != null ? (client.getRendezVousAgence() ? "Oui" : "Non") : null);
                setCellValue(row.createCell(colNum++), client.getDateHeureRendezVous() != null ? client.getDateHeureRendezVous().format(dateTimeFormatter) : null);
                setCellValue(row.createCell(colNum++), client.getFacteurInfluence() != null ? client.getFacteurInfluence().getDisplayName() : null);
                setCellValue(row.createCell(colNum++), client.getAutresFacteurs());
                setCellValue(row.createCell(colNum++), client.getAutresRaisons());


//                setCellValue(row.createCell(colNum++), client.getCreatedAt() != null ? client.getCreatedAt().format(dateTimeFormatter) : null);
                setCellValue(row.createCell(colNum++), client.getUpdatedAt() != null ? client.getUpdatedAt().format(dateTimeFormatter) : null);
                setCellValue(row.createCell(colNum++), client.getAssignedUser() != null ? client.getAssignedUser().getName() : "Non assigné");
//                setCellValue(row.createCell(colNum++), client.getCreatedBy() != null ? client.getCreatedBy().getName() : null);
//                setCellValue(row.createCell(colNum++), client.getUpdatedBy() != null ? client.getUpdatedBy().getName() : null);
            }


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    public byte[] exportClientsWithPhoneChangesToExcel(List<Client> clients) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Clients avec téléphone modifié");


            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);


            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "ID", "Nom", "Prénom", "CIN", "Téléphone Actuel", "Direction", "Région", "Branche"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4500);
            }

            // Remplir les données
            int rowNum = 1;
            for (Client client : clients) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;

                // Seulement les informations de base
                setCellValue(row.createCell(colNum++), client.getId());
                setCellValue(row.createCell(colNum++), client.getNom());
                setCellValue(row.createCell(colNum++), client.getPrenom());
                setCellValue(row.createCell(colNum++), client.getCin());
                setCellValue(row.createCell(colNum++), client.getTelephone());
                setCellValue(row.createCell(colNum++), client.getNMDIR());
                setCellValue(row.createCell(colNum++), client.getNMREG());
                setCellValue(row.createCell(colNum++), client.getNMBRA() != null ? client.getNMBRA().getDisplayname() : null);
            }


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }


    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        } else if (value instanceof LocalDate) {
            cell.setCellValue(((LocalDate) value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    public byte[] exportRendezVousToExcel(List<Client> clients, LocalDate date) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (Workbook workbook = new XSSFWorkbook()) {
            String sheetName = "Rendez-vous du " + date.format(dateFormatter).replace('/', '-');
            Sheet sheet = workbook.createSheet(sheetName);

            // Créer les styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);


            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));


            CellStyle timeStyle = workbook.createCellStyle();
            timeStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("hh:mm"));


            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "Agence", "Nom", "Prénom", "CIN", "Téléphone", "Téléphone 2",
                    "Date du rendez-vous", "Heure du rendez-vous", "Activité",
                    "Conseiller assigné", "Intérêt pour un crédit", "Facteur d'influence",
                    "Raison de non-renouvellement"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4500); // Ajuster la largeur des colonnes
            }

            // Remplir les données
            int rowNum = 1;
            for (Client client : clients) {
                // Ne traiter que les clients ayant un rendez-vous et une date de rendez-vous
                if (client.getRendezVousAgence() != null && client.getRendezVousAgence() &&
                        client.getDateHeureRendezVous() != null) {

                    Row row = sheet.createRow(rowNum++);
                    int colNum = 0;

                    // Informations sur l'agence
                    Cell agenceCell = row.createCell(colNum++);
                    agenceCell.setCellValue(client.getNMBRA() != null ? client.getNMBRA().getDisplayname() : "");

                    // Informations de base du client
                    row.createCell(colNum++).setCellValue(client.getNom() != null ? client.getNom() : "");
                    row.createCell(colNum++).setCellValue(client.getPrenom() != null ? client.getPrenom() : "");
                    row.createCell(colNum++).setCellValue(client.getCin() != null ? client.getCin() : "");
                    row.createCell(colNum++).setCellValue(client.getTelephone() != null ? client.getTelephone() : "");
                    row.createCell(colNum++).setCellValue(client.getTelephone2() != null ? client.getTelephone2() : "");

                    // Informations du rendez-vous
                    LocalDateTime rdvDateTime = client.getDateHeureRendezVous();
                    if (rdvDateTime != null) {
                        // Date du rendez-vous
                        Cell dateCell = row.createCell(colNum++);
                        dateCell.setCellValue(rdvDateTime.format(dateFormatter));
                        dateCell.setCellStyle(dateStyle);

                        // Heure du rendez-vous
                        Cell timeCell = row.createCell(colNum++);
                        timeCell.setCellValue(rdvDateTime.format(timeFormatter));
                        timeCell.setCellStyle(timeStyle);
                    } else {
                        colNum += 2; // Sauter les cellules si pas de date
                    }

                    // Activité du client
                    row.createCell(colNum++).setCellValue(client.getActiviteClient() != null ?
                            client.getActiviteClient().getDisplayName() :
                            (client.getActiviteActuelle() != null ? client.getActiviteActuelle() : ""));

                    // Agent assigné
                    row.createCell(colNum++).setCellValue(client.getAssignedUser() != null ?
                            client.getAssignedUser().getName() : "Non assigné");

                    // Intérêt pour un crédit
                    row.createCell(colNum++).setCellValue(client.getInteretNouveauCredit() != null ?
                            client.getInteretNouveauCredit().getDisplayName() : "");

                    // Facteur d'influence
                    row.createCell(colNum++).setCellValue(client.getFacteurInfluence() != null ?
                            client.getFacteurInfluence().getDisplayName() : "");

                    // Raison de non-renouvellement
                    row.createCell(colNum++).setCellValue(client.getRaisonNonRenouvellement() != null ?
                            client.getRaisonNonRenouvellement().getDisplayName() : "");
                }
            }


            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}