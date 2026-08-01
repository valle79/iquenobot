package com.iquenobot.shared.application;

import com.iquenobot.shared.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender defaultMailSender;
    private final SystemSettingsService settingsService;

    @Value("${spring.mail.username}")
    private String defaultFromEmail;

    @Value("${app.base-url:http://localhost:8085}")
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

    public void sendTestEmail(String to) {
        String subject = "Correo de prueba - IquenoBot";
        String html = """
            <!DOCTYPE html>
            <html><body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Configuración SMTP correcta</h2>
                <p>Este correo confirma que la configuración SMTP de IquenoBot es correcta y funcional.</p>
                <p>Si recibiste este mensaje, tu servidor de correo está listo para enviar notificaciones.</p>
                <hr><small>IquenoBot - CRM Omnicanal con IA</small>
            </body></html>
            """;

        boolean sent = sendHtmlEmail(to, subject, html, true);
        if (!sent) {
            throw new BusinessException("No se pudo enviar el correo de prueba. Revisa la configuración SMTP.");
        }
        log.info("Test email sent to: {}", to);
    }

    private boolean sendHtmlEmail(String to, String subject, String html) {
        return sendHtmlEmail(to, subject, html, false);
    }

    private boolean sendHtmlEmail(String to, String subject, String html, boolean force) {
        if (!force && !settingsService.getBoolean("notifications", "email_notifications", true)) {
            log.debug("Global email notifications disabled. Skipping email to {}", to);
            return false;
        }
        try {
            JavaMailSender sender = resolveMailSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(resolveFromEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.debug("Email sent successfully to: {}", to);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}. Check mail configuration.", to, e.getMessage());
            return false;
        }
    }

    /**
     * Resuelve el JavaMailSender según la configuración global del panel de Super Admin.
     * Si el SMTP global no está configurado, usa el sender por defecto de Spring.
     */
    private JavaMailSender resolveMailSender() {
        String host = settingsService.getString("smtp", "smtp_host", null);
        if (host == null || host.isBlank()) {
            log.debug("Using default JavaMailSender (no global SMTP configured)");
            return defaultMailSender;
        }

        int port = settingsService.getInt("smtp", "smtp_port", 587);
        String user = settingsService.getString("smtp", "smtp_user", "");
        String pass = settingsService.getString("smtp", "smtp_pass", "");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(user);
        sender.setPassword(pass);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        log.debug("Using global SMTP configuration: {}:{}", host, port);
        return sender;
    }

    private String resolveFromEmail() {
        String from = settingsService.getString("smtp", "smtp_from", null);
        if (from != null && !from.isBlank()) {
            return from;
        }
        return defaultFromEmail;
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
