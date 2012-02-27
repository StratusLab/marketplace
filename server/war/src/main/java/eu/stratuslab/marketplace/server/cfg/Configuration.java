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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class Configuration {

    private static final String CONFIG_FILENAME = "marketplace.cfg";

    private static final Properties PROPERTIES;

    static {
        List<File> configFileLocations = getConfigurationFileLocations();
        PROPERTIES = getConfigurationProperties(configFileLocations);
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
            locations.add(new File(dir, CONFIG_FILENAME));
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
            Reader reader = new InputStreamReader(
            		new FileInputStream(configFile), "UTF-8");
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
        } catch (UnsupportedEncodingException e) {
        	// Return empty properties file.
        }

        return properties;
    }

    public static String getParameterValue(Parameter parameter) {
        return parameter.getProperty(PROPERTIES);
    }

    public static boolean getParameterValueAsBoolean(Parameter parameter) {
        return Boolean.parseBoolean(parameter.getProperty(PROPERTIES));
    }

    public static int getParameterValueAsInt(Parameter parameter) {
        return Integer.parseInt(parameter.getProperty(PROPERTIES));
    }

    public static long getParameterValueAsLong(Parameter parameter) {
        return Long.parseLong(parameter.getProperty(PROPERTIES));
    }

    public static File getParameterValueAsFile(Parameter parameter) {
        return new File(parameter.getProperty(PROPERTIES));
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
