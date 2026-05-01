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

    public NotificationService(JavaMailSender mailSender, EmailProperties props) {
        this.mailSender = mailSender;
        this.props = props;
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

        String subject = "Verify your OpenRecords account";
        String body = buildVerificationBody(fullName, verificationLink);

        log.info("Sending verification email to {}", toAddress);
        sendHtmlEmail(toAddress, subject, body);
    }

    /**
     * Send a welcome email after successful email verification.
     * Confirms the account is active and points to the next action.
     */
    @Async("emailExecutor")
    public void sendWelcomeEmail(String toAddress, String fullName) {
        String loginLink = props.getFrontendBaseUrl() + "/login";
        String subject = "Welcome to OpenRecords";
        String body = buildWelcomeBody(fullName, loginLink);

        log.info("Sending welcome email to {}", toAddress);
        sendHtmlEmail(toAddress, subject, body);
    }

    private String buildWelcomeBody(String fullName, String loginLink) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, 'Segoe UI', Roboto, sans-serif; \
                         max-width: 560px; margin: 0 auto; padding: 24px; color: #1f2937;">
              <div style="background: #1e3a8a; color: white; padding: 16px 20px; \
                          border-radius: 8px 8px 0 0;">
                <h1 style="margin: 0; font-size: 20px;">OpenRecords FOIA Portal</h1>
              </div>
              <div style="background: white; padding: 24px 20px; border: 1px solid #e5e7eb; \
                          border-top: none; border-radius: 0 0 8px 8px;">
                <h2 style="font-size: 18px; margin-top: 0;">Welcome aboard, %s</h2>
                <p>Your email is verified and your account is active.</p>
                <p>OpenRecords lets you file Freedom of Information Act (FOIA) requests \
                   to federal agencies, track their progress through the review workflow, \
                   and download released documents — all in one place.</p>
                <p style="text-align: center; margin: 32px 0;">
                  <a href="%s" \
                     style="background: #1e3a8a; color: white; padding: 12px 24px; \
                            text-decoration: none; border-radius: 6px; \
                            display: inline-block; font-weight: 600;">
                    Sign in to get started
                  </a>
                </p>
                <p style="font-size: 14px; color: #6b7280; margin-top: 32px;">
                  Questions? Reply to this email or reach our support team.
                </p>
              </div>
            </body>
            </html>
            """.formatted(fullName, loginLink);
    }

    private String buildVerificationBody(String fullName, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, 'Segoe UI', Roboto, sans-serif; \
                         max-width: 560px; margin: 0 auto; padding: 24px; color: #1f2937;">
              <div style="background: #1e3a8a; color: white; padding: 16px 20px; \
                          border-radius: 8px 8px 0 0;">
                <h1 style="margin: 0; font-size: 20px;">OpenRecords FOIA Portal</h1>
              </div>
              <div style="background: white; padding: 24px 20px; border: 1px solid #e5e7eb; \
                          border-top: none; border-radius: 0 0 8px 8px;">
                <h2 style="font-size: 18px; margin-top: 0;">Welcome, %s</h2>
                <p>Thanks for creating an OpenRecords account. To activate it, \
                   verify your email address by clicking the button below.</p>
                <p style="text-align: center; margin: 32px 0;">
                  <a href="%s" \
                     style="background: #1e3a8a; color: white; padding: 12px 24px; \
                            text-decoration: none; border-radius: 6px; \
                            display: inline-block; font-weight: 600;">
                    Verify email address
                  </a>
                </p>
                <p style="font-size: 14px; color: #6b7280;">
                  Or paste this link into your browser:<br>
                  <a href="%s" style="color: #1e3a8a; word-break: break-all;">%s</a>
                </p>
                <p style="font-size: 14px; color: #6b7280; margin-top: 32px;">
                  This link expires in 24 hours. If you didn't create an account, \
                  you can safely ignore this email.
                </p>
              </div>
            </body>
            </html>
            """.formatted(fullName, verificationLink, verificationLink, verificationLink);
    }

    /**
     * Send a status-change notification to the requester for major workflow events.
     * Returns silently for transitions that aren't user-facing (e.g., ASSIGNED, UNDER_REVIEW).
     */
    @Async("emailExecutor")
    public void sendStatusChangeEmail(FoiaRequest request, FoiaRequestStatus newStatus) {
        StatusEmailContent content = contentFor(newStatus);
        if (content == null) {
            // This transition doesn't warrant a user email. Silent skip.
            return;
        }

        String to = request.getRequester().getEmail();
        String requesterName = request.getRequester().getFullName();
        String detailLink = props.getFrontendBaseUrl() + "/requests/" + request.getId();

        String body = buildStatusChangeBody(
            requesterName,
            request.getTrackingNumber(),
            request.getSubject(),
            content,
            detailLink
        );

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

    private String buildStatusChangeBody(
        String requesterName,
        String trackingNumber,
        String requestSubject,
        StatusEmailContent content,
        String detailLink
    ) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, 'Segoe UI', Roboto, sans-serif; \
                         max-width: 560px; margin: 0 auto; padding: 24px; color: #1f2937;">
              <div style="background: #1e3a8a; color: white; padding: 16px 20px; \
                          border-radius: 8px 8px 0 0;">
                <h1 style="margin: 0; font-size: 20px;">OpenRecords FOIA Portal</h1>
              </div>
              <div style="background: white; padding: 24px 20px; border: 1px solid #e5e7eb; \
                          border-top: none; border-radius: 0 0 8px 8px;">
                <h2 style="font-size: 18px; margin-top: 0;">Hi %s,</h2>
                <p style="font-size: 16px;"><strong>%s</strong></p>
                <p>%s</p>
                <div style="background: #f9fafb; border-left: 4px solid #1e3a8a; \
                            padding: 12px 16px; margin: 24px 0; border-radius: 4px;">
                  <div style="font-size: 12px; color: #6b7280; text-transform: uppercase; \
                              letter-spacing: 0.05em;">Request</div>
                  <div style="font-family: 'Consolas', 'Monaco', monospace; \
                              font-size: 14px; font-weight: 600; margin-top: 2px;">%s</div>
                  <div style="font-size: 14px; color: #4b5563; margin-top: 4px;">%s</div>
                </div>
                <p style="text-align: center; margin: 32px 0;">
                  <a href="%s" \
                     style="background: #1e3a8a; color: white; padding: 12px 24px; \
                            text-decoration: none; border-radius: 6px; \
                            display: inline-block; font-weight: 600;">
                    View request details
                  </a>
                </p>
                <p style="font-size: 14px; color: #6b7280;">
                  Or paste this link into your browser:<br>
                  <a href="%s" style="color: #1e3a8a; word-break: break-all;">%s</a>
                </p>
              </div>
            </body>
            </html>
            """.formatted(
                requesterName,
                content.headline(),
                content.detail(),
                trackingNumber,
                requestSubject,
                detailLink,
                detailLink,
                detailLink
            );
    }
}