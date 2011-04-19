package eu.stratuslab.marketplace.server.cfg;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public enum Parameter {

    STORE_TYPE(true, "memory",
            "Storage type for image metadata database (memory or mysql)") {
        @Override
        public void validate(String value) {
            super.validate(value);
            if (!("memory".equals(value) || "mysql".equals(value))) {
                throw new IllegalArgumentException(getKey()
                        + " must be 'memory' or 'mysql'");
            }
        }
    },

    DATA_DIR(true, "/var/lib/stratuslab/metadata",
            "Directory containing raw metadata data entries.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            File datadir = new File(value);
            if (!datadir.isAbsolute()) {
                throw new IllegalArgumentException(getKey()
                        + " must be an absolute directory");
            }
        }
    },

    PENDING_DIR(true, "/var/lib/stratuslab/pending",
            "Directory for pending (unconfirmed) entries.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            File datadir = new File(value);
            if (!datadir.isAbsolute()) {
                throw new IllegalArgumentException(getKey()
                        + " must be an absolute directory");
            }
        }
    },

    METADATA_MAX_BYTES(true, "10240000",
            "Size limit for a single metadata entry.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isPositiveInt(getKey(), value);
        }
    },

    TIME_RANGE(true, "10", "Directory containing raw metadata data entries.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isPositiveInt(getKey(), value);
        }
    },

    VALIDATE_EMAIL(true, "true", "Flag to determine email must be validated.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isBoolean(value);
        }
    },

    MYSQL_DBNAME(false, "marketplace", "MySQL database name."),

    MYSQL_HOST(false, "localhost", "MySQL hostname."),

    MYSQL_PORT(false, "3306", "Port on for MySQL server.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isValidPort(value);
        }
    },

    MYSQL_DBUSER(false, "sesame", "MySQL username."),

    MYSQL_DBPASS(false, "sesame", "MySQL password."),

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

    MAIL_SSL(false, "Use SSL for SMTP server (default is 'true').") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isBoolean(value);
        }
    },

    MAIL_DEBUG(false, "Debug mail sending (default is 'false').") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isBoolean(value);
        }
    },

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

    private final String key;
    private final boolean required;
    private final String description;
    private final String defaultValue;

    private Parameter(boolean required, String description) {
        this(required, null, description);
    }

    private Parameter(boolean required, String defaultValue, String description) {
        this.key = nameToKey(this.name());
        this.required = required;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getProperty(Properties properties) {
        String value = (String) properties.get(key);
        return (value != null) ? value : defaultValue;
    }

    public void validate(String value) {
        if (isRequired()) {
            if (value == null || "".equals(value)) {
                throw new IllegalArgumentException("required parameter ("
                        + getKey() + ") is not defined");
            }
        }
    }

    public static Parameter parameterFromKey(Object key) {
        String skey = key.toString().toUpperCase().replace('.', '_');
        return Parameter.valueOf(skey);
    }

    public static String nameToKey(String name) {
        return name.toLowerCase().replace('_', '.');
    }

    private static void isBoolean(String s) {
        if (!("true".equals(s) || "false".equals(s))) {
            throw new IllegalArgumentException(
                    "value must be 'true' or 'false'");
        }
    }

    private static void isPositiveInt(String key, String s) {
        try {
            int i = Integer.parseInt(s);
            if (i <= 0) {
                throw new IllegalArgumentException(key
                        + " must be a positive integer");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key
                    + " must be a positive integer: " + e.getMessage());
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
            if (!Pattern.matches(".+@.+", s)) {
                throw new IllegalArgumentException("invalid email address: "
                        + s);

            }
        } catch (AddressException e) {
            throw new IllegalArgumentException("invalid email address: "
                    + e.getMessage());
        }
    }

}