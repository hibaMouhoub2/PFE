package com.example.projetpfe.service.Impl;

import com.example.projetpfe.entity.Client;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendRendezVousEmail(String to, String subject, String body, byte[] attachment, String attachmentName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true); // true pour activer le HTML

        if (attachment != null && attachmentName != null) {
            helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
        }

        mailSender.send(message);
    }

    public void sendRendezVousSummary(String to, List<Client> clients, LocalDate date, byte[] excelAttachment) throws MessagingException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = date.format(formatter);

        // Construction du sujet
        String subject = "Récapitulatif des rendez-vous du " + formattedDate;

        // Construction du corps du message en HTML
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<h2>Récapitulatif des rendez-vous du ").append(formattedDate).append("</h2>");
        body.append("<p>Veuillez trouver ci-joint le récapitulatif des rendez-vous clients pour la journée.</p>");

        if (!clients.isEmpty()) {
            body.append("<table border='1' cellpadding='5' style='border-collapse: collapse;'>");
            body.append("<tr style='background-color: #f2f2f2;'>");
            body.append("<th>Nom</th><th>Prénom</th><th>Téléphone</th><th>Heure</th><th>Agence</th>");
            body.append("</tr>");

            for (Client client : clients) {
                if (client.getDateHeureRendezVous() != null) {
                    body.append("<tr>");
                    body.append("<td>").append(client.getNom()).append("</td>");
                    body.append("<td>").append(client.getPrenom()).append("</td>");
                    body.append("<td>").append(client.getTelephone()).append("</td>");
                    body.append("<td>").append(client.getDateHeureRendezVous().format(DateTimeFormatter.ofPattern("HH:mm"))).append("</td>");
                    body.append("<td>").append(client.getNMBRA() != null ? client.getNMBRA().getDisplayname() : "N/A").append("</td>");
                    body.append("</tr>");
                }
            }

            body.append("</table>");
        } else {
            body.append("<p>Aucun rendez-vous prévu pour cette journée.</p>");
        }

        body.append("<p>Un fichier Excel détaillé est joint à cet email.</p>");
        body.append("<p>Cordialement,<br>ATTAWFIQ MICRO-FINANCE</p>");
        body.append("</body></html>");

        // Nom du fichier joint
        String attachmentName = "rendez_vous_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        sendRendezVousEmail(to, subject, body.toString(), excelAttachment, attachmentName);
    }
//    public void sendEmail(String to, String subject, String text) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom("MOUHOUB_HIBA@emsi-edu.ma"); // Doit correspondre à votre username SMTP
//        message.setTo(to);
//        message.setSubject(subject);
//        message.setText(text);
//        javaMailSender.send(message);
//    }
}