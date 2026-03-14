package com.ecommerce.ecommerce_backend.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.OrderItem;
import com.ecommerce.ecommerce_backend.utils.EmailTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailTemplate emailTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name}")
    private String appName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─── Send Order Confirmation Email ────────────────────────────────
    // @Async: email is sent in background thread
    // Payment API returns response immediately without waiting for email
    @Async
    public void sendOrderConfirmationEmail(Order order) {

        try {
            String orderDate = order.getOrderDate().format(DATE_FORMATTER);

            // Delegate all HTML building to EmailTemplateUtil
            String htmlContent = emailTemplate
                    .buildOrderConfirmationEmail(order, appName, orderDate);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject(appName + " — Order Confirmed #" + order.getId());
            helper.setText(htmlContent, true); // true = send as HTML

            mailSender.send(message);

            System.out.println("[EmailService] Confirmation sent to: "
                    + order.getUser().getEmail()
                    + " | Order ID: " + order.getId());

        } catch (Exception e) {
            // Email failure must never crash the payment flow
            System.err.println("[EmailService] Failed to send email for Order ID: " + order.getId() + " | Error: " + e.getMessage());
        }
    }
}
