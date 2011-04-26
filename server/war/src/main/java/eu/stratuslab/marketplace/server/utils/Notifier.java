/*
 Created as part of the StratusLab project (http://stratuslab.eu),
 co-funded by the European Commission under the Grant Agreement
 INSFO-RI-261552.

 Copyright (c) 2011, SixSq Sarl

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package eu.stratuslab.marketplace.server.utils;

import static eu.stratuslab.marketplace.server.cfg.Parameter.ADMIN_EMAIL;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MAIL_DEBUG;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MAIL_HOST;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MAIL_PASSWORD;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MAIL_PORT;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MAIL_SSL;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MAIL_USER;

import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import eu.stratuslab.marketplace.server.cfg.Configuration;

public final class Notifier {

    private static Logger logger = Logger.getLogger("org.restlet");

    private Notifier() {

    }

    public static boolean sendNotification(String email, String message)
            throws Exception {

        try {
            InternetAddress address = new InternetAddress(email);
            return sendNotification(address, message);
        } catch (AddressException e) {
            throw new Exception(e.getMessage());
        }
    }

    public static boolean sendNotification(InternetAddress email, String message) {

        boolean sendOk = true;

        try {

            InternetAddress admin = getAdminEmail();
            Session session = createSmtpSession();
            String password = getMailPassword();

            StringBuilder sb = new StringBuilder();
            sb.append("Sending notification to " + email + "\n");
            sb.append("Message: " + message + "\n");
            logger.info(sb.toString());

            Message msg = new MimeMessage(session);
            msg.setFrom(admin);

            Address[] recipients = new Address[] { email };

            msg.setRecipients(Message.RecipientType.TO, recipients);

            msg.setSubject("StratusLab Marketplace Message");

            msg.setText(message);

            msg.setHeader("X-Mailer", "javamail");
            msg.setSentDate(new Date());

            msg.saveChanges();

            // send the thing off
            try {
                Transport t = session.getTransport();
                t.connect(null, password);
                t.sendMessage(msg, msg.getAllRecipients());
                logger.info("mail was successfully sent");
            } catch (AuthenticationFailedException afe) {
                StringBuilder m = new StringBuilder();
                m.append("authentication failure\n");
                m.append(afe.getMessage() + "\n");
                logger.severe(m.toString());
                sendOk = false;
            } catch (MessagingException me) {
                StringBuilder m = new StringBuilder();
                m.append("error sending message to " + email + "\n");
                m.append(me.getMessage() + "\n");
                logger.severe(m.toString());
                sendOk = false;
            }

        } catch (MessagingException consumed) {
            // FIXME: Is this logic actually correct?
            consumed.printStackTrace();
            sendOk = false;

        } finally {

        }
        return sendOk;
    }

    private static InternetAddress getAdminEmail() {

        try {

            String adminEmail = Configuration.getParameterValue(ADMIN_EMAIL);
            return new InternetAddress(adminEmail);

        } catch (NullPointerException e) {
            throw new RuntimeException(
                    "administrator email undefined or invalid", e);
        } catch (AddressException e) {
            throw new RuntimeException(
                    "administrator email undefined or invalid", e);
        }

    }

    private static String getMailPassword() {

        String value = Configuration.getParameterValue(MAIL_PASSWORD);
        return (value != null) ? value : "";

    }

    private static Session createSmtpSession() {

        // Retrieve a copy of the system properties as a baseline for
        // setting the mail properties.
        Properties props = System.getProperties();

        // Force authentication for the SMTP server to be used.
        props.put("mail.smtp.auth", "true");

        // Turn on the use of TLS if it is available.
        props.put("mail.smtp.starttls.enable", "true");

        // Determine whether or not to use SSL. By default, SSL is not used
        // when contacting the SMTP server.
        String useSSL = Configuration.getParameterValue(MAIL_SSL);
        String protocol = Boolean.parseBoolean(useSSL) ? "smtps" : "smtp";

        props.put("mail.transport.protocol", protocol);

        // Set the SMTP server parameters. The host name is required; the
        // port is optional.
        String mailHost = Configuration.getParameterValue(MAIL_HOST);

        props.put("mail." + protocol + ".host", mailHost);

        String mailPort = Configuration.getParameterValue(MAIL_PORT);
        props.put("mail." + protocol + ".port", mailPort);

        // Set the name of the user on the SMTP server. This must be
        // specified.
        String mailUser = Configuration.getParameterValue(MAIL_USER);
        props.put("mail." + protocol + ".user", mailUser);

        // Determine whether or not the debugging should be enabled for java
        // mail. If the option isn't given, then debugging will be off.
        String mailDebug = Configuration.getParameterValue(MAIL_DEBUG);
        props.put("mail.debug", Boolean.parseBoolean(mailDebug));

        // Create the session object for later use.
        Session session = Session.getInstance(props);
        session.setDebug(true);

        return session;

    }

}
