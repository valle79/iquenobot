package com.iquenobot.shared.application;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String to, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Recuperación de contraseña - IquenoBot";
        String html = buildPasswordResetHtml(resetLink);

        sendHtmlEmail(to, subject, html);
        log.info("Password reset email sent to: {} with link: {}", to, resetLink);
    }

    public void sendVerificationEmail(String to, String token) {
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        String subject = "Verifica tu email - IquenoBot";
        String html = buildVerificationHtml(verifyLink);

        sendHtmlEmail(to, subject, html);
        log.info("Verification email sent to: {} with link: {}", to, verifyLink);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.debug("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}. Check mail configuration.", to, e.getMessage());
        }
    }

    private String buildPasswordResetHtml(String resetLink) {
        return """
            <!DOCTYPE html>
            <html><body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Recuperación de contraseña</h2>
                <p>Has solicitado restablecer tu contraseña. Haz clic en el siguiente enlace:</p>
                <p><a href="%s" style="background: #6366f1; color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px;">Restablecer contraseña</a></p>
                <p>Si no solicitaste este cambio, ignora este mensaje.</p>
                <p>Este enlace expira en 24 horas.</p>
                <hr><small>IquenoBot - CRM Omnicanal con IA</small>
            </body></html>
            """.formatted(resetLink);
    }

    private String buildVerificationHtml(String verifyLink) {
        return """
            <!DOCTYPE html>
            <html><body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Verifica tu email</h2>
                <p>Gracias por registrarte. Haz clic en el siguiente enlace para verificar tu email:</p>
                <p><a href="%s" style="background: #6366f1; color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px;">Verificar email</a></p>
                <p>Si no creaste una cuenta, ignora este mensaje.</p>
                <hr><small>IquenoBot - CRM Omnicanal con IA</small>
            </body></html>
            """.formatted(verifyLink);
    }
}
