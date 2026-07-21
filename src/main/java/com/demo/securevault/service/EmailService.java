package com.demo.securevault.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@securevault.com}")
    private String fromEmail;

    /**
     * Sends an email with a secure file download link
     * @param toEmail The recipient's email address
     * @param fileId The unique identifier for the file
     * @param originalFilename The original name of the uploaded file
     */
    public void sendFileLink(String toEmail, String fileId, String originalFilename) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Secure File is Ready - SecureVault");

            String htmlContent = buildEmailContent(fileId, originalFilename);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Builds the HTML email content with file details
     */
    private String buildEmailContent(String fileId, String originalFilename) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <style>" +
                "    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "    .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "    .header { background-color: #2c3e50; color: white; padding: 20px; text-align: center; border-radius: 4px 4px 0 0; }" +
                "    .content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }" +
                "    .footer { background-color: #ecf0f1; padding: 15px; text-align: center; font-size: 12px; color: #7f8c8d; border-radius: 0 0 4px 4px; }" +
                "    .file-info { background-color: #ffffff; border-left: 4px solid #3498db; padding: 15px; margin: 15px 0; }" +
                "    .file-id { background-color: #ecf0f1; padding: 10px; border-radius: 4px; font-family: monospace; font-size: 14px; word-break: break-all; margin: 10px 0; }" +
                "    .note { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 15px 0; color: #856404; }" +
                "    h2 { color: #2c3e50; margin-top: 0; }" +
                "    p { margin: 10px 0; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"container\">" +
                "    <div class=\"header\">" +
                "      <h1>SecureVault</h1>" +
                "      <p>Your Secure File is Ready</p>" +
                "    </div>" +
                "    <div class=\"content\">" +
                "      <h2>Welcome to SecureVault!</h2>" +
                "      <p>Your file has been securely uploaded and is ready for download.</p>" +
                "      " +
                "      <div class=\"file-info\">" +
                "        <h3 style=\"margin-top: 0; color: #2c3e50;\">File Details</h3>" +
                "        <p><strong>File Name:</strong> " + escapeHtml(originalFilename) + "</p>" +
                "        <p><strong>File ID:</strong></p>" +
                "        <div class=\"file-id\">" + escapeHtml(fileId) + "</div>" +
                "      </div>" +
                "      " +
                "      <h3>How to Download</h3>" +
                "      <p>To download your file, you will need:</p>" +
                "      <ul>" +
                "        <li>Your <strong>File ID</strong> (shown above)</li>" +
                "        <li>Your <strong>password</strong> that you set during upload</li>" +
                "      </ul>" +
                "      " +
                "      <div class=\"note\">" +
                "        <strong>⚠️ Important:</strong> This file is encrypted. You will need your password to download it. " +
                "        Keep your password safe and do not share it with anyone." +
                "      </div>" +
                "      " +
                "      <p>Thank you for using SecureVault!</p>" +
                "    </div>" +
                "    <div class=\"footer\">" +
                "      <p>&copy; 2024 SecureVault. All rights reserved.</p>" +
                "      <p>This is an automated email. Please do not reply to this address.</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Escapes HTML special characters to prevent injection
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
