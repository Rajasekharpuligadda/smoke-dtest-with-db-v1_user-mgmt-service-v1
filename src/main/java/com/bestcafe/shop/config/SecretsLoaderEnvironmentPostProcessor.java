package com.bestcafe.shop.config;

import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class SecretsLoaderEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SECRETS_DIRECTORY_PROPERTY = "secrets.directory.path";
    private static final String SECRETS_PROVIDER_ENABLED_PROPERTY = "secrets.provider.enabled";
    private static final String SECRETS_PROPERTIES_SOURCE_NAME = "mountedSecretsProperties";
    private final DeferredLog logger;

    public SecretsLoaderEnvironmentPostProcessor() {
        this.logger=new DeferredLog();
    }

    @Override
    public void postProcessEnvironment(

            final ConfigurableEnvironment environment,
            final SpringApplication application) {

        logger.info("Started Loading SecretsLoaderEnvironmentPostProcessor");
        String secretsProviderProperty = environment.getProperty(SECRETS_PROVIDER_ENABLED_PROPERTY);
        boolean secretsProviderEnabled = Boolean.parseBoolean(secretsProviderProperty);
        logger.info(Strings.concat("Secrets Provider: ", SECRETS_PROVIDER_ENABLED_PROPERTY).concat(":").concat(secretsProviderProperty));

        if (secretsProviderEnabled) {
            String secretsDir = environment.getProperty(SECRETS_DIRECTORY_PROPERTY);
            Path secretsPath = Paths.get(secretsDir);

            verifySecretsDirectory(secretsPath);

            Map<String, Object> mountedProperties = new HashMap<>();

            logger.info(Strings.concat("Reading secrets from directory: ", secretsDir));
            try (Stream<Path> files = Files.walk(secretsPath)) {
                files
                        .filter(Files::isRegularFile) // filter all folders in mounted secrets directory
                        .forEach(file -> {
                            Map.Entry<String, Object> secretEntry = processSecretFile(file);
                            String secretEntryKey = secretEntry.getKey();
                            String replacedSecretKey = secretEntryKey.replace("-", "_");
                            mountedProperties.put(replacedSecretKey, secretEntry.getValue());
                        });
            } catch (IOException e) {
                throw new RuntimeException("Error reading secrets from directory", e);
            }
            addPropertiesToEnvironment(environment, mountedProperties);
        }
        application.addInitializers(ctx -> logger.replayTo(SecretsLoaderEnvironmentPostProcessor.class));
    }

    private void addPropertiesToEnvironment(ConfigurableEnvironment environment, Map<String, Object> mountedProperties) {
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource(SECRETS_PROPERTIES_SOURCE_NAME, mountedProperties));
        logger.info(Strings.concat("Secrets successfully added to the : ", SECRETS_PROPERTIES_SOURCE_NAME));
    }

    private Map.Entry<String, Object> processSecretFile(Path secretFile) {
        try {
            String secretName = secretFile.getFileName().toString();
            String secretValue = Files.readString(secretFile);
            return Map.entry(secretName, secretValue);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read secret from file: " + secretFile, e);
        }
    }

    private void verifySecretsDirectory(Path secretsPath) {
        if (!Files.isDirectory(secretsPath)) {
            throw new RuntimeException(Strings.concat("Secrets path is not a directory: ", secretsPath.toString()));
        }
        if (Objects.requireNonNull(secretsPath.toFile().listFiles()).length == 0) {
            throw new RuntimeException(Strings.concat("Secrets directory is empty: ", secretsPath.toString()));
        }
    }
}
