/**
 * Created as part of the StratusLab project (http://stratuslab.eu),
 * co-funded by the European Commission under the Grant Agreement
 * INSFO-RI-261552.
 *
 * Copyright (c) 2011
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.stratuslab.marketplace.server.cfg;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public enum Parameter {

    STORE_TYPE(true, "memory",
            "Storage type for image metadata database (memory or native)") {
        @Override
        public void validate(String value) {
            super.validate(value);
            if("mysql".equals(value) || "postgres".equals(value)){
                throw new IllegalArgumentException(getKey() +
                        " RDBMS support has been removed, please reconfigure"
                        + " the Marketplace to use store.type=native, restart, and"
                        + " execute the marketplace-rebuild-db script.");
            } else if (!("memory".equals(value) || "native".equals(value))) {
                throw new IllegalArgumentException(getKey()
                        + " must be 'memory' or 'native'");
            }
        }
    },

    COUCHBASE_BUCKET(false, "default", "Couchbase bucket name ('default')"),
    
    COUCHBASE_PASSWORD(false, "", "Couchbase password"),
    
    COUCHBASE_MARKETPLACEID(false, "", "Identifier for this Marketplace host"),
    
    COUCHBASE_URIS(false, "http://127.0.0.1:8091/pools", "List of Couchbase URIs") {
    	@Override
    	public void validate(String value) {
    		super.validate(value);
    		String[] uris = value.split("\\s+");
    		for(String uri : uris){
    			try {
    				new URI(uri);
    			} catch (URISyntaxException e) {
    				throw new IllegalArgumentException("invalid Couchbase URI: "
    						+ value, e);
    			}
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

    ENDORSER_REMINDER(false, "false", "Flag to determine if email reminder "
            + "should be sent to endorsers periodically.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isBoolean(value);
        }
    },
    
    FILESTORE_TYPE(true, "file",
    "Storage type for image metadata files (file, couchbase)") {
    	@Override
    	public void validate(String value) {
    		super.validate(value);
    		if (!("file".equals(value) || "couchbase".equals(value))) {
    			throw new IllegalArgumentException(getKey()
    					+ " must be 'file' or 'couchbase'");
    		}
    	}
    },

    GIT_URI(false, "URI of Git repository"),
    GIT_USER(false, "User credential for Git repository"),
    GIT_PASSWORD(false, "Password credential for Git repository"),
    
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

    VALIDATE_EMAIL(true, "true", "Flag to determine email must be validated.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isBoolean(value);
        }
    },

    RDBMS_DBNAME(false, "marketplace", "Database name."),

    RDBMS_HOST(false, "localhost", "RDBMS hostname."),

    RDBMS_PORT(false, "3306", "Port on for RDBMS server.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isValidPort(value);
        }
    },

    RDBMS_DBUSER(false, "sesame", "RDBMS username."),

    RDBMS_DBPASS(false, "sesame", "RDBMS password."),
    
    REPLICATION_ENABLED(false, "false", "Enable replication.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isBoolean(value);
        }
    },

    REPLICATION_TYPE(false, "public",
    "Type of replication (public, private)") {
    	@Override
    	public void validate(String value) {
    		super.validate(value);
    		if (!("public".equals(value) || "private".equals(value))) {
    			throw new IllegalArgumentException(getKey()
    					+ " must be 'public' or 'private'");
    		}
    	}
    },
    
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

    MARKETPLACE_ENDPOINT(false, "http://marketplace.stratuslab.eu", 
    		"Endpoint for this Marketplace instance.") {
    	public void validate(String value) {
            super.validate(value);
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("invalid master URL: "
                        + value, e);
            }
        }
    },
    
    MARKETPLACE_TYPE(false, "Type of Marketplace instance."),
    
    MASTER_URL(true, "URL of the master instance.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("invalid master URL: "
                        + value, e);
            }
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
                        + value, e);
            }
        }
    },

    WHITELIST_ENABLED(false, "false", "Enable endorser whitelist.") {
        @Override
        public void validate(String value) {
            super.validate(value);
            isBoolean(value);
        }
    },

    WHITELIST_LOCATION(false, "/etc/stratuslab/marketplace.whitelist",
            "Endorser whitelist location."),

    WHITELIST_TRUSTSTORE(false, "/etc/stratuslab/marketplace.truststore",
            "Marketplace truststore location."),

    WHITELIST_CRL(false, "/etc/stratuslab/marketplace.crl",
            "Location of CRLs."),
    
    WHITELIST_PASSWORD(false, "Truststore password."),
    
    STYLE_PATH(true, "/eu/stratuslab/style/css/",
            "Path for CSS and style information."),

    IMG_PATH(true, "/eu/stratuslab/style/img/", "Path for image files."),
            
    JS_PATH(true, "/eu/stratuslab/style/js/", "Path for JavaScript files.");

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
                    + " must be a positive integer: " + e.getMessage(), e);
        }
    }

    private static void isValidPort(String s) {
    	final int minPort = 1;
    	final int maxPort = 65535;
    	
        try {
            int port = Integer.parseInt(s);
            if (port < minPort || port > maxPort) {
                throw new IllegalArgumentException("invalid port number("
                        + port + ")");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
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
                    + e.getMessage(), e);
        }
    }

}
