/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
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

package net.openhft.chronicle.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author lburgazzoli
 *
 * Configurationn example:
 *
 * # default
 * chronicle.logger.base = ${java.io.tmpdir}/chronicle/${today}/${pid}
 *
 * # logger : root
 * chronicle.logger.root.type      = vanilla
 * chronicle.logger.root.path      = ${chronicle.logger.base}/root
 * chronicle.logger.root.level     = debug
 * chronicle.logger.root.shortName = false
 * chronicle.logger.root.append    = false
 * chronicle.logger.root.format    = binary
 * chronicle.logger.root.serialize = false
 *
 * # logger : Logger1
 * chronicle.logger.Logger1.path = ${chronicle.logger.base}/logger_1
 * chronicle.logger.Logger1.level = info
 */
public class ChronicleLogConfig {
    public static final String KEY_PROPERTIES_FILE = "chronicle.logger.properties";
    public static final String KEY_PREFIX = "chronicle.logger.";
    public static final String KEY_PREFIX_ROOT = "chronicle.logger.root.";
    public static final String KEY_CFG_PREFIX = "chronicle.logger.root.cfg.";
    public static final String KEY_CHRONICLE_TYPE = "chronicle.logger.root.type";
    public static final String KEY_LEVEL = "level";
    public static final String KEY_PATH = "path";
    public static final String KEY_SHORTNAME = "shortName";
    public static final String KEY_APPEND = "append";
    public static final String KEY_FORMAT = "format";
    public static final String KEY_TYPE = "type";
    public static final String KEY_DATE_FORMAT = "dateFormat";
    public static final String KEY_STACK_TRACE_DEPTH = "stackTraceDepth";
    public static final String FORMAT_BINARY = "binary";
    public static final String FORMAT_TEXT = "text";
    public static final String BINARY_MODE_FORMATTED = "formatted";
    public static final String BINARY_MODE_SERIALIZED = "serialized";
    public static final String PLACEHOLDER_START = "${";
    public static final String PLACEHOLDER_END = "}";
    public static final String PLACEHOLDER_TODAY = "${today}";
    public static final String PLACEHOLDER_TODAY_FORMAT = "yyyyMMdd";
    public static final String PLACEHOLDER_PID = "${pid}";
    public static final String DEFAULT_DATE_FORMAT = "yyyy.MM.dd-HH:mm:ss.SSS";

    public static final List<String> DEFAULT_CFG_LOCATIONS = Arrays.asList(
        "chronicle-logger.properties",
        "config/chronicle-logger.properties"
    );

    public static final List<String> PACKAGE_MASK = Arrays.asList(
        "net.openhft"
    );

    private static final DateFormat DATEFORMAT = new SimpleDateFormat(PLACEHOLDER_TODAY_FORMAT);
    private static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

    private final Properties properties;
    private final ChronicleLogAppenderConfig appenderConfig;

    /**
     * @param properties
     */
    private ChronicleLogConfig(final Properties properties, final ChronicleLogAppenderConfig appenderConfig) {
        this.properties = properties;
        this.appenderConfig = appenderConfig;
    }

    // *************************************************************************
    //
    // *************************************************************************

    public static ChronicleLogConfig load(final Properties properties) {
        final ChronicleLogAppenderConfig cfg = new ChronicleLogAppenderConfig();
        cfg.setProperties(properties, KEY_CFG_PREFIX);

        return new ChronicleLogConfig(properties, cfg);
    }

    /**
     * @param cfgPath   the configuration path
     * @return          the configuration object
     */
    public static ChronicleLogConfig load(String cfgPath) {
        try {
            return load(getConfigurationStream(cfgPath));
        } catch (Exception e) {
            // is printing stack trace and falling through really the right thing
            // to do here, or should it throw out?
            e.printStackTrace();
        }

        return null;
    }

    public static ChronicleLogConfig load(InputStream in) {
        if (in != null) {
            Properties properties = new Properties();

            try {
                properties.load(in);
                in.close();
            } catch (IOException ignored) {
            }

            interpolate(properties);
            return load(properties);

        } else {
            System.err.printf(
                "Unable to configure chronicle-logger:"
                + " configuration file not found in default locations (%s)"
                + " or System property (%s) is not defined \n",
                DEFAULT_CFG_LOCATIONS.toString(),
                KEY_PROPERTIES_FILE);
        }

        return null;
    }

    /**
     * @return  the configuration object
     */
    public static ChronicleLogConfig load() {
        try {
            InputStream is = getConfigurationStream(System.getProperty(KEY_PROPERTIES_FILE));
            if(is == null) {
                for(String location : DEFAULT_CFG_LOCATIONS) {
                    is = getConfigurationStream(location);
                    if(is != null) {
                        break;
                    }
                }
            }

            if(is != null) {
                return load(is);
            }
        } catch (Exception e) {
            // is printing stack trace and falling through really the right thing
            // to do here, or should it throw out?
            e.printStackTrace();
        }

        return null;
    }

    protected static InputStream getConfigurationStream(String cfgPath) throws IOException {
        if(cfgPath != null) {
            final File cfgFile = new File(cfgPath);
            if (!cfgFile.exists()) {
                return Thread.currentThread().getContextClassLoader().getResourceAsStream(cfgPath);

            } else if (cfgFile.canRead()) {
                return new FileInputStream(cfgFile);
            }
        }

        return null;
    }

    // *************************************************************************
    //
    // *************************************************************************

    /**
     * @param tmpProperties
     */
    private static Properties interpolate(final Properties tmpProperties) {
        int amended = 0;
        do {
            amended = 0;
            for (Map.Entry<Object, Object> entries : tmpProperties.entrySet()) {
                String val = tmpProperties.getProperty((String) entries.getKey());
                val = val.replace(PLACEHOLDER_TODAY, DATEFORMAT.format(new Date()));
                val = val.replace(PLACEHOLDER_PID, PID);

                int startIndex = 0;
                int endIndex = 0;

                do {
                    startIndex = val.indexOf(PLACEHOLDER_START, endIndex);
                    if (startIndex != -1) {
                        endIndex = val.indexOf(PLACEHOLDER_END, startIndex);
                        if (endIndex != -1) {
                            String envKey = val.substring(startIndex + 2, endIndex);
                            String newVal = null;
                            if (tmpProperties.containsKey(envKey)) {
                                newVal = tmpProperties.getProperty(envKey);

                            } else if (System.getProperties().containsKey(envKey)) {
                                newVal = System.getProperties().getProperty(envKey);
                            }

                            if (newVal != null) {
                                val = val.replace(PLACEHOLDER_START + envKey + PLACEHOLDER_END, newVal);
                                endIndex += newVal.length() - envKey.length() + 3;

                                amended++;
                            }
                        }
                    }
                } while (startIndex != -1 && endIndex != -1 && endIndex < val.length());

                entries.setValue(val);
            }
        } while (amended > 0);

        return tmpProperties;
    }

    // *************************************************************************
    //
    // *************************************************************************

    /**
     *
     * @return  the Appender configuration
     */
    public ChronicleLogAppenderConfig getAppenderConfig() {
        return this.appenderConfig;
    }

    /**
     * @param loggerName
     * @return
     */
    public String getString(final String loggerName) {
        String name = KEY_PREFIX_ROOT + loggerName;
        return this.properties.getProperty(name);
    }

    /**
     * @param shortName
     * @return
     */
    public String getString(final String loggerName, final String shortName) {
        String key = KEY_PREFIX  + loggerName + "." + shortName;
        String val = this.properties.getProperty(key);

        if (val == null) {
            val = getString(shortName);
        }

        return val;
    }

    /**
     * @param shortName
     * @return
     */
    public Boolean getBoolean(final String shortName) {
        String prop = getString(shortName);
        return (prop != null) ? "true".equalsIgnoreCase(prop) : null;
    }

    /**
     * @param shortName
     * @return
     */
    public Boolean getBoolean(final String shortName, boolean defval) {
        String prop = getString(shortName);
        return (prop != null) ? "true".equalsIgnoreCase(prop) : defval;
    }

    /**
     * @param shortName
     * @return
     */
    public Boolean getBoolean(final String loggerName, final String shortName) {
        String prop = getString(loggerName, shortName);
        return (prop != null) ? "true".equalsIgnoreCase(prop) : null;
    }

    /**
     * @param loggerName
     * @param shortName
     * @param defval
     * @return
     */
    public Boolean getBoolean(final String loggerName, final String shortName, boolean defval) {
        String prop = getString(loggerName, shortName);
        return (prop != null) ? "true".equalsIgnoreCase(prop) : defval;
    }

    /**
     * @param shortName
     * @return
     */
    public Integer getInteger(final String shortName) {
        String prop = getString(shortName);
        return (prop != null) ? Integer.parseInt(prop) : null;
    }

    /**
     * @param loggerName
     * @param shortName
     * @return
     */
    public Integer getInteger(final String loggerName, final String shortName) {
        String prop = getString(loggerName, shortName);
        return (prop != null) ? Integer.parseInt(prop) : null;
    }

    /**
     * @param shortName
     * @return
     */
    public Long getLong(final String shortName) {
        String prop = getString(shortName);
        return (prop != null) ? Long.parseLong(prop) : null;
    }

    /**
     * @param loggerName
     * @param shortName
     * @return
     */
    public Long getLong(final String loggerName, final String shortName) {
        String prop = getString(loggerName, shortName);
        return (prop != null) ? Long.parseLong(prop) : null;
    }

    /**
     * @param shortName
     * @return
     */
    public Double getDouble(final String shortName) {
        String prop = getString(shortName);
        return (prop != null) ? Double.parseDouble(prop) : null;
    }

    /**
     * @param loggerName
     * @param shortName
     * @return
     */
    public Double getDouble(final String loggerName, final String shortName) {
        String prop = getString(loggerName, shortName);
        return (prop != null) ? Double.parseDouble(prop) : null;
    }

    /**
     * @param shortName
     * @return
     */
    public Short getShort(final String shortName) {
        String prop = getString(shortName);
        return (prop != null) ? Short.parseShort(prop) : null;
    }

    /**
     * @param loggerName
     * @param shortName
     * @return
     */
    public Short getShort(final String loggerName, final String shortName) {
        String prop = getString(loggerName, shortName);
        return (prop != null) ? Short.parseShort(prop) : null;
    }

    /**
     * @param loggerName
     * @return
     */
    public ChronicleLogLevel getLevel(final String loggerName) {
        return getLevel(loggerName, null);
    }

    /**
     * @param loggerName
     * @return
     */
    public ChronicleLogLevel getLevel(final String loggerName, ChronicleLogLevel defVal) {
        String prop = getString(loggerName, KEY_LEVEL);
        return (prop != null) ? ChronicleLogLevel.fromStringLevel(prop) : defVal;
    }
}
