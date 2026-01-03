package com.example.Auth.service;

import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

import static com.example.Auth.util.MessageConstants.*;

/**
 * Service for sending emails using Spring Mail and Thymeleaf templates.
 */
@Service
public class EmailService {

    private static final DashLogger logger = DashLoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Sangrah}")
    private String appName;

    @Value("${app.otp.validity-minutes:10}")
    private int otpValidityMinutes;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends an email verification OTP to the user.
     *
     * @param email       The recipient email address
     * @param displayName The user's display name
     * @param otpCode     The OTP code
     */
    public void sendVerificationEmail(String email, String displayName, String otpCode) {
        logger.info(MSG_SENDING_VERIFICATION_EMAIL, Map.of(
                "email", email,
                "displayName", displayName));

        try {
            Context context = new Context();
            context.setVariable("displayName", displayName);
            context.setVariable("email", email);
            context.setVariable("otpCode", otpCode);
            context.setVariable("expiryMinutes", otpValidityMinutes);

            String htmlContent = templateEngine.process("email-verification", context);

            sendEmail(email, "Verify Your Email - " + appName, htmlContent);

            logger.info(MSG_VERIFICATION_EMAIL_SENT_SUCCESS, Map.of("email", email));
        } catch (Exception e) {
            logger.error(MSG_FAILED_SEND_VERIFICATION_EMAIL_LOG, Map.of(
                    "email", email,
                    "error", e.getMessage()));
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Sends an HTML email.
     *
     * @param to          The recipient email address
     * @param subject     The email subject
     * @param htmlContent The HTML content
     */
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);

            logger.debug(MSG_EMAIL_SENT, Map.of(
                    "to", to,
                    "subject", subject));
        } catch (MessagingException e) {
            logger.error(MSG_FAILED_SEND_EMAIL, Map.of(
                    "to", to,
                    "subject", subject,
                    "error", e.getMessage()));
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Sends a password reset OTP email.
     *
     * @param email       The recipient email address
     * @param displayName The user's display name
     * @param otpCode     The OTP code
     */
    public void sendPasswordResetEmail(String email, String displayName, String otpCode) {
        logger.info(MSG_SENDING_PASSWORD_RESET_EMAIL, Map.of("email", email));

        try {
            Context context = new Context();
            context.setVariable("displayName", displayName);
            context.setVariable("email", email);
            context.setVariable("otpCode", otpCode);
            context.setVariable("expiryMinutes", otpValidityMinutes);

            // For now, reuse the same template (you can create a separate template later)
            String htmlContent = templateEngine.process("email-verification", context);

            sendEmail(email, "Password Reset - " + appName, htmlContent);

            logger.info(MSG_PASSWORD_RESET_EMAIL_SENT_SUCCESS, Map.of("email", email));
        } catch (Exception e) {
            logger.error(MSG_FAILED_SEND_PASSWORD_RESET_EMAIL, Map.of(
                    "email", email,
                    "error", e.getMessage()));
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
