package com.openrecords.api.service;

import com.openrecords.api.config.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.openrecords.api.domain.FoiaRequest;
import com.openrecords.api.domain.FoiaRequestStatus;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Sends notification emails asynchronously.
 *
 * All public methods are annotated @Async("emailExecutor"), so callers
 * return immediately. Email sending happens on a background thread pool.
 * Errors are logged; they do NOT propagate to the caller.
 *
 * Policy: emails are fire-and-forget. If SMTP is unavailable, the user
 * action (registration, status change) still succeeds; the email failure
 * is logged for debugging. Never block a user action on email delivery.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties props;
    private final TemplateEngine templateEngine;

    public NotificationService(
        JavaMailSender mailSender,
        EmailProperties props,
        TemplateEngine templateEngine
    ) {
        this.mailSender = mailSender;
        this.props = props;
        this.templateEngine = templateEngine;
    }

    /**
     * Test method — sends a simple email. Used to verify the wiring works.
     * Will be removed in Phase 7-3 once real notification methods exist.
     */
    @Async("emailExecutor")
    public void sendTestEmail(String toAddress, String subject, String htmlBody) {
        log.info("Sending test email to {}", toAddress);
        sendHtmlEmail(toAddress, subject, htmlBody);
    }

    /**
     * Internal helper that sends HTML email.
     * Catches and logs all email-related exceptions; does NOT re-throw.
     */
    private void sendHtmlEmail(String toAddress, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message, false, StandardCharsets.UTF_8.name()
            );

            helper.setFrom(new InternetAddress(props.getFrom(), props.getFromName(),
                StandardCharsets.UTF_8.name()));
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);  // true = HTML body

            mailSender.send(message);
            log.info("Email sent successfully to {} (subject: {})", toAddress, subject);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to construct email to {}: {}", toAddress, e.getMessage(), e);
        } catch (MailException e) {
            log.error("Failed to send email to {} (SMTP error): {}", toAddress, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toAddress, e.getMessage(), e);
        }
    }

    /**
     * Send the email verification link to a newly-registered user.
     */
    @Async("emailExecutor")
    public void sendVerificationEmail(String toAddress, String fullName, String verificationToken) {
        String verificationLink = props.getFrontendBaseUrl() + "/verify?token=" + verificationToken;

        Context ctx = new Context();
        ctx.setVariable("fullName", fullName);
        ctx.setVariable("verificationLink", verificationLink);
        ctx.setVariable("frontendBaseUrl", props.getFrontendBaseUrl());

        String body = templateEngine.process("email/verification", ctx);

        log.info("Sending verification email to {}", toAddress);
        sendHtmlEmail(toAddress, "Verify your OpenRecords account", body);
    }

    /**
     * Send a welcome email after successful email verification.
     * Confirms the account is active and points to the next action.
     */
    @Async("emailExecutor")
    public void sendWelcomeEmail(String toAddress, String fullName) {
        String loginLink = props.getFrontendBaseUrl() + "/login";

        Context ctx = new Context();
        ctx.setVariable("fullName", fullName);
        ctx.setVariable("loginLink", loginLink);
        ctx.setVariable("frontendBaseUrl", props.getFrontendBaseUrl());

        String body = templateEngine.process("email/welcome", ctx);

        log.info("Sending welcome email to {}", toAddress);
        sendHtmlEmail(toAddress, "Welcome to OpenRecords", body);
    }

    /**
     * Send a status-change notification to the requester for major workflow events.
     * Returns silently for transitions that aren't user-facing (e.g., ASSIGNED, UNDER_REVIEW).
     */
    @Async("emailExecutor")
    public void sendStatusChangeEmail(FoiaRequest request, FoiaRequestStatus newStatus) {
        StatusEmailContent content = contentFor(newStatus);
        if (content == null) {
            return;
        }

        String to = request.getRequester().getEmail();
        String requesterName = request.getRequester().getFullName();
        String detailLink = props.getFrontendBaseUrl() + "/requests/" + request.getId();

        Context ctx = new Context();
        ctx.setVariable("requesterName", requesterName);
        ctx.setVariable("headline", content.headline());
        ctx.setVariable("detail", content.detail());
        ctx.setVariable("trackingNumber", request.getTrackingNumber());
        ctx.setVariable("requestSubject", request.getSubject());
        ctx.setVariable("detailLink", detailLink);
        ctx.setVariable("frontendBaseUrl", props.getFrontendBaseUrl());

        String body = templateEngine.process("email/status-change", ctx);

        log.info("Sending {} status notification for {} to {}",
            newStatus, request.getTrackingNumber(), to);
        sendHtmlEmail(to, content.subject(), body);
    }

    /**
     * Determines the user-facing copy for a status transition.
     * Returns null for transitions that should NOT trigger an email
     * (internal staff workflow steps).
     */
    private StatusEmailContent contentFor(FoiaRequestStatus status) {
        return switch (status) {
            case ACKNOWLEDGED -> new StatusEmailContent(
                "Your FOIA request was acknowledged",
                "Your request has been received and acknowledged.",
                "We've started the review process. You'll receive another email when there's an update."
            );
            case REJECTED -> new StatusEmailContent(
                "Your FOIA request was rejected",
                "After review, we're unable to fulfill this request.",
                "Sign in to view the full reason and any options for appeal."
            );
            case ON_HOLD -> new StatusEmailContent(
                "Your FOIA request is on hold",
                "We've placed your request on hold while we work through some details.",
                "You'll receive an update when the review resumes."
            );
            case DOCUMENTS_RELEASED -> new StatusEmailContent(
                "Documents released for your FOIA request",
                "We've completed our review and documents are now available.",
                "Sign in to download the released records."
            );
            case NO_RECORDS -> new StatusEmailContent(
                "No records found for your FOIA request",
                "After a thorough search, we found no records responsive to this request.",
                "Sign in to view the full response details."
            );
            case CLOSED -> new StatusEmailContent(
                "Your FOIA request was closed",
                "Your request has been closed.",
                "Sign in to view the final response and any released documents."
            );
            // Internal-only transitions — no user email
            case DRAFT, SUBMITTED, ASSIGNED, UNDER_REVIEW, RESPONSIVE_RECORDS_FOUND -> null;
        };
    }

    /**
     * Internal record holding the variable parts of a status-change email.
     */
    private record StatusEmailContent(
        String subject,
        String headline,
        String detail
    ) {}

}