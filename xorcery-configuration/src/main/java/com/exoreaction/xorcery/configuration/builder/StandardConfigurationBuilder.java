package com.exoreaction.xorcery.configuration.builder;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.function.Consumer;

public class StandardConfigurationBuilder {

    private static final Logger logger = LogManager.getLogger(Configuration.class);

    private static final YAMLMapper yamlMapper = new YAMLMapper();
    private static final JavaPropsMapper javaPropsMapper = new JavaPropsMapper();

    public StandardConfigurationBuilder() {
    }

    public void addDefaults(Configuration.Builder builder)
            throws UncheckedIOException {

        addXorceryDefaults(builder);
        addExtensions(builder);
        addApplication(builder);

        addUserDirectory(builder);
        addHome(builder);

        addSystemProperties(builder);
        addEnvironmentVariables(builder);
    }


    public void addTestDefaults(Configuration.Builder builder)
            throws UncheckedIOException {

        // First add the standard configuration
        addXorceryDefaults(builder);
        addExtensions(builder);
        addApplication(builder);

        // Then add the test configuration on top
        addXorceryTestDefaults(builder);
        addExtensionsTest(builder);
        addApplicationTest(builder);

        // Then any custom and environment configuration
        addUserDirectory(builder);

        addSystemProperties(builder);
        addEnvironmentVariables(builder);
    }

    public Consumer<Configuration.Builder> addTestDefaultsWithYaml(String yamlString)
            throws UncheckedIOException {
        return builder ->
        {
            // First add the standard configuration
            addXorceryDefaults(builder);
            addExtensions(builder);
            addApplication(builder);

            // Then add the test configuration on top
            addXorceryTestDefaults(builder);
            addExtensionsTest(builder);
            addApplicationTest(builder);

            // Then any custom and environment configuration
            addUserDirectory(builder);

            addSystemProperties(builder);
            addEnvironmentVariables(builder);

            addYaml(yamlString).accept(builder);
        };
    }


    public void addSystemProperties(Configuration.Builder builder) {
        builder.addSystemProperties("SYSTEM");
        logger.info("Loaded system properties");
    }

    public void addEnvironmentVariables(Configuration.Builder builder) {
        builder.addEnvironmentVariables("ENV");
        logger.info("Loaded environment variables");
    }

    public void addXorceryDefaults(Configuration.Builder builder) throws UncheckedIOException {
        // Load Xorcery defaults
        URL resource = Resources.getResource("META-INF/xorcery-defaults.yaml")
                .orElseThrow(() -> new UncheckedIOException(new IOException("Resource not found: META-INF/xorcery-defaults.yaml")));
        try (InputStream in = resource.openStream()) {
            addYaml(in).accept(builder);
            logger.info("Loaded " + resource);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void addExtensions(Configuration.Builder builder) throws UncheckedIOException {
        // Load extensions
        for (URL resource : Resources.getResources("META-INF/xorcery.yaml")) {
            try (InputStream configurationStream = resource.openStream()) {
                addYaml(configurationStream).accept(builder);
                logger.info("Loaded " + resource);
            } catch (IOException ex) {
                throw new UncheckedIOException("Error loading configuration file:" + resource.toExternalForm(), ex);
            } catch (UncheckedIOException ex) {
                throw new UncheckedIOException("Error loading configuration file:" + resource.toExternalForm(), ex.getCause());
            }
        }
    }

    public void addExtensionsTest(Configuration.Builder builder) throws UncheckedIOException {
        // Load extensions
        try {
            for (URL resource : Resources.getResources("META-INF/xorcery-test.yaml")) {
                try (InputStream configurationStream = resource.openStream()) {
                    addYaml(configurationStream).accept(builder);
                    logger.info("Loaded " + resource);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addApplication(Configuration.Builder builder) throws UncheckedIOException {
        // Load application config file
        try {
            for (URL resource : Resources.getResources("xorcery.yaml")) {
                try (InputStream configurationStream = resource.openStream()) {
                    addYaml(configurationStream).accept(builder);
                    logger.info("Loaded " + resource);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addApplicationTest(Configuration.Builder builder) throws UncheckedIOException {
        // Load application config file
        try {
            for (URL resource : Resources.getResources("xorcery-test.yaml")) {
                try (InputStream configurationStream = resource.openStream()) {
                    addYaml(configurationStream).accept(builder);
                    logger.info("Loaded " + resource);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addUserDirectory(Configuration.Builder builder) throws UncheckedIOException {
        // Load user directory overrides
        try {
            File overridesYamlFile = new File(System.getProperty("user.dir"), "xorcery.yaml");
            if (overridesYamlFile.exists()) {
                FileInputStream overridesYamlStream = new FileInputStream(overridesYamlFile);
                addYaml(overridesYamlStream).accept(builder);
                logger.info("Loaded " + overridesYamlFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addHome(Configuration.Builder builder) throws UncheckedIOException {
        // Load user home overrides
        try {
            File userYamlFile = new File(System.getProperty("user.home"), "xorcery/xorcery.yaml");
            if (userYamlFile.exists()) {
                FileInputStream userYamlStream = new FileInputStream(userYamlFile);
                addYaml(userYamlStream).accept(builder);
                logger.info("Loaded " + userYamlFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Consumer<Configuration.Builder> addFile(File configFile) {
        return builder ->
        {
            // Load specified overrides
            if (configFile != null) {
                try {
                    if (configFile.getName().endsWith("yaml") || configFile.getName().endsWith("yml")) {
                        addYaml(new FileInputStream(configFile)).accept(builder);
                        logger.info("Loaded " + configFile);
                    } else if (configFile.getName().endsWith("properties")) {
                        addProperties(new FileInputStream(configFile)).accept(builder);
                        logger.info("Loaded " + configFile);
                    } else {
                        logger.warn("Unknown configuration filetype: " + configFile);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    public void addXorceryTestDefaults(Configuration.Builder builder) throws UncheckedIOException {
        // Load Xorcery defaults
        Resources.getResource("META-INF/xorcery-defaults-test.yaml").ifPresent(resource ->
        {
            try (InputStream in = resource.openStream()) {
                addYaml(in).accept(builder);
                logger.info("Loaded " + resource);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public Consumer<Configuration.Builder> addYaml(InputStream yamlStream) throws UncheckedIOException {
        return builder ->
        {
            try (yamlStream) {
                ObjectNode yaml = (ObjectNode) yamlMapper.readTree(yamlStream);
                new JsonMerger().merge(builder.builder(), yaml);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };
    }

    public Consumer<Configuration.Builder> addYaml(String yamlString) throws UncheckedIOException {
        return builder ->
        {
            try {
                ObjectNode yaml = (ObjectNode) yamlMapper.readTree(yamlString);
                new JsonMerger().merge(builder.builder(), yaml);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    public Consumer<Configuration.Builder> addProperties(InputStream propertiesStream) throws UncheckedIOException {
        return builder ->
        {
            try (propertiesStream) {
                ObjectNode properties = (ObjectNode) javaPropsMapper.readTree(propertiesStream);
                new JsonMerger().merge(builder.builder(), properties);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };
    }

    public Consumer<Configuration.Builder> addProperties(String propertiesString) throws UncheckedIOException {
        return builder ->
        {
            try {
                ObjectNode properties = (ObjectNode) javaPropsMapper.readTree(propertiesString);
                new JsonMerger().merge(builder.builder(), properties);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }


    public static String toYaml(Configuration.Builder builder) {
        ObjectWriter objectWriter = yamlMapper.writer().withDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(builder.builder());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String toYaml(Configuration configuration) {
        ObjectWriter objectWriter = yamlMapper.writer().withDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(configuration.json());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
