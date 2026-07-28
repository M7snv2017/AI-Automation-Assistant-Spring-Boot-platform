package com.aiautomation.automation;

import com.aiautomation.entity.User;
import com.aiautomation.security.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.aiautomation.service.NotificationService;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EncryptionUtil encryptionUtil;
    private final NotificationService notificationService;

    public void sendEmail(User user, String recipient, String subject, String body) {
        if (user.getGmailUsername() == null || user.getGmailUsername().isBlank() ||
            user.getEncryptedAppPassword() == null || user.getEncryptedAppPassword().isBlank()) {
            throw new IllegalStateException("Gmail configuration is missing. Please enter your Gmail address and Google App Password in Settings.");
        }

        String decryptedPassword = encryptionUtil.decrypt(user.getEncryptedAppPassword());

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(user.getGmailUsername());
        mailSender.setPassword(decryptedPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        String[] recipients;
        if (recipient == null || recipient.isBlank()) {
            recipients = new String[]{ (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getGmailUsername() };
        } else if (recipient.contains(",") || recipient.contains(";") || recipient.contains(" ")) {
            recipients = java.util.Arrays.stream(recipient.split("[,;\\s]+"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank() && s.contains("@"))
                    .toArray(String[]::new);
            if (recipients.length == 0) {
                recipients = new String[]{ (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getGmailUsername() };
            }
        } else {
            recipients = new String[]{ recipient.trim() };
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(user.getGmailUsername());
        message.setTo(recipients);
        message.setSubject(subject);
        message.setText(body);

        String recipientSummary = String.join(", ", recipients);
        log.info("Sending automated email to {} via Gmail SMTP...", recipientSummary);
        mailSender.send(message);
        log.info("Email sent successfully to {}", recipientSummary);

        try {
            notificationService.createNotification(user, "Email Dispatched", "Sent email to " + recipientSummary + " with subject: '" + subject + "'", "EMAIL");
        } catch (Exception e) {
            log.warn("Failed to create notification for sent email", e);
        }
    }

    public void sendHtmlEmail(User user, String recipient, String subject, String htmlBody) {
        if (user.getGmailUsername() == null || user.getGmailUsername().isBlank() ||
            user.getEncryptedAppPassword() == null || user.getEncryptedAppPassword().isBlank()) {
            throw new IllegalStateException("Gmail configuration is missing. Please enter your Gmail address and Google App Password in Settings.");
        }

        String decryptedPassword = encryptionUtil.decrypt(user.getEncryptedAppPassword());

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(user.getGmailUsername());
        mailSender.setPassword(decryptedPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        String[] recipients;
        if (recipient == null || recipient.isBlank()) {
            recipients = new String[]{ (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getGmailUsername() };
        } else if (recipient.contains(",") || recipient.contains(";") || recipient.contains(" ")) {
            recipients = java.util.Arrays.stream(recipient.split("[,;\\s]+"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank() && s.contains("@"))
                    .toArray(String[]::new);
            if (recipients.length == 0) {
                recipients = new String[]{ (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getGmailUsername() };
            }
        } else {
            recipients = new String[]{ recipient.trim() };
        }

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(user.getGmailUsername());
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            String recipientSummary = String.join(", ", recipients);
            log.info("Sending automated HTML email to {} via Gmail SMTP...", recipientSummary);
            mailSender.send(message);
            log.info("HTML Email sent successfully to {}", recipientSummary);

            try {
                notificationService.createNotification(user, "Report Email Dispatched", "Sent HTML report email to " + recipientSummary + " with subject: '" + subject + "'", "EMAIL");
            } catch (Exception e) {
                log.warn("Failed to create notification for sent HTML email", e);
            }
        } catch (Exception e) {
            log.error("Failed to send HTML email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }

    public String sendTestEmail(User user) {
        String sender = user.getGmailUsername();
        if (sender == null || sender.isBlank()) {
            throw new IllegalStateException("No Gmail address configured. Please save your Gmail address first.");
        }
        String recipient = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : sender;
        String subject = "AI Automation Assistant - Gmail Connection Test";
        String body = "This is a test email sent from your configured Gmail address (" + sender + ").\n\nYour Gmail integration is connected successfully!";

        sendEmail(user, recipient, subject, body);
        return recipient;
    }
}
