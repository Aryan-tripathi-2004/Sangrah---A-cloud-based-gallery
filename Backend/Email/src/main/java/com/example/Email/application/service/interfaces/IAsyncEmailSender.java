package com.example.Email.application.service.interfaces;

public interface IAsyncEmailSender {
    void sendHtmlEmailAsync(String emailLogId, String fromEmail, String fromName, String toEmail, String subject, String htmlContent);
    void sendEmailWithAttachmentAsync(String emailLogId, String fromEmail, String fromName, String toEmail, String subject, String htmlContent, String attachmentFilename, byte[] attachmentBytes);
}
