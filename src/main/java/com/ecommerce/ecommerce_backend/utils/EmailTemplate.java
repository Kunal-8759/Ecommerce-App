package com.ecommerce.ecommerce_backend.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.OrderItem;

@Component
public class EmailTemplate {

    //  Load and fill order confirmation template 
    public String buildOrderConfirmationEmail(Order order, String appName, String orderDate) 
            throws IOException {

        // 1. Load the raw HTML from resources/templates/
        String template = loadTemplate("templates/order-confirmation.html");

        // 2. Build the order items HTML rows
        String itemRows = buildOrderItemRows(order);

        // 3. Replace all {{placeholders}} with actual values
        return template
                .replace("{{appName}}",       appName)
                .replace("{{customerName}}",  order.getUser().getName())
                .replace("{{orderId}}",       String.valueOf(order.getId()))
                .replace("{{orderDate}}",     orderDate)
                .replace("{{paymentMethod}}", order.getPaymentMethod() != null
                        ? order.getPaymentMethod().name().replace("_", " ")
                        : "N/A")
                .replace("{{orderItems}}",    itemRows)
                .replace("{{totalAmount}}",   "₹" + order.getTotalAmount());
    }

    //  Load HTML file from resources/ 
    private String loadTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(
                resource.getInputStream(),
                StandardCharsets.UTF_8);
    }

    //  Build HTML rows for each order item 
    private String buildOrderItemRows(Order order) {
        StringBuilder rows = new StringBuilder();

        for (OrderItem item : order.getOrderItems()) {
            BigDecimal subtotal = item.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            rows.append("<tr>")
                .append("<td style='padding:10px; border-bottom:1px solid #f0f0f0;'>")
                    .append(item.getProduct().getName())
                .append("</td>")
                .append("<td style='padding:10px; border-bottom:1px solid #f0f0f0; text-align:center;'>")
                    .append(item.getQuantity())
                .append("</td>")
                .append("<td style='padding:10px; border-bottom:1px solid #f0f0f0; text-align:right;'>")
                    .append("₹").append(item.getPrice())
                .append("</td>")
                .append("<td style='padding:10px; border-bottom:1px solid #f0f0f0; text-align:right;'>")
                    .append("₹").append(subtotal)
                .append("</td>")
                .append("</tr>");
        }

        return rows.toString();
    }
}