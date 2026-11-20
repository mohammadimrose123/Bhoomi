package com.bhoomi.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class PasswordResetEmailConfiguration {

    @Bean
    PasswordResetEmailService passwordResetEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${bhoomi.email.enabled:false}") boolean enabled,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.username:}") String username,
            @Value("${bhoomi.email.from:no-reply@bhoomi.local}") String fromAddress) {
        if (!enabled) {
            return new UnconfiguredPasswordResetEmailService();
        }
        if (host.isBlank()) {
            throw new IllegalStateException("SMTP_HOST must be configured when password reset email is enabled");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("SMTP mail sender is unavailable");
        }
        return new SmtpPasswordResetEmailService(mailSender, fromAddress);
    }

    private static final class UnconfiguredPasswordResetEmailService implements PasswordResetEmailService {
        @Override
        public boolean isConfigured() {
            return false;
        }

        @Override
        public void sendResetLink(String emailAddress, String resetUrl) {
            throw new IllegalStateException("Password reset email delivery is not configured");
        }
    }

    private static final class SmtpPasswordResetEmailService implements PasswordResetEmailService {
        private final JavaMailSender mailSender;
        private final String fromAddress;

        private SmtpPasswordResetEmailService(JavaMailSender mailSender, String fromAddress) {
            this.mailSender = mailSender;
            this.fromAddress = fromAddress;
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public void sendResetLink(String emailAddress, String resetUrl) {
            var message = mailSender.createMimeMessage();
            try {
                var helper = new org.springframework.mail.javamail.MimeMessageHelper(message, false, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(emailAddress);
                helper.setSubject("Reset your Bhoomi password");
                helper.setText("Use this link before it expires to reset your Bhoomi password:\n\n"
                        + resetUrl + "\n\nIf you did not request this, you can ignore this email.");
                mailSender.send(message);
            } catch (jakarta.mail.MessagingException exception) {
                throw new org.springframework.mail.MailSendException("Unable to create password reset message", exception);
            }
        }
    }
}
