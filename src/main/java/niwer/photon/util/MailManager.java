package niwer.photon.util;

import java.io.InputStream;
import java.util.Scanner;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.email.Recipient;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.recipient.RecipientBuilder;

import jakarta.mail.Message.RecipientType;
import niwer.photon.Directories;
import niwer.photon.util.TranslationManager.Language;

/**
 * This class is responsible for managing email sending functionality. It provides methods to send emails, configure email settings, and handle email-related tasks.
 * 
 * @author Niwer
 */
public class MailManager {

    private static Mailer mailer = getMailer();

    private MailManager() {}

    private static Mailer getMailer() {
        if(mailer != null) return mailer; // Return the existing mailer if it has already been initialized

        /* If the mailer is not initialized, create a new one */
        if(!Directories.config().hasEmailConfig()) throw new IllegalStateException("Mail configuration is not set. Please check the configuration file.");

        return mailer = MailerBuilder.withSMTPServer(
            Directories.config().mail_smtp_host,
            Directories.config().mail_smtp_port,
            Directories.config().mail_username,
            Directories.config().mail_password)
        .withTransportStrategy(TransportStrategy.SMTP_TLS)
        .async()
        .buildMailer();
    }

    public static void sendContactMessage(String senderName, Language lang, String visitorEmail, String message) {
        if(!Directories.config().hasEmailConfig()) throw new IllegalStateException("Mail configuration is not set.");
        if (visitorEmail == null || visitorEmail.isBlank()) throw new IllegalArgumentException("Visitor email cannot be null or blank.");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Message body cannot be null or blank.");

        String displayName = (senderName != null && !senderName.isBlank()) ? senderName : "Anonymous";
        String body = "Name : " + displayName + "\nEmail : " + visitorEmail + "\n\nMessage :\n" + message;

        final Email email = EmailBuilder.startingBlank()
            .from("Contact Form", Directories.config().mail_sender_email)
            // .to("Admin", Directories.getConfig().mail_sender_email)
            .withReplyTo(displayName, visitorEmail)
            .withSubject("New message from : " + displayName + " via Contact Form")
            .withPlainText(body)
            .buildEmail();

        MailManager.getMailer().sendMail(email);
    }

    /**
     * Sends an email to the specified recipient with the given subject and body.
     * 
     * @param recipientEmail The email address of the recipient.
     * @param type The type of recipient (TO, CC, BCC).
     * @param subject The subject of the email.
     * @param body The body of the email.
     */
    public static void sendEmail(String recipientEmail, RecipientType type, String subject, String body) {
        if(!Directories.config().hasEmailConfig()) throw new IllegalStateException("Mail configuration is not set. Please check the configuration file.");
        if(body == null || body.isBlank()) throw new IllegalArgumentException("Email body cannot be null or blank.");

        /* Create the email */
        final Email EMAIL = initializeEmail(recipientEmail, type, subject).withPlainText(body).buildEmail();

        /* Send the email */
        MailManager.getMailer().sendMail(EMAIL);
    }

    /**
     * Sends an HTML email to the specified recipient with the given subject and HTML body.
     * 
     * @param recipientEmail The email address of the recipient.
     * @param type The type of recipient (TO, CC, BCC).
     * @param subject The subject of the email.
     * @param htmlBody The HTML body of the email.
     */
    public static void sendHtmlEmail(String recipientEmail, RecipientType type, String subject, String htmlBody) {
        if(!Directories.config().hasEmailConfig()) throw new IllegalStateException("Mail configuration is not set. Please check the configuration file.");
        if(htmlBody == null || htmlBody.isBlank()) throw new IllegalArgumentException("Email HTML body cannot be null or blank.");

        /* Create the email */
        final Email EMAIL = initializeEmail(recipientEmail, type, subject).withHTMLText(htmlBody).buildEmail();

        /* Send the email */
        MailManager.getMailer().sendMail(EMAIL);
    }

    /**
     * Sends an HTML email to the specified recipient with the given subject and HTML content loaded from a file.
     * 
     * @param recipientEmail The email address of the recipient.
     * @param type The type of recipient (TO, CC, BCC).
     * @param subject The subject of the email.
     * @param htmlFileName The path to the HTML file containing the email content.
     */
    public static void sendHtmlFileEmail(String recipientEmail, RecipientType type, String subject, String htmlFileName) {
        if(!Directories.config().hasEmailConfig()) throw new IllegalStateException("Mail configuration is not set. Please check the configuration file.");
        if(htmlFileName == null || htmlFileName.isBlank()) throw new IllegalArgumentException("Email HTML file cannot be null or blank.");

        try(InputStream STREAM = OperatingSystem.loadFile("mail/" + htmlFileName)) {
            if(STREAM == null) throw new IllegalArgumentException("Email HTML file not found: " + htmlFileName);

            /* Read the HTML content */
            StringBuilder htmlContent = new StringBuilder();
            try (Scanner SCANNER = new Scanner(STREAM, "UTF-8")) {
                while (SCANNER.hasNextLine()) htmlContent.append(SCANNER.nextLine()).append(System.lineSeparator());
            }

            /* Send the email */
            sendHtmlEmail(recipientEmail, type, subject, htmlContent.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Error loading email HTML file: " + htmlFileName, e);
        }
    }

    private static EmailPopulatingBuilder initializeEmail(String recipientEmail, RecipientType type, String subject) {
        if(recipientEmail == null || recipientEmail.isBlank()) throw new IllegalArgumentException("Recipient email cannot be null or blank.");
        if(type == null) throw new IllegalArgumentException("Recipient type cannot be null.");
        if(subject == null || subject.isBlank()) throw new IllegalArgumentException("Email subject cannot be null or blank.");

        return EmailBuilder.startingBlank().from("Sender", Directories.config().mail_sender_email).withRecipients(forRecipient(recipientEmail, type)).withSubject(subject);
    }

    /**
     * Creates a Recipient object for the given email address and recipient type.
     * 
     * @param recipientEmail The email address of the recipient.
     * @param type The type of recipient (TO, CC, BCC).
     * @return A Recipient object representing the specified recipient.
     */
    private static Recipient forRecipient(String recipientEmail, RecipientType type) {
        return new RecipientBuilder().withName("Recipient").withAddress(recipientEmail).withType(type).build();
    }
}
