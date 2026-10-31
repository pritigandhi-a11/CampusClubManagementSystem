package com.campusclub.util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;

/**
 * Service to manage SMTP email delivery for student identity OTP verification.
 * Loads configuration dynamically from environment variables or .env file.
 * Credentials are NEVER hardcoded.
 */
public final class EmailService {

    private static String smtpHost;
    private static int smtpPort = 587;
    private static String smtpUsername;
    private static String smtpPassword;
    private static String smtpFrom = "noreply@klu.ac.in";
    private static boolean smtpAuth = true;
    private static boolean smtpStartTls = true;

    static {
        loadConfiguration();
    }

    private EmailService() {
        // Utility class
    }

    public static synchronized void loadConfiguration() {
        // 1. Read from git-ignored .env file if present
        File envFile = resolveEnvFile();
        if (envFile != null && envFile.exists() && envFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String k = line.substring(0, idx).trim();
                        String v = line.substring(idx + 1).trim();
                        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                            v = v.substring(1, v.length() - 1);
                        }
                        applyEnvConfig(k, v);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. System environment variables override .env
        for (String k : new String[]{"SMTP_HOST", "SMTP_PORT", "SMTP_USERNAME", "SMTP_PASSWORD", "SMTP_FROM", "SMTP_AUTH", "SMTP_STARTTLS"}) {
            String v = System.getenv(k);
            if (v != null && !v.trim().isEmpty()) {
                applyEnvConfig(k, v.trim());
            }
        }
    }

    private static File resolveEnvFile() {
        File[] candidates = new File[] {
            new File(".env"),
            new File(System.getProperty("user.dir", "."), ".env"),
            new File("c:/Users/Preeti gandhi/Desktop/CampusClubManagementSystem/.env"),
            new File(System.getProperty("user.home", ""), "Desktop/CampusClubManagementSystem/.env")
        };
        for (File candidate : candidates) {
            if (candidate != null && candidate.exists() && candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    private static void applyEnvConfig(String key, String val) {
        if ("SMTP_HOST".equalsIgnoreCase(key)) {
            smtpHost = val;
        } else if ("SMTP_PORT".equalsIgnoreCase(key)) {
            try { smtpPort = Integer.parseInt(val); } catch (NumberFormatException ignored) {}
        } else if ("SMTP_USERNAME".equalsIgnoreCase(key) || "SMTP_USER".equalsIgnoreCase(key) || "MAIL_USERNAME".equalsIgnoreCase(key)) {
            smtpUsername = val;
        } else if ("SMTP_PASSWORD".equalsIgnoreCase(key) || "SMTP_PASS".equalsIgnoreCase(key) || "MAIL_PASSWORD".equalsIgnoreCase(key)) {
            String p = val.trim();
            // Google App Passwords copied with 4-character grouping spaces (e.g. "abcd efgh ijkl mnop")
            if (p.contains(" ") && p.replace(" ", "").length() == 16) {
                p = p.replace(" ", "");
            }
            smtpPassword = p;
        } else if ("SMTP_FROM".equalsIgnoreCase(key) || "MAIL_FROM".equalsIgnoreCase(key)) {
            smtpFrom = val;
        } else if ("SMTP_AUTH".equalsIgnoreCase(key)) {
            smtpAuth = Boolean.parseBoolean(val);
        } else if ("SMTP_STARTTLS".equalsIgnoreCase(key) || "SMTP_TLS".equalsIgnoreCase(key)) {
            smtpStartTls = Boolean.parseBoolean(val);
        }
    }

    /**
     * Checks whether valid SMTP credentials have been configured.
     */
    public static boolean isConfigured() {
        return smtpHost != null && !smtpHost.trim().isEmpty()
                && smtpUsername != null && !smtpUsername.trim().isEmpty()
                && smtpPassword != null && !smtpPassword.trim().isEmpty();
    }

    public static String getSmtpHost() {
        return smtpHost;
    }

    public static int getSmtpPort() {
        return smtpPort;
    }

    public static String getSmtpUsername() {
        return smtpUsername;
    }

    public static String getSmtpFrom() {
        return smtpFrom;
    }

    /**
     * Verifies connection and authentication against the SMTP server without sending an email.
     *
     * @return true if successfully connected and authenticated, false otherwise
     */
    public static boolean testConnection() {
        if (!isConfigured()) {
            return false;
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", String.valueOf(smtpAuth));
            props.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTls));
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            if (smtpHost != null && !smtpHost.trim().isEmpty()) {
                props.put("mail.smtp.ssl.trust", smtpHost);
            }
            props.put("mail.smtp.connectiontimeout", "8000");
            props.put("mail.smtp.timeout", "8000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });

            try (Transport transport = session.getTransport("smtp")) {
                transport.connect(smtpHost, smtpPort, smtpUsername, smtpPassword);
                return transport.isConnected();
            }
        } catch (Exception e) {
            System.err.println("[EmailService] SMTP connection test notice: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends a 6-digit OTP email to the student's institutional email address.
     *
     * @param recipientEmail student's institutional email (e.g. 99527800392@klu.ac.in)
     * @param otpCode 6-digit OTP verification code
     * @param expiryMinutes number of minutes before code expires (default 10)
     * @return true if successfully delivered via SMTP, false if unconfigured or error
     */
    public static boolean sendOtpEmail(String recipientEmail, String otpCode, int expiryMinutes) {
        if (!isConfigured()) {
            return false;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", String.valueOf(smtpAuth));
            props.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTls));
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            if (smtpHost != null && !smtpHost.trim().isEmpty()) {
                props.put("mail.smtp.ssl.trust", smtpHost);
            }
            props.put("mail.smtp.connectiontimeout", "8000");
            props.put("mail.smtp.timeout", "8000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpFrom, "Campus Club Management System"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Kalasalingam University Campus Club - Verification Code", "UTF-8");

            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;\">" +
                    "<h2 style=\"color: #1e3a8a; border-bottom: 2px solid #3b82f6; padding-bottom: 8px;\">Kalasalingam Academy of Research and Education</h2>" +
                    "<p>Hello Student,</p>" +
                    "<p>Thank you for registering on the <strong>Campus Club Management System (CCMS)</strong> portal. Please use the following One-Time Password (OTP) to verify your institutional student identity:</p>" +
                    "<div style=\"background-color: #f1f5f9; padding: 16px; border-radius: 6px; text-align: center; margin: 20px 0;\">" +
                    "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #1d4ed8;\">" + otpCode + "</span>" +
                    "</div>" +
                    "<p style=\"color: #475569;\">This verification code is strictly valid for <strong>" + expiryMinutes + " minutes</strong>. Do not share this OTP with anyone.</p>" +
                    "<p style=\"color: #64748b; font-size: 13px; margin-top: 24px; border-top: 1px solid #e2e8f0; padding-top: 12px;\">" +
                    "If you did not initiate this registration with your @klu.ac.in email, please disregard this email or notify the campus administrator.</p>" +
                    "</div>";

            message.setContent(html, "text/html; charset=UTF-8");

            Transport.send(message);
            return true;
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to dispatch OTP to " + recipientEmail + ": " + e.getMessage());
            return false;
        }
    }
}
