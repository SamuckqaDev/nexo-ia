package com.nexoia.auth.recovery.provider;

import com.nexoia.auth.recovery.config.PasswordRecoveryProperties;
import com.nexoia.auth.recovery.exception.PasswordResetDeliveryException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(PasswordRecoveryProperties.class)
public class SmtpPasswordResetDelivery implements PasswordResetDelivery {

    private final JavaMailSender mailSender;
    private final PasswordRecoveryProperties properties;

    @Override
    public void send(String email, String name, String token) {
        String resetUrl = UriComponentsBuilder.fromUriString(properties.frontendResetUrl())
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.sender());
        message.setTo(email);
        message.setSubject("Reset your Nexo IA password");
        message.setText("Hello " + name + ",\n\nUse this secure link to reset your Nexo IA password:\n"
                + resetUrl + "\n\nIf you did not request this change, ignore this message.");

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new PasswordResetDeliveryException(exception);
        }
    }
}
