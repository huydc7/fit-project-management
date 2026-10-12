package com.example.quanlydetai.notification;

/** A successful send means the mail provider accepted the message, not inbox delivery. */
public interface EmailTransport {
    String send(String id, String recipient, String subject, String body);
}
