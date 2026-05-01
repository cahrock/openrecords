package com.openrecords.api.controller;

import com.openrecords.api.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY endpoint for testing email infrastructure.
 * REMOVE in Phase 7-3 once real notification triggers exist.
 */
@RestController
@RequestMapping("/api/v1/dev/email")
public class NotificationTestController {

    private final NotificationService notificationService;

    public NotificationTestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TestEmailResponse sendTest(@RequestBody TestEmailRequest request) {
        notificationService.sendTestEmail(
            request.to(),
            request.subject() != null ? request.subject() : "OpenRecords test email",
            request.body() != null ? request.body() :
                "<p>This is a test email from <strong>OpenRecords</strong>.</p>" +
                "<p>If you're seeing this in MailHog, your email infrastructure is working.</p>"
        );
        return new TestEmailResponse("Email queued for delivery");
    }

    public record TestEmailRequest(String to, String subject, String body) {}
    public record TestEmailResponse(String message) {}
}