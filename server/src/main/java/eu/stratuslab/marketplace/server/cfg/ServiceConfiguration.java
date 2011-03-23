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
package eu.stratuslab.marketplace.server.cfg;

import java.net.MalformedURLException;
import java.net.URL;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class ServiceConfiguration {

    public enum AllowedParameter {

        ADMIN_EMAIL(true, "Email address for account approvals, etc.") {
            @Override
            public void validate(String value) {
                super.validate(value);
                isValidEmail(value);
            }
        },

        MAIL_HOST(true, "Host for SMTP server for email notifications."),

        MAIL_PORT(false, "Port on SMTP server (defaults to standard ports).") {
            @Override
            public void validate(String value) {
                super.validate(value);
                isValidPort(value);
            }
        },

        MAIL_USER(true, "Username for SMTP server."),

        MAIL_PASSWORD(false, "Password for SMTP server."),

        MAIL_SSL(false, "Use SSL for SMTP server (default is 'true')."),

        MAIL_DEBUG(false, "Debug mail sending (default is 'false')."),

        SUPPORT_EMAIL(true, "Email address for support requests.") {
            @Override
            public void validate(String value) {
                super.validate(value);
                isValidEmail(value);
            }
        },

        SUPPORT_URL(true, "URL where support can be obtained.") {
            @Override
            public void validate(String value) {
                super.validate(value);
                try {
                    new URL(value);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("invalid support URL: "
                            + value);
                }
            }
        };

        private final boolean required;
        private final String description;

        private AllowedParameter(boolean required, String description) {
            this.required = required;
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public String getDescription() {
            return description;
        }

        public void validate(String value) {
            if (value == null || "".equals(value)) {
                throw new IllegalArgumentException(
                        "value cannot be empty or null");
            }
        }

        private static void isValidPort(String s) {
            try {
                int port = Integer.parseInt(s);
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("invalid port number("
                            + port + ")");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        private static void isValidEmail(String s) {
            try {
                new InternetAddress(s);
            } catch (AddressException e) {
                throw new IllegalArgumentException("invalid email address: "
                        + e.getMessage());
            }
        }

    }

    public ServiceParameter getParameter(String key) {
        // FIXME: Must return reasonable value.
        return null;
    }

    public static ServiceConfiguration load() {
        // FIXME: Must return reasonable value.
        return (ServiceConfiguration) null;
    }

}
