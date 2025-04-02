package com.example.projetpfe.util;

import com.example.projetpfe.entity.Client;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExcelExportUtil {

    public byte[] exportClientsToExcel(List<Client> clients) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Clients");

            // Créer les styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Créer l'en-tête
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "Nom", "Prénom", "CIN", "Téléphone", "Téléphone 2",
                    "Statut", "Activité", "Intérêt Nouveau Crédit",
                    "Date Dernière MAJ", "Utilisateur Assigné"
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

                row.createCell(0).setCellValue(client.getNom() != null ? client.getNom() : "");
                row.createCell(1).setCellValue(client.getPrenom() != null ? client.getPrenom() : "");
                row.createCell(2).setCellValue(client.getCin() != null ? client.getCin() : "");
                row.createCell(3).setCellValue(client.getTelephone() != null ? client.getTelephone() : "");
                row.createCell(4).setCellValue(client.getTelephone2() != null ? client.getTelephone2() : "");

                row.createCell(5).setCellValue(client.getStatus() != null ? client.getStatus().getDisplayName() : "");
                row.createCell(6).setCellValue(client.getActiviteClient() != null ? client.getActiviteClient().getDisplayName() : "");
                row.createCell(7).setCellValue(client.getInteretNouveauCredit() != null ? client.getInteretNouveauCredit().getDisplayName() : "");

                row.createCell(8).setCellValue(client.getUpdatedAt() != null ? client.getUpdatedAt().format(dateTimeFormatter) : "");
                row.createCell(9).setCellValue(client.getAssignedUser() != null ? client.getAssignedUser().getName() : "Non assigné");
            }

            // Écrire le workbook dans un ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}