package com.impulsaElCambio.Servicios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender emailSender;
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void enviarCorreoRecuperacion(String destinatario, String nuevaPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario);
            message.setSubject("Recuperación de Contraseña - Impulsa el Cambio");
            message.setText("Hola,\n\n" +
                    "Tu nueva contraseña temporal es: " + nuevaPassword + "\n\n" +
                    "Por favor, cambia esta contraseña tan pronto inicies  sesión.\n\n" +
                    "Saludos,\nEquipo de Impulsa el Cambio");

            emailSender.send(message);
            logger.info("Correo de recuperación enviado a: {}", destinatario);
        } catch (Exception e) {
            logger.error("Error al enviar correo de recuperación: ", e);
            throw new RuntimeException("Error al enviar el correo de recuperación", e);
        }
    }
} 