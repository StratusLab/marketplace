package eu.stratuslab.marketplace.server.cfg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Configuration {

    private static final String configFileName = "marketplace.cfg";

    private static final Properties properties;

    static {
        List<File> configFileLocations = getConfigurationFileLocations();
        properties = getConfigurationProperties(configFileLocations);
    }

    private Configuration() {

    }

    private static List<File> getConfigurationFileLocations() {

        ArrayList<File> locations = new ArrayList<File>();

        // Possible locations for the configuration file are the current working
        // directory, the user's home directory, or the standard system
        // location, in that order.
        File[] dirs = { new File(System.getProperty("user.dir")),
                new File(System.getProperty("user.home")),
                new File("/etc/stratuslab/") };

        for (File dir : dirs) {
            locations.add(new File(dir, configFileName));
        }

        return Collections.unmodifiableList(locations);
    }

    private static Properties getConfigurationProperties(
            List<File> configFileLocations) {

        for (File f : configFileLocations) {
            if (f.canRead()) {
                Properties properties = loadProperties(f);
                validateConfiguration(properties);
                return properties;
            }
        }
        throw new RuntimeException("cannot locate configuration file");
    }

    private static Properties loadProperties(File configFile) {

        Properties properties = new Properties();

        try {
            Reader reader = new FileReader(configFile);
            try {
                properties.load(reader);
            } catch (IOException consumed) {
                // TODO: Add logging.
            } finally {
                try {
                    reader.close();
                } catch (IOException consumed) {
                    // TODO: Add logging.
                }
            }
        } catch (FileNotFoundException consumed) {
            // Return empty properties file.
        }

        return properties;
    }

    public static String getParameterValue(Parameter parameter) {
        return parameter.getProperty(properties);
    }

    public static boolean getParameterValueAsBoolean(Parameter parameter) {
        return Boolean.parseBoolean(parameter.getProperty(properties));
    }

    public static int getParameterValueAsInt(Parameter parameter) {
        return Integer.parseInt(parameter.getProperty(properties));
    }

    public static long getParameterValueAsLong(Parameter parameter) {
        return Long.parseLong(parameter.getProperty(properties));
    }

    public static File getParameterValueAsFile(Parameter parameter) {
        return new File(parameter.getProperty(properties));
    }

    private static void validateConfiguration(Properties properties) {
        checkAllParametersAreValid(properties);
        checkAllParametersAreKnown(properties);
    }

    private static void checkAllParametersAreValid(Properties properties) {
        for (Parameter p : Parameter.values()) {
            String value = p.getProperty(properties);
            if (value != null) {
                p.validate(value);
            }
        }
    }

    private static void checkAllParametersAreKnown(Properties properties) {
        for (Object key : properties.keySet()) {
            Parameter.parameterFromKey(key);
        }
    }

}
